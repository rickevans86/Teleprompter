plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt)
}
android {
    namespace = "dev.telepromptor"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.telepromptor"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }
kapt { correctErrorTypes = true }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.navigation)
    implementation(libs.lifecycle)
    implementation(libs.json)
    implementation(libs.coroutines)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    implementation(libs.hilt.android)
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)
    kapt(libs.hilt.compiler)
}
