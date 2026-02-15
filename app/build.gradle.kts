import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp.room)
}

// Read GEOAPIFY_API_KEY from root/local.properties or fallback to environment variable
val geoapifyApiKey: String? = run {
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) {
        val props = Properties()
        props.load(localProps.inputStream())
        props.getProperty("GEOAPIFY_API_KEY") ?: System.getenv("GEOAPIFY_API_KEY")
    } else {
        System.getenv("GEOAPIFY_API_KEY")
    }
}

android {
    namespace = "com.example.lakbaylaya"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lakbaylaya"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject the key into the Android manifest as a placeholder and into BuildConfig
        manifestPlaceholders["GEOAPIFY_API_KEY"] = geoapifyApiKey ?: ""
        buildConfigField("String", "GEOAPIFY_API_KEY", "\"${geoapifyApiKey ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
        // Enable BuildConfig so buildConfigField in defaultConfig is generated
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // MapLibre for native maps (includes native annotation plugin v9+)
    implementation(libs.maplibre.android.sdk)

    implementation(libs.androidx.compose.foundation)
    // OkHttp for network requests (required by MapLibre and Geoapify)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Kotlin Serialization for JSON handling (voice notes mapping)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Google Play Services for GPS location
    implementation(libs.play.services.location)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Room (local database) - using KSP for annotation processing
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Annotation library (AndroidX) - provides @Nullable, @NonNull, @SuppressLint, etc.
    implementation("androidx.annotation:annotation:1.6.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.compose.material.icons.extended)
}