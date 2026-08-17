package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val SQUARE_BLOCK_RATIO = 0.04f

// montanha reduzida mantendo proporção (mesma escala em X e Y) e encostada à esquerda;
// o espaço que sobra à direita fica livre pra pilha de bloquinhos.
// Antes era 0.52 com a montanha centralizada verticalmente na caixa inteira, o que
// desperdiçava espaço em cima e deixava a base flutuando longe da sombra (ver
// MOUNTAIN_CONTENT_BOTTOM_FRACTION abaixo). Agora que a base é ancorada no chão
// junto com a pilha, dá pra crescer sem estourar o card.
private const val MOUNTAIN_SCALE = 1f

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

// O PNG da montanha tem bastante padding transparente dentro do seu próprio bounding
// box (a ilustração real é bem menor que o canvas exportado) — o MOUNTAIN_SILHOUETTE
// acima já foi desenhado sabendo disso, então em vez de chutar um número novo pro
// "chão visual" da montanha, extraímos ele do próprio polígono: é o y máximo que
// alguém já validou visualmente ao desenhar a máscara de shatter.
private val MOUNTAIN_CONTENT_BOTTOM_FRACTION = MOUNTAIN_SILHOUETTE.maxOf { it.y }

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

/**
 * Largura total de conteúdo necessária pra caber [totalTarget] blocos na pilha
 * sem estourar a altura disponível — calcula quantas linhas cabem verticalmente
 * e, a partir disso, quantas colunas são necessárias. Nunca fica menor que a
 * viewport (a montanha continua preenchendo bem quando há poucos blocos).
 */

private const val PILE_WIDTH_BIAS = 2.2f

private fun computeContentWidth(
    viewportWidth: Float,
    mountainWidth: Float,
    totalTarget: Int,
    squareSize: Float,
    availableHeight: Float
): Float {
    val spacing = squareSize * PILE_SPACING_FACTOR
    val maxRows = ((availableHeight * (1f - PILE_MARGIN_BOTTOM_FRACTION)) / spacing)
        .toInt()
        .coerceAtLeast(1)
    // divide o maxRows pelo bias antes de calcular colunas — isso força o layout
    // a preferir espalhar os blocos horizontalmente em vez de empilhar bem alto
    val effectiveMaxRows = (maxRows / PILE_WIDTH_BIAS).toInt().coerceAtLeast(1)
    val neededCols = kotlin.math.ceil(totalTarget.toFloat() / effectiveMaxRows).toInt().coerceAtLeast(1)
    val pileAreaWidth = neededCols * spacing
    val gap = viewportWidth * PILE_GAP_FROM_MOUNTAIN_FRACTION
    val marginRight = viewportWidth * PILE_MARGIN_RIGHT_FRACTION
    val needed = mountainWidth + gap + pileAreaWidth + marginRight
    return needed.coerceAtLeast(viewportWidth)
}

/**
 * Pequena variação determinística (rotação + deslocamento) por bloco da pilha,
 * pra fugir da grade "caixa de ovos" e parecer minério empilhado à mão.
 * Mesma [slotIndex] + [seed] sempre gera o mesmo jitter — usado tanto na
 * queda animada (MountainReveal) quanto no "print" estático (MountainSnapshot),
 * então a pilha fica visualmente idêntica nas duas telas.
 */
private data class PileJitter(val angleDeg: Float, val offsetXFraction: Float, val offsetYFraction: Float)

private fun jitterFor(slotIndex: Int, seed: Long): PileJitter {
    val r = Random(seed + slotIndex * 7919L)
    val angle = (r.nextFloat() - 0.5f) * 16f          // ±8°
    val jx = (r.nextFloat() - 0.5f) * 0.16f            // fração do squareSize
    val jy = (r.nextFloat() - 0.5f) * 0.12f
    return PileJitter(angle, jx, jy)
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

/** Desenha o bloco já com o jitter (rotação + leve deslocamento) aplicado. */
private fun DrawScope.drawPileBlock(
    slotIndex: Int,
    seed: Long,
    target: Offset,
    size: Float,
    baseColor: Color
) {
    val jitter = jitterFor(slotIndex, seed)
    val jx = target.x + jitter.offsetXFraction * size
    val jy = target.y + jitter.offsetYFraction * size
    val center = Offset(jx + size / 2f, jy + size / 2f)
    rotate(degrees = jitter.angleDeg, pivot = center) {
        drawStyledBlock(topLeftX = jx, topLeftY = jy, size = size, baseColor = baseColor)
    }
}

/**
 * Sombra rasa de "cratera" na área onde uma pedra já foi minerada — em vez de
 * deixar só o fundo flat aparecendo, cria a sensação de profundidade/rocha
 * exposta com um gradiente radial mais escuro nas bordas.
 */
private fun DrawScope.drawCraterShading(cellPx: Path, centroidPx: Offset) {
    val bounds = cellPx.getBounds()
    if (bounds.width <= 0f || bounds.height <= 0f) return
    val radius = (maxOf(bounds.width, bounds.height) / 2f).coerceAtLeast(1f)
    clipPath(cellPx) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.05f),
                    Color.Black.copy(alpha = 0.24f)
                ),
                center = centroidPx,
                radius = radius * 1.4f
            ),
            topLeft = bounds.topLeft,
            size = bounds.size
        )
    }
}

/**
 * Sombra de contato suave (pseudo-blur em camadas) usada tanto embaixo da
 * montanha quanto embaixo da pilha, pra ancorar as duas visualmente no "chão"
 * do card em vez de parecerem flutuando sobre o fundo liso.
 */
private fun DrawScope.drawGroundShadow(left: Float, right: Float, groundY: Float, thickness: Float) {
    val width = (right - left).coerceAtLeast(1f)
    val centerX = (left + right) / 2f
    val layers = listOf(1.5f to 0.045f, 1.1f to 0.075f, 0.78f to 0.11f)
    layers.forEach { (scale, alpha) ->
        drawOval(
            color = Color.Black.copy(alpha = alpha),
            topLeft = Offset(centerX - (width * scale) / 2f, groundY - (thickness * scale) / 2f),
            size = Size(width * scale, thickness * scale)
        )
    }
}

/**
 * Pequenas linhas de fratura, geradas de forma determinística a partir do
 * centroide da célula — usadas pelo ErodingCell pra mostrar rachaduras que
 * crescem em opacidade a cada clique, antes do pedaço soltar de vez.
 */
private fun crackEndpoints(cellIndex: Int, seed: Long, centroid: Offset, bounds: Rect, count: Int = 4): List<Offset> {
    val r = Random(seed + cellIndex * 104_729L)
    val maxRadius = maxOf(bounds.width, bounds.height) * 0.45f
    return List(count) {
        val angle = r.nextFloat() * (2f * PI.toFloat())
        val radius = maxRadius * (0.45f + r.nextFloat() * 0.55f)
        Offset(centroid.x + radius * cos(angle), centroid.y + radius * sin(angle))
    }
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
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .onSizeChanged { viewportSize = it }
    ) {
        if (bitmap != null && viewportSize.width > 0 && viewportSize.height > 0) {
            val viewportW = viewportSize.width.toFloat()
            val h = viewportSize.height.toFloat()

            // montanha ancorada na VIEWPORT (não no conteúdo total) — ela sempre
            // preenche bem o espaço visível ao abrir a tela, mesmo com scroll disponível
            val mw = viewportW * MOUNTAIN_SCALE
            val mh = h * MOUNTAIN_SCALE
            val groundY = h * (1f - PILE_MARGIN_BOTTOM_FRACTION)
            val offsetY = (groundY - mh * MOUNTAIN_CONTENT_BOTTOM_FRACTION).coerceAtLeast(h * 0.02f)
            val squareSize = min(mw, mh) * SQUARE_BLOCK_RATIO

            val totalTarget = goal.totalTarget
            val progress = goal.progress.coerceIn(0, totalTarget)

            val contentWidth = remember(viewportW, mw, totalTarget, squareSize, h) {
                computeContentWidth(viewportW, mw, totalTarget, squareSize, h)
            }
            val contentWidthDp = with(density) { contentWidth.toDp() }

            val cells = remember(seed, totalTarget) {
                buildShatterCells(visualCellCountFor(totalTarget), seed)
            }
            val breakOrder = remember(seed, cells) { cells.indices.shuffled(Random(seed)) }
            val thresholds = remember(cells.size, totalTarget) { buildThresholds(cells.size, totalTarget) }

            val brokenCount = remember(progress, thresholds) { thresholds.count { progress >= it } }
            val brokenSet = remember(brokenCount, breakOrder) { breakOrder.take(brokenCount).toSet() }

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
            val isFinalCell = brokenCount == cells.size - 1

            // acompanha automaticamente o bloco mais recente quando ele cai fora
            // da área visível, pra nunca perder a recompensa do clique
            LaunchedEffect(brokenCount, contentWidth) {
                if (brokenCount == 0) return@LaunchedEffect
                val lastSlot = brokenCount - 1
                val target = pileTargetPosition(lastSlot, squareSize, mw, contentWidth, h)
                val blockRight = target.x + squareSize
                val visibleLeft = scrollState.value.toFloat()
                val visibleRight = visibleLeft + viewportW
                when {
                    blockRight > visibleRight -> {
                        val dest = (blockRight - viewportW + viewportW * 0.08f)
                            .toInt()
                            .coerceIn(0, scrollState.maxValue)
                        scrollState.animateScrollTo(dest)
                    }
                    target.x < visibleLeft -> {
                        scrollState.animateScrollTo(target.x.toInt().coerceAtLeast(0))
                    }
                }
            }

            Box(
                Modifier
                    .horizontalScroll(scrollState)
                    .fillMaxHeight()
            ) {
                Box(
                    Modifier
                        .width(contentWidthDp)
                        .fillMaxHeight()
                ) {
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

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawGroundShadow(
                            left = mw * 0.06f,
                            right = mw * 0.98f,
                            groundY = groundY,
                            thickness = h * 0.035f
                        )
                        if (brokenSet.isNotEmpty()) {
                            val pileLeft = mw + contentWidth * PILE_GAP_FROM_MOUNTAIN_FRACTION
                            val pileRight = contentWidth * (1f - PILE_MARGIN_RIGHT_FRACTION)
                            drawGroundShadow(
                                left = pileLeft,
                                right = pileRight,
                                groundY = groundY,
                                thickness = h * 0.03f
                            )
                        }

                        clipPath(remainingPath) {
                            drawImage(
                                bitmap,
                                dstOffset = IntOffset(0, offsetY.toInt()),
                                dstSize = IntSize(mw.toInt(), mh.toInt())
                            )
                        }
                        brokenSet.forEach { idx ->
                            val cellPx = scalePath(cells[idx].path, mw, mh, offsetY = offsetY)
                            val centroidPx = Offset(
                                cells[idx].centroid.x * mw,
                                cells[idx].centroid.y * mh + offsetY
                            )
                            drawCraterShading(cellPx, centroidPx)
                        }
                    }

                    if (currentCellIndex != null && currentLocalProgress > 0f) {
                        key(currentCellIndex) {
                            ErodingCell(
                                cell = cells[currentCellIndex],
                                cellIndex = currentCellIndex,
                                seed = seed,
                                localProgress = currentLocalProgress,
                                mountainWidth = mw,
                                mountainHeight = mh,
                                offsetY = offsetY,
                                mountainBitmap = bitmap,
                                isFinalCell = isFinalCell
                            )
                        }
                    }

                    // PILHA: 1 clique = 1 bloco, sempre — desacoplado das fatias visuais
                    // da montanha. blocos já pousados ficam estáticos no mesmo Canvas
                    // (como no MountainSnapshot); só o bloco mais recente anima o voo.
                    if (progress > 0) {
                        val landedCount = progress - 1 // todos exceto o mais recente
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            for (slot in 0 until landedCount) {
                                val target = pileTargetPosition(
                                    slotIndex = slot,
                                    squareSize = squareSize,
                                    mountainWidth = mw,
                                    boxWidth = contentWidth,
                                    boxHeight = h
                                )
                                drawPileBlock(
                                    slotIndex = slot,
                                    seed = seed,
                                    target = target,
                                    size = squareSize,
                                    baseColor = accentColor
                                )
                            }
                        }

                        // bloco mais recente: anima voando a partir da fatia ativa no
                        // momento do clique (a que está rachando, ou a última completa)
                        val originIndex = currentCellIndex
                            ?: breakOrder.getOrNull(brokenCount - 1)
                            ?: breakOrder.firstOrNull()
                        if (originIndex != null) {
                            key(progress) {
                                FallingPiece(
                                    slotIndex = landedCount,
                                    originCell = cells[originIndex],
                                    boxWidth = contentWidth,
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
    }
}

@Composable
private fun ErodingCell(
    cell: ShatterCell,
    cellIndex: Int,
    seed: Long,
    localProgress: Float, // 0f = intacta, 1f = prestes a soltar
    mountainWidth: Float,
    mountainHeight: Float,
    offsetY: Float,
    mountainBitmap: ImageBitmap,
    isFinalCell: Boolean
) {
    val cellPx = remember(cell, mountainWidth, mountainHeight, offsetY) {
        scalePath(cell.path, mountainWidth, mountainHeight, offsetY = offsetY)
    }
    val originX = cell.centroid.x * mountainWidth
    val originY = cell.centroid.y * mountainHeight + offsetY
    val centroidPx = Offset(originX, originY)
    val bounds = remember(cellPx) { cellPx.getBounds() }
    val crackEnds = remember(cellIndex, seed, bounds) {
        crackEndpoints(cellIndex, seed, centroidPx, bounds)
    }

    // encolhe e escurece levemente a cada clique, dando sensação de "rachando"
    val scale = 1f - 0.12f * localProgress
    val darken = 1f - 0.25f * localProgress

    // glow pulsante dourado quando esta é a ÚLTIMA pedra que falta — reforço de
    // "quase lá" no final da montanha
    val infiniteTransition = rememberInfiniteTransition(label = "finalCellGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "finalCellGlowAlpha"
    )

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
        if (isFinalCell) {
            val glowRadius = maxOf(bounds.width, bounds.height) * 1.1f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD54F).copy(alpha = glowAlpha),
                        Color(0xFFFFD54F).copy(alpha = 0f)
                    ),
                    center = centroidPx,
                    radius = glowRadius.coerceAtLeast(1f)
                ),
                radius = glowRadius.coerceAtLeast(1f),
                center = centroidPx
            )
        }

        clipPath(cellPx) {
            drawImage(
                mountainBitmap,
                dstOffset = IntOffset(0, offsetY.toInt()),
                dstSize = IntSize(mountainWidth.toInt(), mountainHeight.toInt()),
                alpha = darken
            )

            // rachaduras crescem em opacidade a cada clique — feedback de que a
            // pedra está prestes a soltar, sem esperar o bloco sumir de repente
            val crackAlpha = (localProgress * 0.85f).coerceIn(0f, 0.85f)
            val crackColor = Color.Black.copy(alpha = crackAlpha)
            val crackWidth = (min(mountainWidth, mountainHeight) * 0.004f).coerceAtLeast(1f)
            crackEnds.forEach { end ->
                drawLine(
                    color = crackColor,
                    start = centroidPx,
                    end = end,
                    strokeWidth = crackWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun FallingPiece(
    slotIndex: Int,        // posição na pilha (0-based, = progress - 1 no momento do voo)
    originCell: ShatterCell, // de onde o bloco "nasce" visualmente (fatia ativa no momento do clique)
    boxWidth: Float,
    boxHeight: Float,
    mountainWidth: Float,
    mountainHeight: Float,
    offsetY: Float,
    seed: Long,
    accentColor: Color
) {
    val progressAnim = remember(slotIndex) { Animatable(0f) }
    val landAnim = remember(slotIndex) { Animatable(0f) }

    LaunchedEffect(slotIndex) {
        progressAnim.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        landAnim.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
    }

    val squareSize = min(mountainWidth, mountainHeight) * SQUARE_BLOCK_RATIO

    val rawTarget = pileTargetPosition(
        slotIndex = slotIndex,
        squareSize = squareSize,
        mountainWidth = mountainWidth,
        boxWidth = boxWidth,
        boxHeight = boxHeight
    )
    val jitter = remember(slotIndex, seed) { jitterFor(slotIndex, seed) }
    val targetX = rawTarget.x + jitter.offsetXFraction * squareSize
    val targetY = rawTarget.y + jitter.offsetYFraction * squareSize

    val originX = (originCell.centroid.x * mountainWidth - squareSize / 2f)
        .coerceIn(0f, mountainWidth - squareSize)
    val originY = (originCell.centroid.y * mountainHeight + offsetY - squareSize / 2f)
        .coerceIn(offsetY, offsetY + mountainHeight - squareSize)

    val fullSpins = remember(slotIndex, seed) { 1 + Random(seed + slotIndex).nextInt(2) }
    val totalRotation = fullSpins * 360f + jitter.angleDeg

    val t = progressAnim.value
    val dx = (targetX - originX) * t

    val arcHeight = min(mountainWidth, mountainHeight) * 0.14f
    val arcOffset = -arcHeight * sin(PI.toFloat() * t)
    val dy = (targetY - originY) * t + arcOffset

    val land = landAnim.value
    val landScaleY = 1f - 0.22f * sin(PI.toFloat() * land)
    val landScaleX = 1f + 0.14f * sin(PI.toFloat() * land)

    val elevation = (-arcOffset).coerceAtLeast(0f)
    val shadowAlpha = (0.2f - (elevation / arcHeight) * 0.14f).coerceIn(0.05f, 0.2f)
    val shadowScale = (1f - (elevation / arcHeight) * 0.45f).coerceIn(0.5f, 1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val shadowCenter = Offset(originX + dx + squareSize / 2f, targetY + squareSize * 1.02f)
        drawOval(
            color = Color.Black.copy(alpha = shadowAlpha),
            topLeft = Offset(
                shadowCenter.x - (squareSize * 0.55f * shadowScale) / 2f,
                shadowCenter.y - (squareSize * 0.28f * shadowScale) / 2f
            ),
            size = Size(squareSize * 0.55f * shadowScale, squareSize * 0.28f * shadowScale)
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = dx
                translationY = dy
                rotationZ = totalRotation * t
                scaleX = if (t >= 1f) landScaleX else 1f
                scaleY = if (t >= 1f) landScaleY else 1f
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

            // mesma escala proporcional e mesma âncora de chão usadas na versão animada
            val mw = w * MOUNTAIN_SCALE
            val mh = h * MOUNTAIN_SCALE
            val groundY = h * (1f - PILE_MARGIN_BOTTOM_FRACTION)
            val offsetY = (groundY - mh * MOUNTAIN_CONTENT_BOTTOM_FRACTION).coerceAtLeast(h * 0.02f)
            val squareSize = min(mw, mh) * SQUARE_BLOCK_RATIO

            val totalTarget = goal.totalTarget
            val progress = goal.progress.coerceIn(0, totalTarget)

            // MONTANHA (fatias visuais): mesma lógica de thresholds do MountainReveal —
            // controla só a aparência de buraco/erosão, não a contagem da pilha
            val cells = remember(seed, totalTarget) {
                buildShatterCells(visualCellCountFor(totalTarget), seed)
            }
            val breakOrder = remember(seed, cells) { cells.indices.shuffled(Random(seed)) }
            val thresholds = remember(cells.size, totalTarget) { buildThresholds(cells.size, totalTarget) }
            val brokenCellCount = remember(progress, thresholds) { thresholds.count { progress >= it } }
            val brokenSet = remember(brokenCellCount, breakOrder) { breakOrder.take(brokenCellCount).toSet() }

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
                // mesma ancoragem visual (sombra de contato) da versão animada
                drawGroundShadow(
                    left = mw * 0.06f,
                    right = mw * 0.98f,
                    groundY = groundY,
                    thickness = h * 0.035f
                )
                if (progress > 0) {
                    val pileLeft = mw + w * PILE_GAP_FROM_MOUNTAIN_FRACTION
                    val pileRight = w * (1f - PILE_MARGIN_RIGHT_FRACTION)
                    drawGroundShadow(
                        left = pileLeft,
                        right = pileRight,
                        groundY = groundY,
                        thickness = h * 0.03f
                    )
                }

                clipPath(remainingPath) {
                    drawImage(
                        bitmap,
                        dstOffset = IntOffset(0, offsetY.toInt()),
                        dstSize = IntSize(mw.toInt(), mh.toInt())
                    )
                }

                // mesma sombra de "cratera" da versão animada, pros buracos não
                // ficarem planos também no print estático usado no card da lista
                brokenSet.forEach { idx ->
                    val cellPx = scalePath(cells[idx].path, mw, mh, offsetY = offsetY)
                    val centroidPx = Offset(
                        cells[idx].centroid.x * mw,
                        cells[idx].centroid.y * mh + offsetY
                    )
                    drawCraterShading(cellPx, centroidPx)
                }

                // PILHA: 1 bloco por bloco de progresso real (goal.progress), não por
                // fatia da montanha — mesma regra 1:1 do MountainReveal, só que aqui
                // todos os blocos ficam parados (é um "print", sem voo/animação)
                for (slot in 0 until progress) {
                    val target = pileTargetPosition(
                        slotIndex = slot,
                        squareSize = squareSize,
                        mountainWidth = mw,
                        boxWidth = w,
                        boxHeight = h
                    )
                    drawPileBlock(
                        slotIndex = slot,
                        seed = seed,
                        target = target,
                        size = squareSize,
                        baseColor = accentColor
                    )
                }
            }
        }
    }
}