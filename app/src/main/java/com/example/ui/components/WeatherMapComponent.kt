package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color as AndroidColor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.IceWhite
import com.example.ui.theme.ObsidianBg
import org.json.JSONObject

enum class WeatherMapLayer(val jsKey: String, val label: String) {
    PRECIPITATION("precipitation", "Осадки"),
    CLOUDS("clouds", "Облачность")
}

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
    internal val onReload: () -> Unit = {}
) {
    fun zoomIn() = onZoomIn()
    fun zoomOut() = onZoomOut()
    fun recenter() = onRecenter()
    fun reload() = onReload()
}

private class WeatherMapJsBridge(
    private val mainHandler: Handler,
    private val onMapReadyCallback: () -> Unit,
    private val onZoomChangedCallback: (Int) -> Unit,
    private val onLayerStatusCallback: (layerKey: String, timestampEpochSec: Long, isFallback: Boolean) -> Unit
) {
    @JavascriptInterface
    fun onMapReady() {
        mainHandler.post { onMapReadyCallback() }
    }

    @JavascriptInterface
    fun onZoomChanged(zoom: Int) {
        mainHandler.post { onZoomChangedCallback(zoom) }
    }

    @JavascriptInterface
    fun onLayerStatus(layerKey: String, timestampEpochSec: Long, isFallback: Boolean) {
        mainHandler.post { onLayerStatusCallback(layerKey, timestampEpochSec, isFallback) }
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveWeatherWebViewMap(
    markerInfo: WeatherMapMarkerInfo,
    activeLayer: WeatherMapLayer,
    zoom: Int,
    onZoomChanged: (Int) -> Unit,
    onLayerStatusChanged: (layerKey: String, timestampEpochSec: Long, isFallback: Boolean) -> Unit,
    onControllerReady: (WeatherMapController) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val latestZoomChanged by rememberUpdatedState(onZoomChanged)
    val latestLayerStatusChanged by rememberUpdatedState(onLayerStatusChanged)

    var isPageReady by remember { mutableStateOf(false) }
    var isMapReady by remember { mutableStateOf(false) }
    var hasLoadError by remember { mutableStateOf(false) }

    val initialHtml = remember {
        buildLeafletWeatherHtml(
            initialLat = markerInfo.latitude,
            initialLon = markerInfo.longitude,
            initialZoom = zoom,
            initialLayer = activeLayer.jsKey,
            marker = markerInfo
        )
    }

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.parseColor("#0B0F18"))
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                loadsImagesAutomatically = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = false
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                userAgentString = "${userAgentString.orEmpty()} NeboWeather/1.0 (Android)"
            }

            addJavascriptInterface(
                WeatherMapJsBridge(
                    mainHandler = mainHandler,
                    onMapReadyCallback = {
                        isMapReady = true
                        hasLoadError = false
                    },
                    onZoomChangedCallback = { newZoom ->
                        latestZoomChanged(newZoom)
                    },
                    onLayerStatusCallback = { layerKey, ts, isFallback ->
                        latestLayerStatusChanged(layerKey, ts, isFallback)
                    }
                ),
                "AndroidMapBridge"
            )

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isPageReady = true
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        hasLoadError = true
                    }
                }
            }

            loadDataWithBaseURL(
                "https://nebo.weather.app/",
                initialHtml,
                "text/html",
                "UTF-8",
                null
            )
        }
    }

    DisposableEffect(webView) {
        onControllerReady(
            WeatherMapController(
                onZoomIn = { webView.evaluateJavascript("window.zoomInMap && window.zoomInMap();", null) },
                onZoomOut = { webView.evaluateJavascript("window.zoomOutMap && window.zoomOutMap();", null) },
                onRecenter = { webView.evaluateJavascript("window.recenterOnCity && window.recenterOnCity();", null) },
                onReload = {
                    hasLoadError = false
                    isMapReady = false
                    webView.loadDataWithBaseURL(
                        "https://nebo.weather.app/",
                        buildLeafletWeatherHtml(
                            initialLat = markerInfo.latitude,
                            initialLon = markerInfo.longitude,
                            initialZoom = zoom,
                            initialLayer = activeLayer.jsKey,
                            marker = markerInfo
                        ),
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            )
        )
        webView.onResume()
        onDispose {
            webView.onPause()
            webView.removeJavascriptInterface("AndroidMapBridge")
            webView.destroy()
        }
    }

    // Update marker and recenter when selected city or weather report updates
    LaunchedEffect(
        isPageReady,
        isMapReady,
        markerInfo.latitude,
        markerInfo.longitude,
        markerInfo.cityName,
        markerInfo.temperatureText,
        markerInfo.conditionText,
        markerInfo.iconEmoji,
        markerInfo.humidityText,
        markerInfo.windText
    ) {
        if (isPageReady) {
            val jsCall = """
                window.updateWeatherMarker && window.updateWeatherMarker(
                    ${markerInfo.latitude},
                    ${markerInfo.longitude},
                    ${JSONObject.quote(markerInfo.cityName)},
                    ${JSONObject.quote(markerInfo.temperatureText)},
                    ${JSONObject.quote(markerInfo.conditionText)},
                    ${JSONObject.quote(markerInfo.iconEmoji)},
                    ${JSONObject.quote(markerInfo.humidityText)},
                    ${JSONObject.quote(markerInfo.windText)}
                );
            """.trimIndent()
            webView.evaluateJavascript(jsCall, null)
        }
    }

    // Update active radar/cloud layer when toggle changes
    LaunchedEffect(isPageReady, isMapReady, activeLayer) {
        if (isPageReady) {
            val jsCall = "window.setWeatherLayer && window.setWeatherLayer(${JSONObject.quote(activeLayer.jsKey)});"
            webView.evaluateJavascript(jsCall, null)
        }
    }

    Box(
        modifier = modifier
            .background(ObsidianBg)
            .testTag("weather_map_webview_container")
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )

        if (!isMapReady && !hasLoadError) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xD90D121E))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(
                        color = AmberPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Загрузка карты и радара…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IceWhite
                    )
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

private fun buildLeafletWeatherHtml(
    initialLat: Double,
    initialLon: Double,
    initialZoom: Int,
    initialLayer: String,
    marker: WeatherMapMarkerInfo
): String {
    val cityJson = JSONObject.quote(marker.cityName)
    val tempJson = JSONObject.quote(marker.temperatureText)
    val condJson = JSONObject.quote(marker.conditionText)
    val emojiJson = JSONObject.quote(marker.iconEmoji)
    val humidityJson = JSONObject.quote(marker.humidityText)
    val windJson = JSONObject.quote(marker.windText)
    val layerJson = JSONObject.quote(initialLayer)

    return """
<!DOCTYPE html>
<html lang="ru">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>Небо — Карта погоды</title>
  <link id="leaflet-css" rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <style>
    html, body, #map {
      margin: 0;
      padding: 0;
      width: 100%;
      height: 100%;
      background: #0B0F18;
      overflow: hidden;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
    }
    .osm-base-tiles {
      filter: brightness(0.88) contrast(1.06) saturate(0.92);
    }
    .leaflet-control-attribution {
      background: rgba(11, 15, 24, 0.72) !important;
      color: rgba(248, 250, 252, 0.7) !important;
      font-size: 10px !important;
      border-top-right-radius: 8px;
      padding: 2px 6px !important;
    }
    .leaflet-control-attribution a {
      color: #F59E0B !important;
      text-decoration: none;
    }
    .nebo-marker-wrap {
      background: transparent;
      border: none;
    }
    .nebo-marker-card {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 7px 12px;
      border-radius: 16px;
      background: rgba(13, 18, 30, 0.92);
      border: 1.5px solid rgba(245, 158, 11, 0.85);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.48), 0 0 14px rgba(245, 158, 11, 0.28);
      color: #F8FAFC;
      white-space: nowrap;
      transform: translate(-50%, -100%);
      position: relative;
    }
    .nebo-marker-card::after {
      content: "";
      position: absolute;
      left: 50%;
      bottom: -7px;
      transform: translateX(-50%) rotate(45deg);
      width: 11px;
      height: 11px;
      background: rgba(13, 18, 30, 0.94);
      border-right: 1.5px solid rgba(245, 158, 11, 0.85);
      border-bottom: 1.5px solid rgba(245, 158, 11, 0.85);
    }
    .nebo-emoji {
      font-size: 20px;
      line-height: 1;
    }
    .nebo-info {
      display: flex;
      flex-direction: column;
      line-height: 1.15;
    }
    .nebo-top {
      display: flex;
      align-items: baseline;
      gap: 6px;
    }
    .nebo-temp {
      font-size: 16px;
      font-weight: 700;
      color: #F59E0B;
    }
    .nebo-city {
      font-size: 13px;
      font-weight: 600;
      color: #F8FAFC;
    }
    .nebo-cond {
      font-size: 11px;
      color: #CBD5E1;
      margin-top: 1px;
    }
    .leaflet-popup-content-wrapper, .leaflet-popup-tip {
      background: rgba(13, 18, 30, 0.95) !important;
      color: #F8FAFC !important;
      border: 1px solid rgba(245, 158, 11, 0.45);
      border-radius: 14px !important;
    }
    .leaflet-popup-close-button {
      color: #CBD5E1 !important;
    }
  </style>
</head>
<body>
  <div id="map"></div>

  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <script>
    (function ensureLeafletAndBoot() {
      if (typeof L !== 'undefined') {
        bootMap();
        return;
      }
      var fallbackCss = document.createElement('link');
      fallbackCss.rel = 'stylesheet';
      fallbackCss.href = 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css';
      document.head.appendChild(fallbackCss);

      var fallbackScript = document.createElement('script');
      fallbackScript.src = 'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js';
      fallbackScript.onload = bootMap;
      document.head.appendChild(fallbackScript);
    })();

    var map = null;
    var weatherMarker = null;
    var precipitationLayer = null;
    var cloudsLayer = null;
    var currentLayerKey = $layerJson;
    var lastCityCoords = [$initialLat, $initialLon];
    var radarTimestamp = 0;
    var cloudsTimestamp = 0;
    var cloudsIsFallback = false;

    function escapeHtml(str) {
      if (!str) return '';
      return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }

    function createWeatherIcon(tempText, cityName, conditionText, iconEmoji) {
      var html =
        '<div class="nebo-marker-card">' +
          '<span class="nebo-emoji">' + escapeHtml(iconEmoji) + '</span>' +
          '<div class="nebo-info">' +
            '<div class="nebo-top">' +
              '<span class="nebo-temp">' + escapeHtml(tempText) + '</span>' +
              '<span class="nebo-city">' + escapeHtml(cityName) + '</span>' +
            '</div>' +
            '<div class="nebo-cond">' + escapeHtml(conditionText) + '</div>' +
          '</div>' +
        '</div>';
      return L.divIcon({
        className: 'nebo-marker-wrap',
        html: html,
        iconSize: [0, 0],
        iconAnchor: [0, 8]
      });
    }

    function formatPopupHtml(cityName, tempText, conditionText, iconEmoji, humidityText, windText) {
      return (
        '<div style="min-width:155px;padding:2px 2px;">' +
          '<div style="font-size:15px;font-weight:700;margin-bottom:4px;">' +
            escapeHtml(iconEmoji) + ' ' + escapeHtml(cityName) + ' • <span style="color:#F59E0B;">' + escapeHtml(tempText) + '</span>' +
          '</div>' +
          '<div style="font-size:12px;color:#E2E8F0;margin-bottom:6px;">' + escapeHtml(conditionText) + '</div>' +
          '<div style="font-size:11px;color:#94A3B8;">Влажность: <b style="color:#F8FAFC;">' + escapeHtml(humidityText) + '</b></div>' +
          '<div style="font-size:11px;color:#94A3B8;">Ветер: <b style="color:#F8FAFC;">' + escapeHtml(windText) + '</b></div>' +
        '</div>'
      );
    }

    function notifyAndroidLayerStatus() {
      if (!window.AndroidMapBridge) return;
      if (currentLayerKey === 'precipitation') {
        window.AndroidMapBridge.onLayerStatus('precipitation', radarTimestamp, false);
      } else {
        window.AndroidMapBridge.onLayerStatus('clouds', cloudsTimestamp, cloudsIsFallback);
      }
    }

    function applyActiveLayer() {
      if (!map) return;
      if (precipitationLayer && map.hasLayer(precipitationLayer)) {
        map.removeLayer(precipitationLayer);
      }
      if (cloudsLayer && map.hasLayer(cloudsLayer)) {
        map.removeLayer(cloudsLayer);
      }
      if (currentLayerKey === 'precipitation' && precipitationLayer) {
        precipitationLayer.addTo(map);
      } else if (currentLayerKey === 'clouds' && cloudsLayer) {
        cloudsLayer.addTo(map);
      }
      notifyAndroidLayerStatus();
    }

    function buildYesterdayIsoDate() {
      var d = new Date(Date.now() - 24 * 60 * 60 * 1000);
      return d.toISOString().slice(0, 10);
    }

    async function loadRainViewerLayers() {
      try {
        var resp = await fetch('https://api.rainviewer.com/public/weather-maps.json');
        var data = await resp.json();
        var host = (data && data.host) ? data.host : 'https://tilecache.rainviewer.com';

        if (data && data.radar) {
          var past = data.radar.past || [];
          var nowcast = data.radar.nowcast || [];
          var frame = past.length > 0 ? past[past.length - 1] : (nowcast.length > 0 ? nowcast[0] : null);
          if (frame && frame.path) {
            radarTimestamp = frame.time || 0;
            var radarUrl = host + frame.path + '/256/{z}/{x}/{y}/2/1_1.png';
            precipitationLayer = L.tileLayer(radarUrl, {
              tileSize: 256,
              maxNativeZoom: 7,
              maxZoom: 18,
              opacity: 0.74,
              zIndex: 200,
              attribution: '&copy; RainViewer'
            });
          }
        }

        if (data && data.satellite && data.satellite.infrared && data.satellite.infrared.length > 0) {
          var satFrames = data.satellite.infrared;
          var satFrame = satFrames[satFrames.length - 1];
          cloudsTimestamp = satFrame.time || 0;
          cloudsIsFallback = false;
          var satUrl = host + satFrame.path + '/256/{z}/{x}/{y}/0/0_0.png';
          cloudsLayer = L.tileLayer(satUrl, {
            tileSize: 256,
            maxNativeZoom: 7,
            maxZoom: 18,
            opacity: 0.65,
            zIndex: 200,
            attribution: '&copy; RainViewer'
          });
        } else {
          // Fallback free keyless global cloud cover layer if RainViewer satellite feed is omitted
          cloudsTimestamp = 0;
          cloudsIsFallback = true;
          var dateStr = buildYesterdayIsoDate();
          var gibsUrl = 'https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/MODIS_Terra_Cloud_Top_Temp_Day/default/' +
            dateStr + '/GoogleMapsCompatible_Level6/{z}/{y}/{x}.png';
          cloudsLayer = L.tileLayer(gibsUrl, {
            tileSize: 256,
            maxNativeZoom: 6,
            maxZoom: 18,
            opacity: 0.60,
            zIndex: 200,
            attribution: '&copy; NASA GIBS'
          });
        }

        applyActiveLayer();
      } catch (err) {
        console.error('RainViewer load error:', err);
      }
    }

    function bootMap() {
      if (map) return;

      map = L.map('map', {
        zoomControl: false,
        attributionControl: true,
        minZoom: 2,
        maxZoom: 18
      }).setView([$initialLat, $initialLon], $initialZoom);

      map.attributionControl.setPosition('bottomleft');
      map.attributionControl.setPrefix(false);

      // Base OpenStreetMap layer
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18,
        className: 'osm-base-tiles',
        attribution: '&copy; OpenStreetMap'
      }).addTo(map);

      // City temperature & condition marker
      weatherMarker = L.marker([$initialLat, $initialLon], {
        icon: createWeatherIcon($tempJson, $cityJson, $condJson, $emojiJson),
        zIndexOffset: 500
      }).addTo(map);

      weatherMarker.bindPopup(
        formatPopupHtml($cityJson, $tempJson, $condJson, $emojiJson, $humidityJson, $windJson),
        { offset: [0, -34] }
      );

      map.on('zoomend', function() {
        if (window.AndroidMapBridge) {
          window.AndroidMapBridge.onZoomChanged(map.getZoom());
        }
      });

      if (window.AndroidMapBridge) {
        window.AndroidMapBridge.onMapReady();
      }

      loadRainViewerLayers();
    }

    window.setWeatherLayer = function(layerKey) {
      currentLayerKey = layerKey;
      applyActiveLayer();
    };

    window.zoomInMap = function() {
      if (map) map.zoomIn();
    };

    window.zoomOutMap = function() {
      if (map) map.zoomOut();
    };

    window.recenterOnCity = function() {
      if (map && lastCityCoords) {
        map.flyTo(lastCityCoords, Math.max(map.getZoom(), 6), { duration: 0.65 });
      }
    };

    window.updateWeatherMarker = function(lat, lon, cityName, tempText, conditionText, iconEmoji, humidityText, windText) {
      var coordsChanged = Math.abs(lastCityCoords[0] - lat) > 0.0001 || Math.abs(lastCityCoords[1] - lon) > 0.0001;
      lastCityCoords = [lat, lon];
      if (!map) return;

      if (weatherMarker) {
        weatherMarker.setLatLng([lat, lon]);
        weatherMarker.setIcon(createWeatherIcon(tempText, cityName, conditionText, iconEmoji));
        weatherMarker.setPopupContent(
          formatPopupHtml(cityName, tempText, conditionText, iconEmoji, humidityText, windText)
        );
      }
      if (coordsChanged) {
        map.setView([lat, lon], map.getZoom());
      }
    };
  </script>
</body>
</html>
    """.trimIndent()
}
