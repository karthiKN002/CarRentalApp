import java.util.Properties
import java.io.FileNotFoundException

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream ->
        localProperties.load(stream)
    }
} else {
    throw FileNotFoundException("local.properties file not found.")
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

        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${localProperties["GOOGLE_MAPS_API_KEY"]}\"")
        buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"${localProperties["STRIPE_PUBLISHABLE_KEY"]}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProperties["GOOGLE_WEB_CLIENT_ID"]}\"")
        buildConfigField("String", "ADMIN_EMAILS", "\"${localProperties["ADMIN_EMAILS"]}\"")
        buildConfigField("String", "GOOGLE_CSE_API_KEY", "\"${localProperties["GOOGLE_CSE_API_KEY"]}\"")
        buildConfigField("String", "GOOGLE_CSE_CX", "\"${localProperties["GOOGLE_CSE_CX"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    lint {
        abortOnError = false
    }
    packaging {
        resources.excludes.add("META-INF/INDEX.LIST")
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/*.kotlin_module")
        resources.excludes.add("META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0") {
        exclude(group="org.apache.httpcomponents")// Added to prevent httpclient/httpcore conflicts
    }
    implementation("com.google.firebase:firebase-messaging:24.1.1")
    implementation("com.google.firebase:firebase-functions:21.0.0")

    implementation("androidx.media:media:1.6.0")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")

    implementation("com.google.android.libraries.places:places:4.2.0")
    implementation("com.google.firebase:firebase-appcheck-debug:18.0.0")
    //implementation("com.google.firebase:firebase-appcheck-playintegrity:18.0.0")
    // Core Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // AndroidX
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Firebase
    implementation("com.google.firebase:firebase-auth:23.2.0")
    implementation("com.google.firebase:firebase-firestore:25.1.4")
    implementation("com.google.firebase:firebase-storage:21.0.1")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.2.0") {
        exclude("com.google.firebase","firebase-appcheck")
        exclude("com.google.firebase","firebase-appcheck-playintegrity")
    }
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.gms:play-services-base:18.4.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.2.0")
    implementation("com.google.android.gms:play-services-wallet:18.1.3")
    implementation("com.google.android.gms:play-services-places:17.0.0")

    // Google Identity / Credentials
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("androidx.camera:camera-video:1.4.2")

    // Stripe Payments
    implementation("com.stripe:stripe-android:20.43.0")

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

    // Image Loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.squareup.picasso:picasso:2.8")

    // Networking
    implementation("com.stripe:stripe-android:20.43.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Integrity API
    implementation("com.google.android.play:integrity:1.4.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
