package com.lanread.app.ads

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "AdMobBannerView"

@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.bannerAdUnitId,
    usePreloaded: Boolean = true
) {
    val context = LocalContext.current

    val adView = remember(adUnitId) {
        val preloaded = if (usePreloaded) AdMobManager.takePreloadedBanner(context) else null
        if (preloaded != null) {
            Log.d(TAG, "Reusing preloaded banner AdView")
            preloaded
        } else {
            Log.d(TAG, "Creating new banner AdView for adUnitId=$adUnitId")
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.i(TAG, "Banner ad loaded successfully (unit: $adUnitId)")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Banner ad failed to load: code=${error.code}, message=${error.message}")
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
    }

    DisposableEffect(adView) {
        onDispose {
            try {
                adView.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying AdView: ${e.message}")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { adView },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }
}
