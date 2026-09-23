plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.smartcounter.ai"
    minSdk = 24
    targetSdk = 36
    versionCode = 11
    versionName = "2.0.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
  }

  signingConfigs {
    val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
    val kFile = file(keystorePath)
    if (kFile.exists() && !System.getenv("STORE_PASSWORD").isNullOrEmpty()) {
      create("release") {
        storeFile = kFile
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("STORE_PASSWORD")
      }
    }
  }

  // Set to true to test the RELEASE build with Google's official AdMob test ad units.
  // Set to false for live production ads from your AdMob account.
  // Can also be toggled via Gradle property: -PuseTestAdsInRelease=false
  val useTestAdsInRelease = providers.gradleProperty("useTestAdsInRelease")
    .map { it.toBoolean() }
    .orElse(providers.environmentVariable("USE_TEST_ADS").map { it.toBoolean() })
    .getOrElse(true)

  val releaseAppId = if (useTestAdsInRelease) "ca-app-pub-3940256099942544~3347511713" else "ca-app-pub-1859648502281028~7809732470"
  val releaseAppOpenId = if (useTestAdsInRelease) "ca-app-pub-3940256099942544/9257395921" else "ca-app-pub-1859648502281028/2338291242"
  val releaseBannerId = if (useTestAdsInRelease) "ca-app-pub-3940256099942544/9214589741" else "ca-app-pub-1859648502281028/8764134185"
  val releaseInterstitialId = if (useTestAdsInRelease) "ca-app-pub-3940256099942544/1033173712" else "ca-app-pub-1859648502281028/8413579150"
  val releaseTestMode = useTestAdsInRelease

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      ndk {
        debugSymbolLevel = "FULL"
      }
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
      manifestPlaceholders["admobAppId"] = releaseAppId
      buildConfigField("Boolean", "ADMOB_TEST_MODE", "$releaseTestMode")
      buildConfigField("String", "ADMOB_APP_ID", "\"$releaseAppId\"")
      buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"$releaseAppOpenId\"")
      buildConfigField("String", "ADMOB_BANNER_ID", "\"$releaseBannerId\"")
      buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$releaseInterstitialId\"")
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
      manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
      buildConfigField("Boolean", "ADMOB_TEST_MODE", "true")
      buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
      buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
      buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/9214589741\"")
      buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
    }
  }
  lint {
    abortOnError = false
    checkReleaseBuilds = false
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.mlkit.barcode.scanning)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.play.services.ads)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
