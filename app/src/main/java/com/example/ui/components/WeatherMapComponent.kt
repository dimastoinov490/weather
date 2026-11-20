package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.TypedValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.remote.NetworkModule
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedText
import com.example.ui.theme.ObsidianBg
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import kotlin.math.roundToInt

data class WeatherMapMarkerInfo(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val temperatureText: String,
    val conditionText: String,
    val iconEmoji: String,
    val humidityText: String,
    val windText: String
)

data class WeatherMapController(
    internal val onZoomIn: () -> Unit = {},
    internal val onZoomOut: () -> Unit = {},
    internal val onRecenter: () -> Unit = {},
    internal val onReloadRadar: () -> Unit = {}
) {
    fun zoomIn() = onZoomIn()
    fun zoomOut() = onZoomOut()
    fun recenter() = onRecenter()
    fun reloadRadar() = onReloadRadar()
}

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun rememberNetworkAvailability(): Boolean {
    val context = LocalContext.current
    var available by remember { mutableStateOf(isNetworkAvailable(context)) }

    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            return@DisposableEffect onDispose {}
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                available = true
            }

            override fun onLost(network: Network) {
                available = isNetworkAvailable(context)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                available = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        onDispose {
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
    }
    return available
}

@Composable
fun NativeOsmdroidWeatherMap(
    markerInfo: WeatherMapMarkerInfo,
    initialZoom: Int,
    isOnline: Boolean,
    onZoomChanged: (Int) -> Unit,
    onRadarStatusMessage: (String?) -> Unit,
    onControllerReady: (WeatherMapController) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestZoomChanged by rememberUpdatedState(onZoomChanged)
    val latestRadarStatus by rememberUpdatedState(onRadarStatusMessage)
    val latestMarkerInfo by rememberUpdatedState(markerInfo)

    var radarRetryTrigger by remember { mutableIntStateOf(0) }
    var activeRadarOverlay by remember { mutableStateOf<TilesOverlay?>(null) }
    var activeRadarProvider by remember { mutableStateOf<MapTileProviderBasic?>(null) }

    // Configure osmdroid userAgentValue and app-private cache directory before creating MapView
    remember(context) {
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = context.packageName
        val cacheRoot = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        osmConfig.osmdroidBasePath = cacheRoot
        osmConfig.osmdroidTileCache = File(cacheRoot, "tiles").apply { mkdirs() }
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 18.0
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(initialZoom.toDouble())
            controller.setCenter(GeoPoint(markerInfo.latitude, markerInfo.longitude))
        }
    }

    val cityMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
    }

    // Track last centered city coordinates so tapping "Recenter" or switching cities moves cleanly
    var lastCenteredLatLon by remember {
        mutableStateOf(markerInfo.latitude to markerInfo.longitude)
    }

    // Attach zoom listener & marker once, handle lifecycle resume/pause/detach
    DisposableEffect(mapView, lifecycleOwner) {
        if (!mapView.overlays.contains(cityMarker)) {
            mapView.overlays.add(cityMarker)
        }

        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean = false

            override fun onZoom(event: ZoomEvent?): Boolean {
                val z = mapView.zoomLevelDouble.roundToInt().coerceIn(3, 18)
                latestZoomChanged(z)
                return false
            }
        }
        mapView.addMapListener(listener)

        onControllerReady(
            WeatherMapController(
                onZoomIn = { mapView.controller.zoomIn() },
                onZoomOut = { mapView.controller.zoomOut() },
                onRecenter = {
                    val info = latestMarkerInfo
                    val target = GeoPoint(info.latitude, info.longitude)
                    mapView.controller.animateTo(target, mapView.zoomLevelDouble.coerceAtLeast(7.0), 550L)
                },
                onReloadRadar = {
                    radarRetryTrigger++
                }
            )
        )

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.removeMapListener(listener)
            activeRadarProvider?.detach()
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Fetch latest RainViewer past radar frame and attach native TilesOverlay above OSM streets
    LaunchedEffect(isOnline, radarRetryTrigger) {
        if (!isOnline) {
            latestRadarStatus(context.getString(R.string.map_offline_message))
            return@LaunchedEffect
        }

        val result = runCatching { NetworkModule.rainViewerApi.getRadarMaps() }
        result.onSuccess { response ->
            val latestFrame = response.radar?.past?.lastOrNull()
            if (latestFrame != null && latestFrame.path.isNotBlank()) {
                val host = response.host.trimEnd('/')
                val path = latestFrame.path
                val radarTileSource = object : OnlineTileSourceBase(
                    "RainViewer_${latestFrame.time}",
                    0,
                    7,
                    256,
                    ".png",
                    arrayOf("$host$path/256/")
                ) {
                    override fun getTileURLString(pMapTileIndex: Long): String {
                        val z = MapTileIndex.getZoom(pMapTileIndex)
                        val x = MapTileIndex.getX(pMapTileIndex)
                        val y = MapTileIndex.getY(pMapTileIndex)
                        return "$host$path/256/$z/$x/$y/2/1_1.png"
                    }
                }

                // Replace any previous radar overlay cleanly
                activeRadarOverlay?.let { oldOverlay ->
                    mapView.overlays.remove(oldOverlay)
                }
                activeRadarProvider?.detach()

                val tileProvider = MapTileProviderBasic(context, radarTileSource).apply {
                    setUseDataConnection(true)
                }
                val alphaMatrix = ColorMatrix().apply {
                    setScale(1f, 1f, 1f, 0.70f)
                }
                val radarOverlay = TilesOverlay(tileProvider, context).apply {
                    loadingBackgroundColor = AndroidColor.TRANSPARENT
                    loadingLineColor = AndroidColor.TRANSPARENT
                    setColorFilter(ColorMatrixColorFilter(alphaMatrix))
                }

                // Keep city marker on top of the radar TilesOverlay
                val markerIndex = mapView.overlays.indexOf(cityMarker)
                if (markerIndex >= 0) {
                    mapView.overlays.add(markerIndex, radarOverlay)
                } else {
                    mapView.overlays.add(radarOverlay)
                    mapView.overlays.add(cityMarker)
                }

                activeRadarProvider = tileProvider
                activeRadarOverlay = radarOverlay
                latestRadarStatus(null)
                mapView.invalidate()
            } else {
                latestRadarStatus(context.getString(R.string.map_tile_error_message))
            }
        }.onFailure {
            latestRadarStatus(context.getString(R.string.map_tile_error_message))
        }
    }

    // Update marker appearance and recenter when city or temperature updates
    LaunchedEffect(
        markerInfo.latitude,
        markerInfo.longitude,
        markerInfo.cityName,
        markerInfo.temperatureText,
        markerInfo.conditionText,
        markerInfo.iconEmoji,
        markerInfo.humidityText,
        markerInfo.windText
    ) {
        val point = GeoPoint(markerInfo.latitude, markerInfo.longitude)
        cityMarker.position = point
        cityMarker.title = "${markerInfo.cityName} • ${markerInfo.temperatureText}"
        cityMarker.snippet = "${markerInfo.conditionText} • Влажность: ${markerInfo.humidityText}, Ветер: ${markerInfo.windText}"
        cityMarker.icon = createCityWeatherPinDrawable(
            context = context,
            emoji = markerInfo.iconEmoji,
            temperatureText = markerInfo.temperatureText,
            cityName = markerInfo.cityName
        )

        val coordsChanged = kotlin.math.abs(lastCenteredLatLon.first - markerInfo.latitude) > 0.0001 ||
            kotlin.math.abs(lastCenteredLatLon.second - markerInfo.longitude) > 0.0001
        if (coordsChanged) {
            lastCenteredLatLon = markerInfo.latitude to markerInfo.longitude
            mapView.controller.animateTo(point)
        }
        mapView.invalidate()
    }

    Box(
        modifier = modifier
            .background(ObsidianBg)
            .testTag("weather_map_native_container")
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        if (!isOnline && activeRadarOverlay == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x990A0816)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xEB151029),
                    border = BorderStroke(1.dp, Color(0x668B5CF6)),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 360.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Карта недоступна",
                            style = MaterialTheme.typography.titleLarge,
                            color = IceWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.map_offline_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { radarRetryTrigger++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF7C3AED),
                                contentColor = IceWhite
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("map_fatal_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(18.dp)
                            )
                            Text(text = stringResource(R.string.retry_button))
                        }
                    }
                }
            }
        }
    }
}

fun weatherConditionEmoji(weatherCode: Int, isDay: Boolean): String {
    return when (weatherCode) {
        0 -> if (isDay) "☀️" else "🌙"
        1 -> if (isDay) "🌤️" else "🌙"
        2 -> if (isDay) "⛅" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> if (isDay) "⛅" else "🌙"
    }
}

private fun createCityWeatherPinDrawable(
    context: Context,
    emoji: String,
    temperatureText: String,
    cityName: String
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val metrics = context.resources.displayMetrics

    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#38BDF8")
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15f, metrics)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#F8FAFC")
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13f, metrics)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15f, metrics)
    }

    val safeCity = if (cityName.length > 16) cityName.take(15) + "…" else cityName
    val emojiW = emojiPaint.measureText(emoji)
    val tempW = tempPaint.measureText(temperatureText)
    val cityW = cityPaint.measureText(safeCity)

    val hPad = 12f * density
    val gap = 6f * density
    val vPad = 8f * density
    val pointerH = 8f * density
    val shadowPad = 4f * density

    val pillW = (hPad * 2f + emojiW + gap + tempW + gap + cityW).coerceAtLeast(96f * density)
    val textHeight = tempPaint.fontMetrics.let { it.descent - it.ascent }
    val pillH = textHeight + vPad * 2f

    val totalW = (pillW + shadowPad * 2f).roundToInt().coerceAtLeast(1)
    val totalH = (pillH + pointerH + shadowPad * 2f).roundToInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val rect = RectF(shadowPad, shadowPad, shadowPad + pillW, shadowPad + pillH)
    val radius = 14f * density

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#EB140F2A")
        style = Paint.Style.FILL
        setShadowLayer(4f * density, 0f, 2f * density, AndroidColor.argb(140, 0, 0, 0))
    }
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#DDA78BFA")
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
    }

    canvas.drawRoundRect(rect, radius, radius, bgPaint)
    canvas.drawRoundRect(rect, radius, radius, borderPaint)

    // Pointer triangle at bottom center
    val cx = rect.centerX()
    val pointerPath = AndroidPath().apply {
        moveTo(cx - 6f * density, rect.bottom - 1f)
        lineTo(cx, rect.bottom + pointerH)
        lineTo(cx + 6f * density, rect.bottom - 1f)
        close()
    }
    canvas.drawPath(pointerPath, bgPaint)

    val baselineY = rect.centerY() - (tempPaint.fontMetrics.ascent + tempPaint.fontMetrics.descent) / 2f
    var cursorX = rect.left + hPad
    canvas.drawText(emoji, cursorX, baselineY, emojiPaint)
    cursorX += emojiW + gap
    canvas.drawText(temperatureText, cursorX, baselineY, tempPaint)
    cursorX += tempW + gap
    canvas.drawText(safeCity, cursorX, baselineY, cityPaint)

    return BitmapDrawable(context.resources, bitmap)
}
