plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

apply(from = "$rootDir/gradle/android-common.gradle")

android {
    namespace = "org.skitrace.skitrace"

    defaultConfig {
        applicationId = "org.skitrace.skitrace"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    // Project dependencies
    implementation(project(":data"))
    implementation(project(":core"))

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose Runtime
    implementation("androidx.compose.runtime:runtime")

    // AndroidX Compose UI Refrences
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // AndroidX Kotlin core
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")

    // Android Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.9.6")

    // Maplibre Native Compose
    implementation("org.maplibre.compose:maplibre-compose:0.12.1")

    // Material Design library for Jetpack Compose
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Jetpack Compose Activity Compose
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    // Jetpack Compose Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Junit
    testImplementation("junit:junit:4.13.2")

    // Android test junit
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

}