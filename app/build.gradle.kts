plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

apply(from = "$rootDir/gradle/android-common.gradle")

android {
    namespace = "org.skitrace.skitrace"

    defaultConfig {
        applicationId = "org.skitrace.skitrace"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(project(":data"))
    implementation(project(":core"))
    implementation("androidx.compose.ui:ui:1.10.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}