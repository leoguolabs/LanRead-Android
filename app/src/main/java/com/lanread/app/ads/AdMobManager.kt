package com.lanread.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

object AdMobManager {

    private const val TAG = "AdMobManager"
    private val isInitialized = AtomicBoolean(false)

    // Cached preloaded banner
    private var cachedBannerView: AdView? = null
    private var cachedBannerTime: Long = 0
    private var isBannerLoading = false
    private const val BANNER_CACHE_TTL_MS = 55 * 60 * 1000L

    // Interstitial Ad
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    // Rewarded Interstitial Fallback
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isRewardedInterstitialLoading = false

    fun initialize(context: Context) {
        if (isInitialized.compareAndSet(false, true)) {
            val testDevices = listOf(
                AdRequest.DEVICE_ID_EMULATOR
            )
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(testDevices)
                .build()
            MobileAds.setRequestConfiguration(configuration)

            MobileAds.initialize(context) { status ->
                val adapterMap = status.adapterStatusMap
                Log.i(TAG, "MobileAds initialized: ${adapterMap.keys.joinToString { "$it=${adapterMap[it]?.initializationState}" }}")
            }
            Log.i(TAG, "AdMob SDK initialization started. Using test ads: ${AdMobConfig.useTestAds}")
        }
    }

    // ==========================================
    // Banner Preloading & Management
    // ==========================================

    fun preloadSummaryBannerIfNeeded(context: Context, trigger: String) {
        val now = System.currentTimeMillis()
        if (cachedBannerView != null && (now - cachedBannerTime) < BANNER_CACHE_TTL_MS) {
            Log.d(TAG, "Banner already preloaded and fresh. Trigger: $trigger")
            return
        }
        if (isBannerLoading) return

        isBannerLoading = true
        val adUnitId = AdMobConfig.bannerAdUnitId
        Log.i(TAG, "Start preloading banner (placement=summary, trigger=$trigger, adUnitId=$adUnitId)")

        val banner = AdView(context.applicationContext).apply {
            setAdSize(AdSize.BANNER)
            setAdUnitId(adUnitId)
        }

        banner.adListener = object : AdListener() {
            override fun onAdLoaded() {
                isBannerLoading = false
                cachedBannerView = banner
                cachedBannerTime = System.currentTimeMillis()
                Log.i(TAG, "Banner preloaded successfully: responseId=${banner.responseInfo?.responseId}")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                isBannerLoading = false
                cachedBannerView = null
                Log.e(TAG, "Banner preload failed: code=${error.code}, message=${error.message}, domain=${error.domain}")
            }
        }

        banner.loadAd(AdRequest.Builder().build())
    }

    fun takePreloadedBanner(context: Context): AdView? {
        val now = System.currentTimeMillis()
        val banner = cachedBannerView
        if (banner != null && (now - cachedBannerTime) < BANNER_CACHE_TTL_MS) {
            cachedBannerView = null
            Log.i(TAG, "Consuming preloaded banner ad")
            return banner
        }
        cachedBannerView = null
        return null
    }

    // ==========================================
    // Interstitial Ad Management
    // ==========================================

    fun loadInterstitial(
        context: Context,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        if (interstitialAd != null) {
            Log.d(TAG, "Interstitial ad already ready")
            onLoaded?.invoke()
            return
        }
        if (isInterstitialLoading) {
            Log.d(TAG, "Interstitial ad currently loading")
            return
        }

        isInterstitialLoading = true
        val adUnitId = AdMobConfig.interstitialAdUnitId
        Log.i(TAG, "Loading interstitial ad: adUnitId=$adUnitId")

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.i(TAG, "Interstitial ad loaded: responseId=${ad.responseInfo.responseId}")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.e(TAG, "Interstitial ad failed to load: code=${error.code}, message=${error.message}")
                    onFailed?.invoke(error)
                }
            }
        )
    }

    fun isInterstitialReady(): Boolean = interstitialAd != null

    fun showInterstitial(context: Context, onDismissed: () -> Unit): Boolean {
        val activity = context.findActivity()
        if (activity == null) {
            Log.e(TAG, "Cannot show interstitial: context is not an Activity")
            return false
        }
        return showInterstitial(activity, onDismissed)
    }

    fun showInterstitial(activity: Activity, onDismissed: () -> Unit): Boolean {
        val ad = interstitialAd
        if (ad == null) {
            Log.w(TAG, "Interstitial ad not ready to show")
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.i(TAG, "Interstitial ad dismissed")
                interstitialAd = null
                onDismissed()
                // Preload the next interstitial
                loadInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: code=${error.code}, message=${error.message}")
                interstitialAd = null
                onDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.i(TAG, "Interstitial ad showed full screen content")
            }
        }

        ad.show(activity)
        return true
    }

    // ==========================================
    // Rewarded Interstitial Fallback
    // ==========================================

    fun loadRewardedInterstitial(
        context: Context,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        if (rewardedInterstitialAd != null || isRewardedInterstitialLoading) return
        isRewardedInterstitialLoading = true
        val adUnitId = AdMobConfig.rewardedInterstitialAdUnitId
        Log.i(TAG, "Loading rewarded interstitial fallback: adUnitId=$adUnitId")

        RewardedInterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    isRewardedInterstitialLoading = false
                    Log.i(TAG, "Rewarded interstitial loaded: responseId=${ad.responseInfo.responseId}")
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedInterstitialAd = null
                    isRewardedInterstitialLoading = false
                    Log.e(TAG, "Rewarded interstitial failed to load: code=${error.code}, message=${error.message}")
                    onFailed?.invoke(error)
                }
            }
        )
    }

    fun isRewardedInterstitialReady(): Boolean = rewardedInterstitialAd != null

    fun showRewardedInterstitial(context: Context, onDismissed: () -> Unit): Boolean {
        val activity = context.findActivity()
        if (activity == null) {
            Log.e(TAG, "Cannot show rewarded interstitial: context is not an Activity")
            return false
        }
        return showRewardedInterstitial(activity, onDismissed)
    }

    fun showRewardedInterstitial(activity: Activity, onDismissed: () -> Unit): Boolean {
        val ad = rewardedInterstitialAd ?: return false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.i(TAG, "Rewarded interstitial dismissed")
                rewardedInterstitialAd = null
                onDismissed()
                loadRewardedInterstitial(activity)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded interstitial failed to show: code=${error.code}")
                rewardedInterstitialAd = null
                onDismissed()
            }
        }
        ad.show(activity) { rewardItem ->
            Log.i(TAG, "User earned reward: type=${rewardItem.type}, amount=${rewardItem.amount}")
        }
        return true
    }
}
