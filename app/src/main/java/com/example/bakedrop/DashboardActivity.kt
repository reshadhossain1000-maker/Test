package com.example.bakedrop

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.bakedrop.chat.DeepSeekChatEngine
import com.example.bakedrop.chat.SupportActionHandler
import com.example.bakedrop.databinding.ActivityDashboardBinding
import com.example.bakedrop.services.bakeshop.BakeShopHandler
import com.example.bakedrop.services.cakerider.CakeRiderHandler
import com.example.bakedrop.services.helpinghand.HelpingHandHandler
import com.example.bakedrop.services.midnight.MidnightHandler
import com.example.bakedrop.services.parceldrop.ParcelDropHandler
import com.example.bakedrop.ui.banner.PromoBannerManager
import com.example.bakedrop.ui.radar.RadarMapController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.mapbox.geojson.Point
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var radarMapController: RadarMapController
    private lateinit var promoBannerManager: PromoBannerManager
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isTransitioning = false

    companion object {
        private val DHAKA_FALLBACK = Point.fromLngLat(90.4125, 23.8103)
        private const val PREFS_NAME = "BakeDropPrefs"
        private const val KEY_LAST_LAT = "last_lat"
        private const val KEY_LAST_LNG = "last_lng"
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            checkGpsSettingsAndFetchLocation()
        }
    }

    private val gpsResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchFreshGpsLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        radarMapController = RadarMapController(this, binding)
        promoBannerManager = PromoBannerManager(binding, lifecycleScope)

        radarMapController.initializeMap {
            checkLocationPermissionAndStart()
        }

        loadMerchantData()
        setupServiceButtonListeners()
        setupBottomNavTabs()
        setupLiveChatEngine()
        setupNetworkMonitoring()
        setupSupportAvatarImage()
    }

    private fun setupSupportAvatarImage() {
        val resId = resources.getIdentifier("img_support_agent", "drawable", packageName)
        if (resId != 0) {
            binding.imgSupportAvatar.setImageResource(resId)
        }
    }

    private fun checkLocationPermissionAndStart() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            checkGpsSettingsAndFetchLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun checkGpsSettingsAndFetchLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
            .addOnSuccessListener { fetchFreshGpsLocation() }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        val request = IntentSenderRequest.Builder(exception.resolution).build()
                        gpsResolutionLauncher.launch(request)
                    } catch (e: Exception) {
                        fetchFreshGpsLocation()
                    }
                } else {
                    fetchFreshGpsLocation()
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun fetchFreshGpsLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdates(1)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    val point = Point.fromLngLat(loc.longitude, loc.latitude)
                    radarMapController.onLocationAcquired(point, loc.latitude, loc.longitude)
                }
            },
            mainLooper
        )
    }

    private fun setupServiceButtonListeners() {
        binding.btnServiceCakeRider.setOnClickListener { CakeRiderHandler.openService(this) }
        binding.btnServiceParcelDrop.setOnClickListener { ParcelDropHandler.openService(this) }
        binding.btnServiceMidnight.setOnClickListener { MidnightHandler.openService(this) }
        binding.btnServiceBakeShop.setOnClickListener { BakeShopHandler.openService(this) }
        binding.btnServiceHelpingHand.setOnClickListener { HelpingHandHandler.openService(this) }

        binding.btnOpenDrawer.setOnClickListener { binding.drawerLayout.openDrawer(Gravity.RIGHT) }
        binding.btnDrawerLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupBottomNavTabs() {
        binding.tabHome.setOnClickListener { switchTabWith1SecondPulse(0) }
        binding.tabOrders.setOnClickListener { switchTabWith1SecondPulse(1) }
        binding.tabChat.setOnClickListener { switchTabWith1SecondPulse(2) }
        binding.tabInbox.setOnClickListener { switchTabWith1SecondPulse(3) }

        binding.btnBackFromChat.setOnClickListener {
            switchTabWith1SecondPulse(0)
        }
    }

    // 🌟 Claude-এর স্টাইলে ১ সেকেন্ডের আল্ট্রা-স্মুথ স্প্রিং পালস ট্রানজিশন
    private fun switchTabWith1SecondPulse(index: Int) {
        if (isTransitioning) return
        isTransitioning = true

        val activeColor = Color.parseColor("#38BDF8")
        val inactiveColor = Color.parseColor("#94A3B8")

        // Tab Color Updates
        binding.iconHome.setColorFilter(if (index == 0) activeColor else inactiveColor)
        binding.tvHome.setTextColor(if (index == 0) activeColor else inactiveColor)

        binding.iconOrders.setColorFilter(if (index == 1) activeColor else inactiveColor)
        binding.tvOrders.setTextColor(if (index == 1) activeColor else inactiveColor)

        binding.iconChat.setColorFilter(if (index == 2) activeColor else inactiveColor)
        binding.tvChat.setTextColor(if (index == 2) activeColor else inactiveColor)

        binding.iconInbox.setColorFilter(if (index == 3) activeColor else inactiveColor)
        binding.tvInbox.setTextColor(if (index == 3) activeColor else inactiveColor)

        // Pulse Card Animation (Scale & Fade-In)
        val overlay = binding.tabTransitionOverlay
        val pulseCard = binding.pulseCard

        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        pulseCard.scaleX = 0.5f
        pulseCard.scaleY = 0.5f
        pulseCard.alpha = 0f

        val overlayFadeIn = ObjectAnimator.ofFloat(overlay, View.ALPHA, 0f, 1f).setDuration(150)
        val cardScaleX = ObjectAnimator.ofFloat(pulseCard, View.SCALE_X, 0.5f, 1f)
        val cardScaleY = ObjectAnimator.ofFloat(pulseCard, View.SCALE_Y, 0.5f, 1f)
        val cardFadeIn = ObjectAnimator.ofFloat(pulseCard, View.ALPHA, 0f, 1f)

        AnimatorSet().apply {
            playTogether(overlayFadeIn, cardScaleX, cardScaleY, cardFadeIn)
            duration = 250
            interpolator = OvershootInterpolator(1.2f)
            start()
        }

        // ৭০০ মিলি-সেকেন্ড পর স্ক্রিন সুইচ ও ফেড-আউট (মোট ১০০০ মিলি-সেকেন্ড ট্রানজিশন)
        lifecycleScope.launch {
            delay(700)

            binding.frostedHeader.visibility = if (index == 2) View.GONE else View.VISIBLE
            binding.viewHome.visibility = if (index == 0) View.VISIBLE else View.GONE
            binding.viewOrders.visibility = if (index == 1) View.VISIBLE else View.GONE
            binding.viewChat.visibility = if (index == 2) View.VISIBLE else View.GONE
            binding.viewInbox.visibility = if (index == 3) View.VISIBLE else View.GONE

            val overlayFadeOut = ObjectAnimator.ofFloat(overlay, View.ALPHA, 1f, 0f).setDuration(300)
            overlayFadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    overlay.visibility = View.GONE
                    isTransitioning = false
                }
            })
            overlayFadeOut.start()
        }
    }

    // 🌟 Live Chat Engine With DeepSeek & Typing Wave
    private fun setupLiveChatEngine() {
        val user = auth.currentUser ?: return
        val chatRef = db.collection("users").document(user.uid).collection("live_support_chats")

        chatRef.orderBy("timestamp", Query.Direction.ASCENDING).addSnapshotListener { snapshots, _ ->
            if (snapshots != null && !snapshots.isEmpty) {
                binding.layoutMessages.removeAllViews()
                for (doc in snapshots.documents) {
                    val sender = doc.getString("sender") ?: "user"
                    val text = doc.getString("message") ?: ""
                    if (sender == "user") {
                        renderUserBubble(text)
                    } else {
                        renderAiBubble(text)
                    }
                }
            }
        }

        binding.btnChatAttachment.setOnClickListener {
            SupportActionHandler.showSupportActionSheet(this)
        }

        binding.btnSendMessage.setOnClickListener {
            val msg = binding.etChatMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                binding.etChatMessage.text.clear()

                val userMsgData = hashMapOf(
                    "sender" to "user",
                    "message" to msg,
                    "timestamp" to System.currentTimeMillis()
                )
                chatRef.add(userMsgData)

                // 🌟 লাইভ ৩-ডট সাইন-ওয়েভ জাম্পিং অ্যানিমেশন
                lifecycleScope.launch {
                    delay(800)
                    binding.layoutTypingIndicator.visibility = View.VISIBLE
                    binding.typingWaveView.startAnimating()
                    binding.scrollChat.post { binding.scrollChat.fullScroll(View.FOCUS_DOWN) }

                    // DeepSeek এআই থেকে উত্তর আনা (Claude's DeepSeek Engine)
                    val reply = DeepSeekChatEngine.fetchReply(msg)

                    delay(2500) // ২.৫ সেকেন্ড ডটগুলো তরঙ্গের মতো লাফাবে

                    binding.typingWaveView.stopAnimating()
                    binding.layoutTypingIndicator.visibility = View.GONE

                    val aiMsgData = hashMapOf(
                        "sender" to "ai",
                        "message" to reply,
                        "timestamp" to System.currentTimeMillis()
                    )
                    chatRef.add(aiMsgData)
                }
            }
        }
    }

    private fun renderUserBubble(messageText: String) {
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@DashboardActivity, R.drawable.bg_chat_bubble_user)
            setPadding(36, 26, 36, 26)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
                topMargin = 14
                marginStart = 110
            }
            layoutParams = params
        }
        val tv = TextView(this).apply {
            setText(messageText)
            setTextColor(Color.WHITE)
            textSize = 14f
            setLineSpacing(4f, 1f)
        }
        bubble.addView(tv)
        binding.layoutMessages.addView(bubble)
        binding.scrollChat.post { binding.scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun renderAiBubble(messageText: String) {
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@DashboardActivity, R.drawable.bg_chat_bubble_ai)
            setPadding(36, 26, 36, 26)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                topMargin = 14
                marginEnd = 110
            }
            layoutParams = params
        }
        val tv = TextView(this).apply {
            setText(messageText)
            setTextColor(Color.parseColor("#0B3575"))
            textSize = 14f
            setLineSpacing(4f, 1f)
        }
        bubble.addView(tv)
        binding.layoutMessages.addView(bubble)
        binding.scrollChat.post { binding.scrollChat.fullScroll(View.FOCUS_DOWN) }
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    binding.layoutOfflineOverlay.animate().alpha(0f).setDuration(400).withEndAction {
                        binding.layoutOfflineOverlay.visibility = View.GONE
                    }.start()
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    binding.layoutOfflineOverlay.alpha = 0f
                    binding.layoutOfflineOverlay.visibility = View.VISIBLE
                    binding.layoutOfflineOverlay.animate().alpha(1f).setDuration(400).start()
                }
            }
        }
        connectivityManager?.registerNetworkCallback(request, networkCallback!!)
    }

    private fun loadMerchantData() {
        val user = auth.currentUser ?: return
        val name = user.displayName ?: "Merchant"
        binding.tvMerchantGreeting.text = "Hi, $name"
        binding.tvDrawerName.text = name
        binding.tvDrawerEmail.text = user.email ?: ""
        val initial = if (name.isNotEmpty()) name.first().uppercase() else "M"
        binding.tvAvatarInitial.text = initial
        binding.tvDrawerAvatar.text = initial

        db.collection("users").document(user.uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val fullName = snapshot.getString("name") ?: name
                binding.tvMerchantGreeting.text = "Hi, $fullName"
                binding.tvDrawerName.text = fullName
            }
        }

        db.collection("schedules")
            .whereEqualTo("merchantUid", user.uid)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    binding.tvTotalRequests.text = snapshots.size().toString()
                    val completed = snapshots.documents.count { it.getString("status") == "completed" }
                    binding.tvTotalCompleted.text = completed.toString()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        promoBannerManager.startBannerCycle()
    }

    override fun onPause() {
        promoBannerManager.stopBannerCycle()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        radarMapController.onDestroy()
        binding.typingWaveView.stopAnimating()
        try {
            networkCallback?.let { callback ->
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        } catch (e: Exception) {
            // Safe cleanup
        }
    }
}