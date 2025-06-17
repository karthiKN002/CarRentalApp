plugins {
    id("com.android.application") version "8.8.2" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}

subprojects {
    afterEvaluate {
        configurations.all {
            resolutionStrategy {
                force("androidx.core:core-ktx:1.16.0")
                force("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
                force("androidx.compose.ui:ui:1.7.8")
                // Ensure consistent Play Services versions for App Check compatibility
                force("com.google.android.gms:play-services-basement:18.6.0")
                force("com.google.android.gms:play-services-tasks:18.2.0")
            }
        }
    }
}