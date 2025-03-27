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
    namespace = "com.example.carrentalapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.carrentalapp"
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
    }
}

dependencies {
    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.database)

    // Firebase App Check
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug.v1711) // Debug provider

    // Google Play Services
    implementation(libs.play.services.auth.v2070)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.places)
    implementation(libs.play.services.base.v1830)

    // Google APIs
    implementation("com.google.api-client:google-api-client-android:1.25.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
    }
    implementation("com.google.api-client:google-api-client-gson:1.25.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
    }
    implementation("com.google.apis:google-api-services-calendar:v3-rev411-1.25.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
        exclude(group = "org.apache.httpcomponents", module = "httpcore")
    }

    // Stripe
    implementation(libs.stripe.android)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Image Loading
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    // WorkManager
    implementation(libs.work.runtime)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core) }