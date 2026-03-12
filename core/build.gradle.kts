plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

apply(from = "$rootDir/android-common.gradle")

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
    debugImplementation(libs.androidx.test.ext.junit.gtest)
    debugImplementation(libs.google.ndk.googletest)
    androidTestImplementation(libs.androidx.test.runner)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
}