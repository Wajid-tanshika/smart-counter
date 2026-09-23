plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.example"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.smartcounter.ai"
        minSdk = 24
        targetSdk = 36
        versionCode = 11
        versionName = "2.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // REAL PRODUCTION ADMOB APP ID
        manifestPlaceholders["admobAppId"] =
            "ca-app-pub-1859648502281028~7809732470"
    }

    signingConfigs {
        val keystorePath =
            System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"

        val kFile = file(keystorePath)

        if (kFile.exists() && !System.getenv("STORE_PASSWORD").isNullOrEmpty()) {
            create("release") {
                storeFile = kFile
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
                keyPassword =
                    System.getenv("KEY_PASSWORD")
                        ?: System.getenv("STORE_PASSWORD")
            }
        }
    }

    buildTypes {

        release {
            isCrunchPngs = false
            isMinifyEnabled = true
            isShrinkResources = true

            ndk {
                debugSymbolLevel = "FULL"
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig =
                signingConfigs.findByName("release")
                    ?: signingConfigs.getByName("debug")

            // REAL PRODUCTION ADMOB APP ID
            manifestPlaceholders["admobAppId"] =
                "ca-app-pub-1859648502281028~7809732470"

            // REAL PRODUCTION ADMOB CONFIGURATION
            buildConfigField(
                "Boolean",
                "ADMOB_TEST_MODE",
                "false"
            )

            buildConfigField(
                "String",
                "ADMOB_APP_ID",
                "\"ca-app-pub-1859648502281028~7809732470\""
            )

            buildConfigField(
                "String",
                "ADMOB_APP_OPEN_ID",
                "\"ca-app-pub-1859648502281028/3997929505\""
            )

            buildConfigField(
                "String",
                "ADMOB_BANNER_ID",
                "\"ca-app-pub-1859648502281028/6432521251\""
            )

            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_ID",
                "\"ca-app-pub-1859648502281028/2435285123\""
            )
        }

        debug {
            signingConfig = signingConfigs.getByName("debug")

            // REAL PRODUCTION ADMOB APP ID
            manifestPlaceholders["admobAppId"] =
                "ca-app-pub-1859648502281028~7809732470"

            // REAL PRODUCTION ADMOB CONFIGURATION
            buildConfigField(
                "Boolean",
                "ADMOB_TEST_MODE",
                "false"
            )

            buildConfigField(
                "String",
                "ADMOB_APP_ID",
                "\"ca-app-pub-1859648502281028~7809732470\""
            )

            buildConfigField(
                "String",
                "ADMOB_APP_OPEN_ID",
                "\"ca-app-pub-1859648502281028/3997929505\""
            )

            buildConfigField(
                "String",
                "ADMOB_BANNER_ID",
                "\"ca-app-pub-1859648502281028/6432521251\""
            )

            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_ID",
                "\"ca-app-pub-1859648502281028/2435285123\""
            )
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

// Configure the Secrets Gradle Plugin
secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

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

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    implementation(libs.coil.compose)
    implementation(libs.converter.moshi)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp)

    // Google AdMob
    implementation(libs.play.services.ads)

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
