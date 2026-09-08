package com.example.bakedrop.ui.banner

import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.bakedrop.databinding.ActivityDashboardBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PromoBannerManager(
    private val binding: ActivityDashboardBinding,
    private val scope: LifecycleCoroutineScope
) {
    private var bannerJob: Job? = null

    fun startBannerCycle() {
        bannerJob?.cancel()
        bannerJob = scope.launch {
            delay(7000)
            while (isActive) {
                showPromoBanner()
                delay(12000) // প্রতি ১২ সেকেন্ড পর পর বিজ্ঞাপন ভেসে উঠবে
            }
        }
    }

    fun stopBannerCycle() {
        bannerJob?.cancel()
    }

    private suspend fun showPromoBanner() {
        val banner = binding.bannerCardOverlay
        banner.visibility = View.VISIBLE
        banner.alpha = 0f
        banner.scaleX = 0.95f
        banner.scaleY = 0.95f
        banner.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(450).start()

        delay(3500)

        banner.animate()
            .alpha(0f)
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(450)
            .withEndAction { banner.visibility = View.GONE }
            .start()
    }
}