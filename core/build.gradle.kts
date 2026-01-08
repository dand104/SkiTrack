plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

apply(from = "$rootDir/gradle/android-common.gradle")

android {
    namespace = "org.skitrace.skitrace.core"
    buildFeatures {
        prefab = true
    }
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags("")
                abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }
    // Test builds only
    packaging {
        jniLibs {
            testOnly += "**/libskitrace_tests.so"
        }
    }
    buildTypes {
        getByName("debug") {
            externalNativeBuild {
                cmake {
                    arguments += "-DBUILD_TESTING=ON"
                }
            }
        }
    }
}

dependencies {
    // Test deps
    debugImplementation("androidx.test.ext:junit-gtest:1.0.0-alpha01")
    debugImplementation("com.android.ndk.thirdparty:googletest:1.11.0-beta-1")
    androidTestImplementation("androidx.test:runner:1.7.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
}