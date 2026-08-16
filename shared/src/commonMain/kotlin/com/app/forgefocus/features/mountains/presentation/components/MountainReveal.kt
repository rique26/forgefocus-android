package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.app.forgefocus.core.domain.model.Goal
import forgefocus.shared.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.min
import kotlin.random.Random

private const val SQUARE_BLOCK_RATIO = 0.09f

// montanha reduzida mantendo proporção (mesma escala em X e Y) e encostada à esquerda;
// o espaço que sobra à direita fica livre pra pilha de bloquinhos
private const val MOUNTAIN_SCALE = 0.52f

// ============================================================
// 1. CARREGAMENTO DO BITMAP (portável, sem API Android)
// ============================================================

private object MountainBitmapCache {
    private var cached: ImageBitmap? = null
    private val mutex = Mutex()

    suspend fun get(): ImageBitmap {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: run {
                val bytes = Res.readBytes("drawable/mountain_illustration.png")
                bytes.decodeToImageBitmap().also { cached = it }
            }
        }
    }
}

@Composable
private fun rememberMountainBitmap(): ImageBitmap? {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        bitmap = MountainBitmapCache.get()
    }
    return bitmap
}

// ============================================================
// 2. SILHUETA DA MONTANHA (polígono normalizado 0..1)
// ============================================================

private val MOUNTAIN_SILHOUETTE = listOf(
    Offset(0.50f, 0.14f),
    Offset(0.58f, 0.30f),
    Offset(0.63f, 0.33f),
    Offset(0.70f, 0.40f),
    Offset(0.66f, 0.44f),
    Offset(0.74f, 0.52f),
    Offset(0.70f, 0.55f),
    Offset(0.78f, 0.63f),
    Offset(0.66f, 0.62f),
    Offset(0.55f, 0.66f),
    Offset(0.50f, 0.64f),
    Offset(0.45f, 0.66f),
    Offset(0.34f, 0.62f),
    Offset(0.22f, 0.63f),
    Offset(0.30f, 0.55f),
    Offset(0.26f, 0.52f),
    Offset(0.34f, 0.44f),
    Offset(0.30f, 0.40f),
    Offset(0.37f, 0.33f),
    Offset(0.42f, 0.30f)
)

private fun silhouettePath(
    width: Float,
    height: Float,
    offsetX: Float = 0f,
    offsetY: Float = 0f
): Path = Path().apply {
    val pts = MOUNTAIN_SILHOUETTE.map { Offset(it.x * width + offsetX, it.y * height + offsetY) }
    moveTo(pts[0].x, pts[0].y)
    pts.drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

private fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
        val pi = polygon[i]
        val pj = polygon[j]
        if ((pi.y > point.y) != (pj.y > point.y) &&
            point.x < (pj.x - pi.x) * (point.y - pi.y) / (pj.y - pi.y) + pi.x
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

// ============================================================
// 3. GERAÇÃO DAS CÉLULAS (Voronoi simplificado, sem gaps entre pedras)
// ============================================================

data class ShatterCell(
    val path: Path,       // coordenadas normalizadas 0..1
    val centroid: Offset  // 0..1
)

private const val LABEL_GRID = 96          // resolução maior p/ suportar mais células sem buracos
private const val MIN_SEED_DIST_FLOOR = 0.012f  // menor distância permitida entre pedras (packing denso)
private const val MIN_SEED_DIST_CEIL = 0.05f    // maior distância permitida (poucas pedras, mais espaçadas)

fun buildShatterCells(cellCount: Int, seed: Long): List<ShatterCell> {
    val rnd = Random(seed)
    val seeds = mutableListOf<Offset>()
    var attempts = 0

    // distância mínima entre sementes diminui conforme pedimos mais células,
    // permitindo empacotar até ~240 pedaços dentro da mesma silhueta
    val minSeedDist = (0.62f / kotlin.math.sqrt(cellCount.toFloat()))
        .coerceIn(MIN_SEED_DIST_FLOOR, MIN_SEED_DIST_CEIL)
    val minDistSq = minSeedDist * minSeedDist

    while (seeds.size < cellCount && attempts < cellCount * 800) {
        attempts++
        val p = Offset(rnd.nextFloat(), rnd.nextFloat())
        if (!pointInPolygon(p, MOUNTAIN_SILHOUETTE)) continue
        val tooClose = seeds.any { s ->
            val dx = s.x - p.x
            val dy = s.y - p.y
            (dx * dx + dy * dy) < minDistSq
        }
        if (!tooClose) seeds.add(p)
    }
    if (seeds.isEmpty()) return emptyList()

    val labels = Array(LABEL_GRID) { IntArray(LABEL_GRID) { -1 } }
    for (gy in 0 until LABEL_GRID) {
        for (gx in 0 until LABEL_GRID) {
            val p = Offset((gx + 0.5f) / LABEL_GRID, (gy + 0.5f) / LABEL_GRID)
            if (!pointInPolygon(p, MOUNTAIN_SILHOUETTE)) continue
            var best = -1
            var bestDist = Float.MAX_VALUE
            seeds.forEachIndexed { i, s ->
                val d = (s.x - p.x) * (s.x - p.x) + (s.y - p.y) * (s.y - p.y)
                if (d < bestDist) { bestDist = d; best = i }
            }
            labels[gy][gx] = best
        }
    }

    val cw = 1f / LABEL_GRID
    val ch = 1f / LABEL_GRID

    return seeds.indices.mapNotNull { cellIndex ->
        val cellPath = Path()
        var any = false
        var sumX = 0f; var sumY = 0f; var count = 0
        for (gy in 0 until LABEL_GRID) {
            for (gx in 0 until LABEL_GRID) {
                if (labels[gy][gx] == cellIndex) {
                    any = true
                    val x = gx * cw
                    val y = gy * ch
                    cellPath.addRect(Rect(x, y, x + cw, y + ch))
                    sumX += x + cw / 2; sumY += y + ch / 2; count++
                }
            }
        }
        if (!any) return@mapNotNull null
        val unioned = Path().apply { op(cellPath, cellPath, PathOperation.Union) }
        ShatterCell(path = unioned, centroid = Offset(sumX / count, sumY / count))
    }
}

private fun scalePath(
    normalized: Path,
    w: Float,
    h: Float,
    offsetX: Float = 0f,
    offsetY: Float = 0f
): Path {
    val m = Matrix().apply { scale(w, h, 1f) }
    val scaled = Path().apply {
        addPath(normalized)
        transform(m)
    }
    if (offsetX == 0f && offsetY == 0f) return scaled
    return Path().apply { addPath(scaled, Offset(offsetX, offsetY)) }
}

// ============================================================
// 3.1 POSIÇÃO DA PILHA — empilhamento físico, de baixo pra cima,
// coluna a coluna, sem sobreposição, ocupando só a área livre à
// direita da montanha (entre a borda direita da montanha e a
// borda direita do componente).
// ============================================================

private const val PILE_SPACING_FACTOR = 1.08f  // pequeno respiro entre blocos
private const val PILE_MARGIN_RIGHT_FRACTION = 0.04f
private const val PILE_MARGIN_BOTTOM_FRACTION = 0.05f
private const val PILE_GAP_FROM_MOUNTAIN_FRACTION = 0.04f

/**
 * Calcula a posição (canto superior-esquerdo) do bloquinho de índice [slotIndex]
 * dentro da pilha. slotIndex deve ser único por bloco (0, 1, 2, ...) — cada valor
 * mapeia pra uma célula exclusiva da grade, então nunca há dois blocos disputando
 * o mesmo lugar.
 *
 * A grade é preenchida coluna a coluna a partir da esquerda (perto da montanha),
 * e cada coluna cresce de baixo pra cima (linha 0 encostada no "chão").
 */
private fun pileTargetPosition(
    slotIndex: Int,
    squareSize: Float,
    mountainWidth: Float,
    boxWidth: Float,
    boxHeight: Float
): Offset {
    val spacing = squareSize * PILE_SPACING_FACTOR

    val pileAreaLeft = mountainWidth + boxWidth * PILE_GAP_FROM_MOUNTAIN_FRACTION
    val pileAreaRight = boxWidth * (1f - PILE_MARGIN_RIGHT_FRACTION)
    val pileAreaWidth = (pileAreaRight - pileAreaLeft).coerceAtLeast(spacing)

    val cols = (pileAreaWidth / spacing).toInt().coerceAtLeast(1)
    val col = slotIndex % cols
    val row = slotIndex / cols

    val groundY = boxHeight * (1f - PILE_MARGIN_BOTTOM_FRACTION) - squareSize

    val x = pileAreaLeft + col * spacing
    val y = groundY - row * spacing

    return Offset(x, y)
}

// ============================================================
// 3.2 BLOCO ESTILIZADO — cor sólida + bisel (luz vindo de cima-
// esquerda), no lugar do recorte fotográfico da montanha. Usado
// só nos bloquinhos da pilha (a montanha em si continua usando
// o bitmap normalmente).
// ============================================================

private fun DrawScope.drawStyledBlock(
    topLeftX: Float,
    topLeftY: Float,
    size: Float,
    baseColor: Color
) {
    val corner = CornerRadius(size * 0.22f, size * 0.22f)
    drawRoundRect(
        color = baseColor,
        topLeft = Offset(topLeftX, topLeftY),
        size = Size(size, size),
        cornerRadius = corner
    )

    val highlight = lerp(baseColor, Color.White, 0.42f)
    val shadow = lerp(baseColor, Color.Black, 0.38f)
    val strokeWidth = (size * 0.1f).coerceAtLeast(1f)
    val half = strokeWidth / 2f

    // aresta clara: topo + esquerda (simula luz vindo de cima-esquerda)
    drawLine(
        color = highlight,
        start = Offset(topLeftX + half, topLeftY + size - half),
        end = Offset(topLeftX + half, topLeftY + half),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = highlight,
        start = Offset(topLeftX + half, topLeftY + half),
        end = Offset(topLeftX + size - half, topLeftY + half),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // aresta escura: baixo + direita
    drawLine(
        color = shadow,
        start = Offset(topLeftX + size - half, topLeftY + half),
        end = Offset(topLeftX + size - half, topLeftY + size - half),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = shadow,
        start = Offset(topLeftX + size - half, topLeftY + size - half),
        end = Offset(topLeftX + half, topLeftY + size - half),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

// ============================================================
// 4. VERSÃO ANIMADA (GoalDetailScreen) — blocos voando pra pilha
// ============================================================

private const val MAX_VISUAL_CELLS = 240  // teto de performance/legibilidade

private fun visualCellCountFor(totalTarget: Int): Int =
    totalTarget.coerceIn(8, MAX_VISUAL_CELLS)

/** Para cada fatia i (0-based, na ordem de quebra), o nº de blocos acumulados
 *  necessário pra ela estar 100% quebrada. A última fatia sempre bate
 *  exatamente com totalTarget — nunca quebra antes da hora. */
private fun buildThresholds(cellCount: Int, totalTarget: Int): List<Int> =
    (1..cellCount).map { i ->
        ((i.toFloat() / cellCount) * totalTarget).let { kotlin.math.ceil(it).toInt() }
    }.let { list ->
        // garante estritamente crescente e o último == totalTarget
        val fixed = list.toMutableList()
        for (i in 1 until fixed.size) {
            if (fixed[i] <= fixed[i - 1]) fixed[i] = fixed[i - 1] + 1
        }
        fixed[fixed.lastIndex] = totalTarget
        fixed
    }

@Composable
fun MountainReveal(
    goal: Goal,
    seed: Long,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val bitmap = rememberMountainBitmap()
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .onSizeChanged { boxSize = it }
    ) {
        if (bitmap != null && boxSize.width > 0 && boxSize.height > 0) {
            val w = boxSize.width.toFloat()
            val h = boxSize.height.toFloat()

            // montanha reduzida mantendo proporção (mesmo fator em X e Y), encostada à esquerda
            val mw = w * MOUNTAIN_SCALE
            val mh = h * MOUNTAIN_SCALE
            val offsetY = (h - mh) / 2f

            val totalTarget = goal.totalTarget
            val progress = goal.progress.coerceIn(0, totalTarget)

            val cells = remember(seed, goal.totalTarget) {
                buildShatterCells(visualCellCountFor(goal.totalTarget), seed)
            }
            val breakOrder = remember(seed, cells) { cells.indices.shuffled(Random(seed)) }
            val thresholds = remember(cells.size, totalTarget) { buildThresholds(cells.size, totalTarget) }

            // quantas fatias já 100% quebradas
            val brokenCount = remember(progress, thresholds) { thresholds.count { progress >= it } }
            val brokenSet = remember(brokenCount, breakOrder) { breakOrder.take(brokenCount).toSet() }

            // progresso local (0f..1f) da fatia que está sendo quebrada agora
            val currentLocalProgress = remember(progress, thresholds, brokenCount) {
                if (brokenCount >= cells.size) 1f
                else {
                    val prevThreshold = if (brokenCount == 0) 0 else thresholds[brokenCount - 1]
                    val nextThreshold = thresholds[brokenCount]
                    val span = (nextThreshold - prevThreshold).coerceAtLeast(1)
                    ((progress - prevThreshold).toFloat() / span).coerceIn(0f, 1f)
                }
            }
            val currentCellIndex = if (brokenCount < breakOrder.size) breakOrder[brokenCount] else null

            val remainingPath = remember(brokenSet, cells, mw, mh, offsetY) {
                var path = silhouettePath(mw, mh, offsetY = offsetY)
                brokenSet.forEach { idx ->
                    val cellPx = scalePath(cells[idx].path, mw, mh, offsetY = offsetY)
                    val next = Path()
                    next.op(path, cellPx, PathOperation.Difference)
                    path = next
                }
                path
            }

            // base: montanha com buracos onde já quebrou de vez
            Canvas(modifier = Modifier.fillMaxSize()) {
                clipPath(remainingPath) {
                    drawImage(
                        bitmap,
                        dstOffset = IntOffset(0, offsetY.toInt()),
                        dstSize = IntSize(mw.toInt(), mh.toInt())
                    )
                }
            }

            // fatia "em quebra": erosão progressiva a cada clique (feedback imediato) —
            // continua usando o bitmap da montanha, só os blocos da pilha mudam de estilo
            if (currentCellIndex != null && currentLocalProgress > 0f) {
                key(currentCellIndex) {
                    ErodingCell(
                        cell = cells[currentCellIndex],
                        localProgress = currentLocalProgress,
                        mountainWidth = mw,
                        mountainHeight = mh,
                        offsetY = offsetY,
                        mountainBitmap = bitmap
                    )
                }
            }

            // pedaços quebrados voando pra pilha — bloco estilizado (cor sólida + bisel)
            cells.forEachIndexed { index, cell ->
                if (index in brokenSet) {
                    key(index) {
                        FallingPiece(
                            cellIndex = breakOrder.indexOf(index),
                            cell = cell,
                            boxWidth = w,       // pilha usa largura TOTAL (área livre à direita)
                            boxHeight = h,
                            mountainWidth = mw,
                            mountainHeight = mh,
                            offsetY = offsetY,
                            seed = seed,
                            accentColor = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErodingCell(
    cell: ShatterCell,
    localProgress: Float, // 0f = intacta, 1f = prestes a soltar
    mountainWidth: Float,
    mountainHeight: Float,
    offsetY: Float,
    mountainBitmap: ImageBitmap
) {
    val cellPx = remember(cell, mountainWidth, mountainHeight, offsetY) {
        scalePath(cell.path, mountainWidth, mountainHeight, offsetY = offsetY)
    }
    val originX = cell.centroid.x * mountainWidth
    val originY = cell.centroid.y * mountainHeight + offsetY

    // encolhe e escurece levemente a cada clique, dando sensação de "rachando"
    val scale = 1f - 0.12f * localProgress
    val darken = 1f - 0.25f * localProgress

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(originX / size.width, originY / size.height)
                alpha = 1f - 0.15f * localProgress
            }
    ) {
        clipPath(cellPx) {
            drawImage(
                mountainBitmap,
                dstOffset = IntOffset(0, offsetY.toInt()),
                dstSize = IntSize(mountainWidth.toInt(), mountainHeight.toInt()),
                alpha = darken
            )
        }
    }
}

@Composable
private fun FallingPiece(
    cellIndex: Int,
    cell: ShatterCell,
    boxWidth: Float,       // largura total do componente — onde a pilha vive, à direita
    boxHeight: Float,
    mountainWidth: Float,  // largura da montanha já reduzida (origem geométrica do bloco)
    mountainHeight: Float,
    offsetY: Float,
    seed: Long,
    accentColor: Color
) {
    val progressAnim = remember(cellIndex) { Animatable(0f) }

    LaunchedEffect(cellIndex) {
        progressAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    val squareSize = min(mountainWidth, mountainHeight) * SQUARE_BLOCK_RATIO

    // cellIndex já é único por bloco (posição dele na ordem de quebra), então
    // usamos ele direto como slot — sem módulo, sem colisão de posição na pilha
    val target = pileTargetPosition(
        slotIndex = cellIndex,
        squareSize = squareSize,
        mountainWidth = mountainWidth,
        boxWidth = boxWidth,
        boxHeight = boxHeight
    )
    val targetX = target.x
    val targetY = target.y

    // origem: onde o bloco "nasce", centrado no centroide da célula que quebrou
    val originX = (cell.centroid.x * mountainWidth - squareSize / 2f)
        .coerceIn(0f, mountainWidth - squareSize)
    val originY = (cell.centroid.y * mountainHeight + offsetY - squareSize / 2f)
        .coerceIn(offsetY, offsetY + mountainHeight - squareSize)

    val spin = remember(cellIndex) { (Random(seed + cellIndex).nextFloat() - 0.5f) * 540f }

    val t = progressAnim.value
    val dx = (targetX - originX) * t
    val dy = (targetY - originY) * t

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = dx
                translationY = dy
                rotationZ = spin * t
                transformOrigin = TransformOrigin(
                    (originX + squareSize / 2f) / boxWidth,
                    (originY + squareSize / 2f) / boxHeight
                )
            }
    ) {
        drawStyledBlock(
            topLeftX = originX,
            topLeftY = originY,
            size = squareSize,
            baseColor = accentColor
        )
    }
}

// ============================================================
// 5. VERSÃO ESTÁTICA (GoalCard) — "print" do estado atual
// ============================================================

@Composable
fun MountainSnapshot(
    goal: Goal,
    progressFraction: Float,
    seed: Long,
    accentColor: Color = Color(goal.color),
    modifier: Modifier = Modifier
) {
    val bitmap = rememberMountainBitmap()
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .onSizeChanged { boxSize = it }
    ) {
        if (bitmap != null && boxSize.width > 0 && boxSize.height > 0) {
            val w = boxSize.width.toFloat()
            val h = boxSize.height.toFloat()

            // mesma escala proporcional usada na versão animada
            val mw = w * MOUNTAIN_SCALE
            val mh = h * MOUNTAIN_SCALE
            val offsetY = (h - mh) / 2f

            val cells = remember(seed, goal.totalTarget) {
                buildShatterCells(visualCellCountFor(goal.totalTarget), seed)
            }
            val breakOrder = remember(seed, cells) { cells.indices.shuffled(Random(seed)) }
            val brokenCount = (progressFraction * cells.size).toInt().coerceIn(0, cells.size)
            val brokenSet = remember(brokenCount, breakOrder) { breakOrder.take(brokenCount).toSet() }

            val remainingPath = remember(brokenSet, cells, mw, mh, offsetY) {
                var path = silhouettePath(mw, mh, offsetY = offsetY)
                brokenSet.forEach { idx ->
                    val cellPx = scalePath(cells[idx].path, mw, mh, offsetY = offsetY)
                    val next = Path()
                    next.op(path, cellPx, PathOperation.Difference)
                    path = next
                }
                path
            }

            // sem animação, sem blocos voando — só o estado atual "congelado"
            Canvas(modifier = Modifier.fillMaxSize()) {
                clipPath(remainingPath) {
                    drawImage(
                        bitmap,
                        dstOffset = IntOffset(0, offsetY.toInt()),
                        dstSize = IntSize(mw.toInt(), mh.toInt())
                    )
                }

                // pilha de bloquinhos já minerados, congelada (sem animação — é o "print").
                // bloco estilizado (cor sólida + bisel) no lugar do recorte de bitmap.
                val squareSize = min(mw, mh) * SQUARE_BLOCK_RATIO
                brokenSet.forEachIndexed { i, _ ->
                    // i já é a posição única do bloco na ordem de quebra — mesmo slot que
                    // a versão animada usaria, garantindo empilhamento idêntico e sem overlap
                    val target = pileTargetPosition(
                        slotIndex = i,
                        squareSize = squareSize,
                        mountainWidth = mw,
                        boxWidth = w,
                        boxHeight = h
                    )
                    drawStyledBlock(
                        topLeftX = target.x,
                        topLeftY = target.y,
                        size = squareSize,
                        baseColor = accentColor
                    )
                }
            }
        }
    }
}