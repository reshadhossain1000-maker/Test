package com.example.bakedrop.ui.radar

import android.content.Context
import android.graphics.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bakedrop.R
import com.example.bakedrop.databinding.ActivityDashboardBinding
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar

class RadarMapController(
    private val activity: AppCompatActivity,
    private val binding: ActivityDashboardBinding
) {
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var riderAnnotations: MutableList<PointAnnotation> = mutableListOf()
    var currentMerchantPoint: Point? = null

    companion object {
        val DHAKA_FALLBACK: Point = Point.fromLngLat(90.4125, 23.8103)
        private const val PREFS_NAME = "BakeDropPrefs"
        private const val KEY_LAST_LAT = "last_lat"
        private const val KEY_LAST_LNG = "last_lng"
    }

    fun initializeMap(onMapReady: () -> Unit) {
        binding.mapView.mapboxMap.loadStyle(Style.STANDARD) {
            binding.mapView.scalebar.updateSettings { enabled = false }
            binding.mapView.compass.updateSettings { enabled = false }
            binding.mapView.logo.updateSettings { enabled = false }
            binding.mapView.attribution.updateSettings { enabled = false }

            // 🔒 🌟 ম্যাপের সব ম্যানুয়াল আঙুলের টাচ গেসচার ১০০% লক করা
            binding.mapView.gestures.updateSettings {
                scrollEnabled = false
                pinchToZoomEnabled = false
                doubleTapToZoomInEnabled = false
                doubleTouchToZoomOutEnabled = false
                quickZoomEnabled = false
                pitchEnabled = false
                rotateEnabled = false
            }

            loadSavedLocationOrFallback()
            pointAnnotationManager = binding.mapView.annotations.createPointAnnotationManager()

            setupMapControls()
            onMapReady()
        }
    }

    private fun setupMapControls() {
        // 🎯 Soft White GPS Recenter Button
        binding.btnRecenterMap.setOnClickListener {
            val target = currentMerchantPoint ?: DHAKA_FALLBACK
            centerMapOn(target)
            Toast.makeText(activity, "Centered on your location", Toast.LENGTH_SHORT).show()
        }

        // 🔍 Zoom In (+) Button
        binding.btnZoomIn.setOnClickListener {
            val currentZoom = binding.mapView.mapboxMap.cameraState.zoom
            binding.mapView.mapboxMap.flyTo(
                CameraOptions.Builder().zoom(currentZoom + 1.0).build(),
                MapAnimationOptions.mapAnimationOptions { duration(350) }
            )
        }

        // 🔍 Zoom Out (−) Button
        binding.btnZoomOut.setOnClickListener {
            val currentZoom = binding.mapView.mapboxMap.cameraState.zoom
            binding.mapView.mapboxMap.flyTo(
                CameraOptions.Builder().zoom(currentZoom - 1.0).build(),
                MapAnimationOptions.mapAnimationOptions { duration(350) }
            )
        }
    }

    fun onLocationAcquired(point: Point, lat: Double, lng: Double) {
        currentMerchantPoint = point
        saveLocationToPrefs(lat, lng)
        centerMapOn(point)
        enableLiveLocationPuck()
        spawnStaticRidersOnRoads(point)
    }

    private fun saveLocationToPrefs(lat: Double, lng: Double) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_LAST_LAT, lat.toFloat()).putFloat(KEY_LAST_LNG, lng.toFloat()).apply()
    }

    private fun loadSavedLocationOrFallback() {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLat = prefs.getFloat(KEY_LAST_LAT, 0f).toDouble()
        val savedLng = prefs.getFloat(KEY_LAST_LNG, 0f).toDouble()

        val initialPoint = if (savedLat != 0.0 && savedLng != 0.0) {
            Point.fromLngLat(savedLng, savedLat)
        } else {
            DHAKA_FALLBACK
        }
        currentMerchantPoint = initialPoint

        binding.mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(initialPoint)
                .zoom(15.2)
                .pitch(45.0)
                .build()
        )
    }

    private fun enableLiveLocationPuck() {
        binding.mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
            pulsingColor = Color.parseColor("#38BDF8")
            pulsingMaxRadius = 140f
        }
    }

    fun centerMapOn(point: Point) {
        binding.mapView.mapboxMap.flyTo(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.5)
                .pitch(45.0)
                .build(),
            MapAnimationOptions.mapAnimationOptions { duration(800) }
        )
    }

    private fun spawnStaticRidersOnRoads(center: Point) {
        pointAnnotationManager?.deleteAll()
        riderAnnotations.clear()

        val offsets = listOf(
            Pair(0.0022, 0.0018),   // Close ~300m
            Pair(-0.0024, 0.0028),  // Close ~400m
            Pair(0.0032, -0.0022),  // Close ~450m
            Pair(-0.0020, -0.0028), // Close ~500m
            Pair(0.0085, 0.0075),   // Far ~1.2 km
            Pair(-0.0095, -0.0080), // Far ~1.5 km
            Pair(0.0120, -0.0065)   // Far ~1.8 km
        )

        offsets.forEachIndexed { index, offset ->
            val riderPoint = Point.fromLngLat(center.longitude() + offset.second, center.latitude() + offset.first)
            val riderBitmap = getRiderCustomBadge(index)
            val options = PointAnnotationOptions()
                .withPoint(riderPoint)
                .withIconImage(riderBitmap)
                .withIconSize(0.72)

            val annotation = pointAnnotationManager?.create(options)
            if (annotation != null) {
                riderAnnotations.add(annotation)
            }
        }
    }

    private fun getRiderCustomBadge(index: Int): Bitmap {
        val customName = "rider_${index + 1}"
        val resId = activity.resources.getIdentifier(customName, "drawable", activity.packageName)
        if (resId != 0) {
            val d = ContextCompat.getDrawable(activity, resId)
            if (d != null) {
                val size = 90
                val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                d.setBounds(0, 0, size, size)
                d.draw(canvas)
                return getCircularCroppedBitmap(bmp)
            }
        }
        return generateVectorRiderBadge(index)
    }

    private fun generateVectorRiderBadge(index: Int): Bitmap {
        val size = 84
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val colors = intArrayOf(
            Color.parseColor("#0284C7"),
            Color.parseColor("#059669"),
            Color.parseColor("#D97706"),
            Color.parseColor("#7C3AED"),
            Color.parseColor("#2563EB"),
            Color.parseColor("#DC2626"),
            Color.parseColor("#0891B2")
        )
        val ringColor = colors[index % colors.size]

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
            setShadowLayer(6f, 0f, 3f, Color.parseColor("#99000000"))
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ringColor
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        val r = size / 2f
        canvas.drawCircle(r, r, r - 6f, bgPaint)
        canvas.drawCircle(r, r, r - 6f, borderPaint)

        val text = "R${index + 1}"
        val yPos = (r - (textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(text, r, yPos, textPaint)

        return bmp
    }

    private fun getCircularCroppedBitmap(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = Rect(0, 0, size, size)

        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, borderPaint)
        return output
    }

    fun onDestroy() {
        pointAnnotationManager?.onDestroy()
    }
}