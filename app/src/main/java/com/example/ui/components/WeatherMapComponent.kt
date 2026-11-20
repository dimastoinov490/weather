package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.remote.NetworkModule
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CoolCyan
import com.example.ui.theme.IceWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tan

private object RadarTileMemoryCache {
    val bitmaps = object : LruCache<String, ImageBitmap>(64) {}
    var latestRadarPath: String? = null
    var latestRadarTime: Long = 0L
}

private data class TileCoord(
    val x: Int,
    val y: Int,
    val dx: Int,
    val dy: Int
)

private data class LoadedMapTiles(
    val baseTiles: Map<Pair<Int, Int>, ImageBitmap>,
    val radarTiles: Map<Pair<Int, Int>, ImageBitmap>,
    val centerTileX: Double,
    val centerTileY: Double,
    val radarTimestamp: Long?
)

@Composable
fun LiveWeatherRadarMap(
    latitude: Double,
    longitude: Double,
    zoom: Int,
    showRadarOverlay: Boolean = true,
    modifier: Modifier = Modifier
) {
    var loadedTiles by remember { mutableStateOf<LoadedMapTiles?>(null) }

    LaunchedEffect(latitude, longitude, zoom, showRadarOverlay) {
        loadedTiles = loadTilesForCoordinate(latitude, longitude, zoom, showRadarOverlay)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0F1A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle dark stylized grid fallback underneath tiles
            val gridStep = 44f
            var gx = 0f
            while (gx < w) {
                drawLine(
                    color = Color(0xFF1A2438).copy(alpha = 0.45f),
                    start = Offset(gx, 0f),
                    end = Offset(gx, h),
                    strokeWidth = 1f
                )
                gx += gridStep
            }
            var gy = 0f
            while (gy < h) {
                drawLine(
                    color = Color(0xFF1A2438).copy(alpha = 0.45f),
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1f
                )
                gy += gridStep
            }

            val data = loadedTiles
            if (data != null && data.baseTiles.isNotEmpty()) {
                val tileDrawSize = 256f * 1.45f
                val baseX = floor(data.centerTileX).toInt()
                val baseY = floor(data.centerTileY).toInt()
                val fracX = (data.centerTileX - baseX).toFloat()
                val fracY = (data.centerTileY - baseY).toFloat()

                val originLeft = w / 2f - fracX * tileDrawSize
                val originTop = h / 2f - fracY * tileDrawSize

                for (dx in -2..2) {
                    for (dy in -2..2) {
                        val key = dx to dy
                        val left = (originLeft + dx * tileDrawSize).roundToInt()
                        val top = (originTop + dy * tileDrawSize).roundToInt()
                        val sz = tileDrawSize.roundToInt() + 1

                        data.baseTiles[key]?.let { bmp ->
                            drawImage(
                                image = bmp,
                                dstOffset = IntOffset(left, top),
                                dstSize = IntSize(sz, sz)
                            )
                        }
                        if (showRadarOverlay) {
                            data.radarTiles[key]?.let { radarBmp ->
                                drawImage(
                                    image = radarBmp,
                                    dstOffset = IntOffset(left, top),
                                    dstSize = IntSize(sz, sz),
                                    alpha = 0.78f
                                )
                            }
                        }
                    }
                }
            } else {
                // Soft ambient radar sweep glow while loading or offline
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CoolCyan.copy(alpha = 0.22f),
                            Color(0xFF22D3EE).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.72f, h * 0.42f),
                        radius = size.minDimension * 0.62f
                    ),
                    radius = size.minDimension * 0.62f,
                    center = Offset(w * 0.72f, h * 0.42f)
                )
            }

            // Soft vignette for card polish
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color(0x66060910)),
                    center = Offset(w / 2f, h / 2f),
                    radius = size.maxDimension * 0.78f
                )
            )
        }

        // Glowing Amber Location Pin at center
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(34.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AmberPrimary.copy(alpha = 0.48f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2f
                )
            }
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null,
                tint = AmberPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        if (showRadarOverlay && loadedTiles?.radarTimestamp != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xAA0A0E18))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Радар осадков • Live",
                    style = MaterialTheme.typography.labelSmall,
                    color = IceWhite.copy(alpha = 0.86f)
                )
            }
        }
    }
}

private suspend fun loadTilesForCoordinate(
    lat: Double,
    lon: Double,
    zoom: Int,
    includeRadar: Boolean
): LoadedMapTiles = withContext(Dispatchers.IO) {
    val n = 2.0.pow(zoom)
    val clampedLat = lat.coerceIn(-85.0, 85.0)
    val latRad = clampedLat * PI / 180.0
    val centerTileX = ((lon + 180.0) / 360.0) * n
    val centerTileY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n

    val baseX = floor(centerTileX).toInt()
    val baseY = floor(centerTileY).toInt()
    val maxTile = n.toInt()

    val radarPath = if (includeRadar) {
        RadarTileMemoryCache.latestRadarPath ?: runCatching {
            val resp = NetworkModule.rainViewerApi.getRadarMaps()
            val frame = resp.radar?.past?.lastOrNull()
            if (frame != null) {
                val full = "${resp.host}${frame.path}"
                RadarTileMemoryCache.latestRadarPath = full
                RadarTileMemoryCache.latestRadarTime = frame.time
                full
            } else null
        }.getOrNull()
    } else null

    val coords = mutableListOf<TileCoord>()
    for (dx in -1..1) {
        for (dy in -1..1) {
            val tx = ((baseX + dx) % maxTile + maxTile) % maxTile
            val ty = (baseY + dy).coerceIn(0, maxTile - 1)
            coords.add(TileCoord(tx, ty, dx, dy))
        }
    }

    val baseMap = mutableMapOf<Pair<Int, Int>, ImageBitmap>()
    val radarMap = mutableMapOf<Pair<Int, Int>, ImageBitmap>()

    coroutineScope {
        coords.map { tc ->
            async {
                val baseUrl = "https://a.basemaps.cartocdn.com/dark_all/$zoom/${tc.x}/${tc.y}.png"
                fetchBitmapCached(baseUrl)?.let { bmp ->
                    synchronized(baseMap) { baseMap[tc.dx to tc.dy] = bmp }
                }
                if (radarPath != null) {
                    val radarUrl = "$radarPath/256/$zoom/${tc.x}/${tc.y}/2/1_1.png"
                    fetchBitmapCached(radarUrl)?.let { bmp ->
                        synchronized(radarMap) { radarMap[tc.dx to tc.dy] = bmp }
                    }
                }
            }
        }.awaitAll()
    }

    LoadedMapTiles(
        baseTiles = baseMap,
        radarTiles = radarMap,
        centerTileX = centerTileX,
        centerTileY = centerTileY,
        radarTimestamp = if (radarPath != null) RadarTileMemoryCache.latestRadarTime else null
    )
}

private fun fetchBitmapCached(url: String): ImageBitmap? {
    RadarTileMemoryCache.bitmaps.get(url)?.let { return it }
    return runCatching {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "NeboWeatherAndroid/1.0")
            .build()
        NetworkModule.okHttpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val bytes = resp.body?.bytes() ?: return null
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val img = bitmap.asImageBitmap()
            RadarTileMemoryCache.bitmaps.put(url, img)
            img
        }
    }.getOrNull()
}
