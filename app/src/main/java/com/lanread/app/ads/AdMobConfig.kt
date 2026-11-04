package com.lanread.app.ads

import com.lanread.app.BuildConfig

object AdMobConfig {
    // Production IDs from Google AdMob account (pub-5587239366359667)
    const val PROD_APP_ID = "ca-app-pub-5587239366359667~1138964370"
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-5587239366359667/1726781020"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5587239366359667/8961614073"
    const val PROD_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5587239366359667/8905773750"

    // Official Google Sample / Test IDs for development and testing
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"

    /**
     * Whether to use Google's official test ad units.
     * In DEBUG builds, default to true so that ads always fill immediately
     * without policy violations on test devices. Can be toggled at runtime.
     */
    var useTestAds: Boolean = BuildConfig.DEBUG

    val bannerAdUnitId: String
        get() = if (useTestAds) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    val interstitialAdUnitId: String
        get() = if (useTestAds) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    val rewardedInterstitialAdUnitId: String
        get() = if (useTestAds) TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID else PROD_REWARDED_INTERSTITIAL_AD_UNIT_ID
}
