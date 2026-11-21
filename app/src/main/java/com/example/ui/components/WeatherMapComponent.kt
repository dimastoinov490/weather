package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File
import kotlin.math.roundToInt

enum class WeatherMapMode {
    MAP,
    FORECAST,
    PRECIPITATION
}

data class RadarFrameInfo(
    val timeEpochSec: Long,
    val path: String,
    val isNowcast: Boolean
)

data class WeatherMapMarkerInfo(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val temperatureText: String,
    val conditionText: String,
    val iconEmoji: String,
    val humidityText: String,
    val windText: String,
    val isCurrentLocation: Boolean = false
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
    mapMode: WeatherMapMode,
    isRadarEnabled: Boolean,
    selectedRadarIndex: Int,
    locationRecenterToken: Int,
    isOnline: Boolean,
    onZoomChanged: (Int) -> Unit,
    onRadarFramesLoaded: (frames: List<RadarFrameInfo>, defaultLiveIndex: Int) -> Unit,
    onRadarStatusMessage: (String?) -> Unit,
    onControllerReady: (WeatherMapController) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestZoomChanged by rememberUpdatedState(onZoomChanged)
    val latestRadarFramesLoaded by rememberUpdatedState(onRadarFramesLoaded)
    val latestRadarStatus by rememberUpdatedState(onRadarStatusMessage)
    val latestMarkerInfo by rememberUpdatedState(markerInfo)

    var radarRetryTrigger by remember { mutableIntStateOf(0) }
    var radarHost by remember { mutableStateOf("https://tilecache.rainviewer.com") }
    var radarFrames by remember { mutableStateOf<List<RadarFrameInfo>>(emptyList()) }
    var activeRadarPath by remember { mutableStateOf<String?>(null) }
    var activeRadarOverlay by remember { mutableStateOf<TilesOverlay?>(null) }
    var activeRadarProvider by remember { mutableStateOf<MapTileProviderBasic?>(null) }

    // Configure osmdroid with a policy-compliant User-Agent and clean cache paths before MapView creation
    remember(context) {
        val osmConfig = Configuration.getInstance()
        val prefs = context.getSharedPreferences("osmdroid_weather_prefs", Context.MODE_PRIVATE)
        osmConfig.load(context, prefs)
        osmConfig.userAgentValue = "${context.packageName}/1.0 (Android; NeboWeather)"
        val cacheRoot = File(context.cacheDir, "osmdroid").apply { mkdirs() }
        val tilesDir = File(cacheRoot, "tiles").apply { mkdirs() }
        osmConfig.osmdroidBasePath = cacheRoot
        osmConfig.osmdroidTileCache = tilesDir
        runCatching { File(tilesDir, "CartoDarkMatterNavy").deleteRecursively() }
        true
    }

    val mapView = remember {
        MapView(context).apply {
            setBackgroundColor(AndroidColor.parseColor("#07111F"))
            setTileSource(TileSourceFactory.MAPNIK)
            setUseDataConnection(true)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 18.0
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(initialZoom.toDouble())
            controller.setCenter(GeoPoint(markerInfo.latitude, markerInfo.longitude))

            // Luminance-inversion + deep-navy tint matching #07111F / #0B1930 map aesthetic
            val darkNavyOsmMatrix = ColorMatrix(
                floatArrayOf(
                    -0.281f, -0.552f, -0.107f, 0f, 236f,
                    -0.284f, -0.558f, -0.108f, 0f, 245f,
                    -0.392f, -0.534f, 0.016f, 0f, 254f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(darkNavyOsmMatrix))
            overlayManager.tilesOverlay.loadingBackgroundColor = AndroidColor.parseColor("#07111F")
            overlayManager.tilesOverlay.loadingLineColor = AndroidColor.parseColor("#203858")
        }
    }

    val pulsingLocationOverlay = remember(context) {
        PulsingLocationOverlay(
            context = context,
            initialPoint = GeoPoint(markerInfo.latitude, markerInfo.longitude)
        )
    }

    val cityMarker = remember(mapView) {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = createMarkerHitboxDrawable(context)
        }
    }

    var lastCenteredLatLon by remember {
        mutableStateOf(markerInfo.latitude to markerInfo.longitude)
    }

    DisposableEffect(mapView, lifecycleOwner) {
        if (!mapView.overlays.contains(pulsingLocationOverlay)) {
            mapView.overlays.add(pulsingLocationOverlay)
        }
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
                    mapView.controller.animateTo(
                        target,
                        mapView.zoomLevelDouble.coerceAtLeast(8.0),
                        550L
                    )
                },
                onReloadRadar = {
                    mapView.invalidate()
                    radarRetryTrigger++
                }
            )
        )

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    pulsingLocationOverlay.isAnimating = true
                    mapView.onResume()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    pulsingLocationOverlay.isAnimating = false
                    mapView.onPause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        pulsingLocationOverlay.isAnimating = true
        mapView.onResume()

        onDispose {
            pulsingLocationOverlay.isAnimating = false
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.removeMapListener(listener)
            activeRadarProvider?.detach()
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Fetch RainViewer past & nowcast radar frames
    LaunchedEffect(isOnline, radarRetryTrigger) {
        if (!isOnline) {
            latestRadarStatus(context.getString(R.string.map_offline_message))
            return@LaunchedEffect
        }

        val result = runCatching { NetworkModule.rainViewerApi.getRadarMaps() }
        result.onSuccess { response ->
            val past = response.radar?.past.orEmpty()
                .filter { it.path.isNotBlank() }
                .map { RadarFrameInfo(timeEpochSec = it.time, path = it.path, isNowcast = false) }
            val nowcast = response.radar?.nowcast.orEmpty()
                .filter { it.path.isNotBlank() }
                .map { RadarFrameInfo(timeEpochSec = it.time, path = it.path, isNowcast = true) }
            val allFrames = past + nowcast
            if (allFrames.isNotEmpty()) {
                radarHost = response.host.trimEnd('/')
                radarFrames = allFrames
                val defaultLiveIndex = (past.size - 1).coerceAtLeast(0)
                latestRadarFramesLoaded(allFrames, defaultLiveIndex)
                latestRadarStatus(null)
            } else {
                radarFrames = emptyList()
                latestRadarFramesLoaded(emptyList(), 0)
                latestRadarStatus(context.getString(R.string.map_tile_error_message))
            }
        }.onFailure {
            latestRadarStatus(context.getString(R.string.map_tile_error_message))
        }
    }

    // Attach, update, or remove RainViewer precipitation overlay based on toggle, frame, and mode
    LaunchedEffect(isRadarEnabled, radarHost, radarFrames, selectedRadarIndex, mapMode) {
        if (!isRadarEnabled) {
            activeRadarOverlay?.let { oldOverlay ->
                mapView.overlays.remove(oldOverlay)
            }
            activeRadarProvider?.detach()
            activeRadarPath = null
            activeRadarOverlay = null
            activeRadarProvider = null
            mapView.invalidate()
            return@LaunchedEffect
        }

        val frame = radarFrames.getOrNull(selectedRadarIndex)
            ?: radarFrames.lastOrNull()
            ?: return@LaunchedEffect

        val targetAlpha = when (mapMode) {
            WeatherMapMode.MAP -> 0.74f
            WeatherMapMode.FORECAST -> 0.78f
            WeatherMapMode.PRECIPITATION -> 0.90f
        }
        val radarColorFilter = ColorMatrixColorFilter(
            ColorMatrix().apply { setScale(1f, 1.02f, 1.06f, targetAlpha) }
        )

        if (activeRadarPath == frame.path && activeRadarOverlay != null) {
            activeRadarOverlay?.setColorFilter(radarColorFilter)
            mapView.invalidate()
            return@LaunchedEffect
        }

        val host = radarHost
        val path = frame.path
        val radarTileSource = object : OnlineTileSourceBase(
            "RainViewer_${frame.timeEpochSec}",
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
                // Scheme 2 = Universal Blue (cyan -> blue -> purple) with smooth interpolation (1_1)
                return "$host$path/256/$z/$x/$y/2/1_1.png"
            }
        }

        val tileProvider = MapTileProviderBasic(context, radarTileSource).apply {
            setUseDataConnection(true)
        }
        val radarOverlay = TilesOverlay(tileProvider, context).apply {
            loadingBackgroundColor = AndroidColor.TRANSPARENT
            loadingLineColor = AndroidColor.TRANSPARENT
            setColorFilter(radarColorFilter)
        }

        activeRadarOverlay?.let { oldOverlay ->
            mapView.overlays.remove(oldOverlay)
        }
        activeRadarProvider?.detach()

        val pulseIndex = mapView.overlays.indexOf(pulsingLocationOverlay)
        if (pulseIndex >= 0) {
            mapView.overlays.add(pulseIndex, radarOverlay)
        } else {
            mapView.overlays.add(radarOverlay)
            mapView.overlays.add(pulsingLocationOverlay)
            mapView.overlays.add(cityMarker)
        }

        activeRadarPath = frame.path
        activeRadarProvider = tileProvider
        activeRadarOverlay = radarOverlay
        mapView.invalidate()
    }

    // Update pulsing marker metadata & coordinates and animate map on city switch
    LaunchedEffect(
        markerInfo.latitude,
        markerInfo.longitude,
        markerInfo.cityName,
        markerInfo.temperatureText,
        markerInfo.conditionText,
        markerInfo.humidityText,
        markerInfo.windText,
        markerInfo.isCurrentLocation
    ) {
        val point = GeoPoint(markerInfo.latitude, markerInfo.longitude)
        pulsingLocationOverlay.geoPoint = point
        pulsingLocationOverlay.isCurrentGpsLocation = markerInfo.isCurrentLocation
        cityMarker.position = point
        cityMarker.title = "${markerInfo.cityName} • ${markerInfo.temperatureText}"
        cityMarker.snippet =
            "${markerInfo.conditionText} • Влажность: ${markerInfo.humidityText}, Ветер: ${markerInfo.windText}"

        val coordsChanged = kotlin.math.abs(lastCenteredLatLon.first - markerInfo.latitude) > 0.0001 ||
            kotlin.math.abs(lastCenteredLatLon.second - markerInfo.longitude) > 0.0001
        if (coordsChanged) {
            lastCenteredLatLon = markerInfo.latitude to markerInfo.longitude
            mapView.controller.animateTo(point)
        }
        mapView.invalidate()
    }

    // Always fly & zoom to user's GPS location whenever GPS detection finishes
    LaunchedEffect(locationRecenterToken) {
        if (locationRecenterToken > 0) {
            val point = GeoPoint(latestMarkerInfo.latitude, latestMarkerInfo.longitude)
            pulsingLocationOverlay.geoPoint = point
            pulsingLocationOverlay.isCurrentGpsLocation = true
            lastCenteredLatLon = point.latitude to point.longitude
            val targetZoom = mapView.zoomLevelDouble.coerceAtLeast(10.0)
            mapView.controller.animateTo(point, targetZoom, 600L)
        }
    }

    Box(
        modifier = modifier
            .background(MapBgPrimary)
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
                    .background(Color(0xA607111F)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MapBgSecondary,
                    border = BorderStroke(1.dp, MapElementBorder),
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
                            tint = MapAccentPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Карта недоступна",
                            style = MaterialTheme.typography.titleLarge,
                            color = MapTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.map_offline_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MapTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { radarRetryTrigger++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MapActiveGlow,
                                contentColor = MapTextPrimary
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

private class PulsingLocationOverlay(
    context: Context,
    initialPoint: GeoPoint
) : Overlay() {
    var geoPoint: GeoPoint = initialPoint
    var isCurrentGpsLocation: Boolean = false
    var isAnimating: Boolean = true

    private val density = context.resources.displayMetrics.density
    private val screenPoint = Point()

    private val pulseRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pulseStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * density
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val coreFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#2478D4")
        style = Paint.Style.FILL
    }
    private val whiteRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.6f * density
    }

    override fun draw(canvas: AndroidCanvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        mapView.projection.toPixels(geoPoint, screenPoint)
        val cx = screenPoint.x.toFloat()
        val cy = screenPoint.y.toFloat()

        // Smooth 1.8s pulse cycle
        val phase = (SystemClock.uptimeMillis() % 1800L) / 1800f
        val maxPulseR = (if (isCurrentGpsLocation) 30f else 24f) * density
        val minPulseR = 9f * density
        val pulseR = minPulseR + (maxPulseR - minPulseR) * phase
        val pulseAlpha = ((1f - phase) * (1f - phase) * 150f).roundToInt().coerceIn(0, 255)

        pulseRingPaint.color = AndroidColor.argb((pulseAlpha * 0.45f).roundToInt(), 54, 155, 255)
        canvas.drawCircle(cx, cy, pulseR, pulseRingPaint)

        pulseStrokePaint.color = AndroidColor.argb(pulseAlpha, 101, 184, 255)
        canvas.drawCircle(cx, cy, pulseR, pulseStrokePaint)

        val glowR = 20f * density
        glowPaint.shader = RadialGradient(
            cx,
            cy,
            glowR,
            intArrayOf(
                AndroidColor.argb(190, 101, 184, 255),
                AndroidColor.argb(110, 36, 120, 212),
                AndroidColor.argb(0, 36, 120, 212)
            ),
            floatArrayOf(0.25f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, glowR, glowPaint)

        val coreR = 8.2f * density
        canvas.drawCircle(cx, cy, coreR, coreFillPaint)
        canvas.drawCircle(cx, cy, coreR, whiteRingPaint)

        if (isAnimating) {
            mapView.postInvalidateDelayed(33L)
        }
    }
}

private fun createMarkerHitboxDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (32f * density).roundToInt().coerceAtLeast(24)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    return BitmapDrawable(context.resources, bitmap)
}
