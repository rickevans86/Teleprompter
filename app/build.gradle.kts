plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}
// Release builds get these from CI (-PversionCode, -PversionName); never edit them by hand.
val appVersionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
val appVersionName = (project.findProperty("versionName") as String?) ?: "0.0.1-dev"

android {
    namespace = "dev.telepromptor"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.telepromptor"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        // Release signing comes from environment variables set by the Release workflow.
        // Without them no release config exists, so local, debug and pull request builds need no secrets.
        val releaseKeystore = System.getenv("RELEASE_KEYSTORE_PATH")
        if (!releaseKeystore.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            // Local release builds fall back to the debug key. CI verifies the real key was used.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
kapt { correctErrorTypes = true }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.lifecycle.runtime)
    implementation(libs.hilt.viewmodel.compose)
    implementation(libs.json)
    implementation(libs.coroutines)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.testing)
    androidTestImplementation(libs.android.junit)
    debugImplementation(libs.compose.manifest)
    testImplementation(libs.coroutines.test)
    implementation(libs.hilt.android)
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)
    kapt(libs.hilt.compiler)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt.yml"))
}
