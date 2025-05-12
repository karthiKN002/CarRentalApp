import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import java.util.Properties
import java.io.FileInputStream
import java.io.FileNotFoundException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
} else {
    throw FileNotFoundException("local.properties file not found. Please create one and add your API keys.")
}

android {
    namespace = "com.example.gearup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gearup"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add the API keys to BuildConfig
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${localProperties["GOOGLE_MAPS_API_KEY"]}\"")
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"${localProperties["STRIPE_PUBLISHABLE_KEY"]}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProperties["GOOGLE_WEB_CLIENT_ID"]}\"")
        buildConfigField("String", "ADMIN_EMAILS", "\"${localProperties["ADMIN_EMAILS"]}\"")
        buildConfigField("String", "GOOGLE_CSE_API_KEY", "\"${localProperties["GOOGLE_CSE_API_KEY"]}\"") // Added
        buildConfigField("String", "GOOGLE_CSE_CX", "\"${localProperties["GOOGLE_CSE_CX"]}\"") // Added

        // Add App Check debug token
        //resValue("string", "app_check_debug_token", localProperties["APP_CHECK_DEBUG_TOKEN"]?.toString() ?: "")
    }
    lint {
        abortOnError = false
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = localProperties["GOOGLE_MAPS_API_KEY"] as String
            manifestPlaceholders["STRIPE_PUBLISHABLE_KEY"] = localProperties["STRIPE_PUBLISHABLE_KEY"] as String
        }
        debug {
            manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = localProperties["GOOGLE_MAPS_API_KEY"] as String
            manifestPlaceholders["STRIPE_PUBLISHABLE_KEY"] = localProperties["STRIPE_PUBLISHABLE_KEY"] as String
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // Enable core library desugaring
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {

    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5") // Latest as of May 2025
    // AndroidX Core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)

    // Firebase Libraries (explicit versions)
    implementation("com.google.firebase:firebase-auth:23.2.0")//23.2.0
    implementation("com.google.firebase:firebase-firestore:25.1.4")//25.1.4
    implementation("com.google.firebase:firebase-storage:21.0.1")//21.0.1
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")//21.0.0



    // Play Services (explicit versions)


    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.gms:play-services-location:20.7.0")
    implementation("com.google.android.gms:play-services-base:18.4.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.2.0")//
    implementation("com.google.android.gms:play-services-wallet:18.1.3")
    implementation("com.google.android.gms:play-services-places:17.0.0")



    // Libraries not managed by BOM
    implementation("com.google.android.libraries.places:places:3.5.0") // Updated from 3.3.0
    implementation("com.google.android.play:integrity:1.4.0") // Updated from 1.3.0
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // UI & Media
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.google.android.material:material:1.12.0") // Updated from 1.11.0
    implementation("androidx.browser:browser:1.8.0") // Updated from 1.7.0    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // Networking
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation("androidx.core:core-ktx:1.16.0")//1.12.0
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")//2.6.2

    // Google APIs
    implementation("com.google.api-client:google-api-client-android:1.25.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.api-client:google-api-client-gson:1.25.0") {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation("com.google.apis:google-api-services-calendar:v3-rev411-1.25.0") {
        exclude(group = "org.apache.httpcomponents")
    }

    // Payment
    implementation("com.stripe:stripe-android:20.37.0") // Updated from 20.35.2

    // Background Tasks
    implementation("androidx.work:work-runtime-ktx:2.9.1") // Updated from 2.9.0
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}