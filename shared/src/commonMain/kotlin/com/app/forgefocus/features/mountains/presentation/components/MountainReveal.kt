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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import forgefocus.shared.generated.resources.Res
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.min
import kotlin.random.Random

// ============================================================
// 1. CARREGAMENTO DO BITMAP (portável, sem API Android)
// ============================================================

/**
 * Cache simples em memória do processo. Evita re-decodificar o PNG
 * toda vez que um GoalCard aparece na lista (LazyColumn recompõe bastante).
 */
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
//    Ajuste esses pontos olhando o seu PNG se quiser encaixe mais fino.
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

private fun silhouettePath(width: Float, height: Float): Path = Path().apply {
    val pts = MOUNTAIN_SILHOUETTE.map { Offset(it.x * width, it.y * height) }
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
// 3. GERAÇÃO DAS CÉLULAS (Voronoi simplificado)
// ============================================================

data class ShatterCell(
    val path: Path,       // coordenadas normalizadas 0..1
    val centroid: Offset  // 0..1
)

private const val LABEL_GRID = 48
private const val CELL_COUNT = 18
private const val PILE_COLS = 6
private const val PILE_ROWS = 3

fun buildShatterCells(cellCount: Int, seed: Long): List<ShatterCell> {
    val rnd = Random(seed)

    val seeds = mutableListOf<Offset>()
    var attempts = 0
    while (seeds.size < cellCount && attempts < cellCount * 200) {
        attempts++
        val p = Offset(rnd.nextFloat(), rnd.nextFloat())
        if (pointInPolygon(p, MOUNTAIN_SILHOUETTE)) seeds.add(p)
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

private fun scalePath(normalized: Path, w: Float, h: Float): Path {
    val m = Matrix().apply { scale(w, h, 1f) }
    return Path().apply {
        addPath(normalized)
        transform(m)
    }
}

// ============================================================
// 4. COMPOSABLE PRINCIPAL
// ============================================================

@Composable
fun MountainReveal(
    progressFraction: Float,
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

            val cells = remember(seed) { buildShatterCells(CELL_COUNT, seed) }
            val breakOrder = remember(seed, cells) { cells.indices.shuffled(Random(seed)) }
            val brokenCount = (progressFraction * cells.size).toInt().coerceIn(0, cells.size)
            val brokenSet = remember(brokenCount, breakOrder) { breakOrder.take(brokenCount).toSet() }

            val remainingPath = remember(brokenSet, cells, w, h) {
                var path = silhouettePath(w, h)
                brokenSet.forEach { idx ->
                    val cellPx = scalePath(cells[idx].path, w, h)
                    val next = Path()
                    next.op(path, cellPx, PathOperation.Difference)
                    path = next
                }
                path
            }

            // base: montanha "com buracos" onde já quebrou
            Canvas(modifier = Modifier.fillMaxSize()) {
                clipPath(remainingPath) {
                    drawImage(bitmap, dstSize = IntSize(w.toInt(), h.toInt()))
                }
            }

            // pedaços quebrados voando pra pilha
            cells.forEachIndexed { index, cell ->
                if (index in brokenSet) {
                    key(index) {
                        FallingPiece(
                            cellIndex = index,
                            cell = cell,
                            boxWidth = w,
                            boxHeight = h,
                            seed = seed,
                            mountainBitmap = bitmap
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FallingPiece(
    cellIndex: Int,
    cell: ShatterCell,
    boxWidth: Float,
    boxHeight: Float,
    seed: Long,
    mountainBitmap: ImageBitmap
) {
    val progressAnim = remember(cellIndex) { Animatable(0f) }

    LaunchedEffect(cellIndex) {
        progressAnim.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    val slot = cellIndex % (PILE_COLS * PILE_ROWS)
    val slotSize = min(boxWidth, boxHeight) * 0.09f
    val pileStartX = boxWidth * 0.98f - slotSize
    val pileTopY = boxHeight * 0.06f
    val targetX = pileStartX - (slot % PILE_COLS) * (slotSize * 1.05f)
    val targetY = pileTopY + (slot / PILE_COLS) * (slotSize * 1.05f)

    val cellPx = remember(cell, boxWidth, boxHeight) { scalePath(cell.path, boxWidth, boxHeight) }
    val originX = cell.centroid.x * boxWidth
    val originY = cell.centroid.y * boxHeight
    val spin = remember(cellIndex) { (Random(seed + cellIndex).nextFloat() - 0.5f) * 540f }

    val t = progressAnim.value
    val dx = (targetX - originX) * t
    val dy = (targetY - originY) * t
    val scale = 1f - 0.65f * t

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = dx
                translationY = dy
                rotationZ = spin * t
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(originX / boxWidth, originY / boxHeight)
            }
    ) {
        clipPath(cellPx) {
            drawImage(mountainBitmap, dstSize = IntSize(boxWidth.toInt(), boxHeight.toInt()))
        }
    }
}

@Composable
fun MountainSnapshot(
    progressFraction: Float,
    seed: Long,
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

            val cells = remember(seed) { buildShatterCells(CELL_COUNT, seed) }
            val breakOrder = remember(seed, cells) { cells.indices.shuffled(Random(seed)) }
            val brokenCount = (progressFraction * cells.size).toInt().coerceIn(0, cells.size)
            val brokenSet = remember(brokenCount, breakOrder) { breakOrder.take(brokenCount).toSet() }

            val remainingPath = remember(brokenSet, cells, w, h) {
                var path = silhouettePath(w, h)
                brokenSet.forEach { idx ->
                    val cellPx = scalePath(cells[idx].path, w, h)
                    val next = Path()
                    next.op(path, cellPx, PathOperation.Difference)
                    path = next
                }
                path
            }

            // sem animação, sem pedras voando — só o estado atual "congelado"
            Canvas(modifier = Modifier.fillMaxSize()) {
                clipPath(remainingPath) {
                    drawImage(bitmap, dstSize = IntSize(w.toInt(), h.toInt()))
                }
            }
        }
    }
}