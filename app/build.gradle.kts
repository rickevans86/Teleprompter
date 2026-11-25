plugins {
    alias(libs.plugins.android)
    alias(libs.plugins.compose)
    alias(libs.plugins.serialization)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}
android {
    namespace = "dev.telepromptor"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.telepromptor"
        minSdk = 26
        targetSdk = 37
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
