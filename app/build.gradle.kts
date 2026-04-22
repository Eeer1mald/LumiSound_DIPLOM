plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android.plugin)
    kotlin("plugin.serialization")
    kotlin("kapt")
}

android {
    namespace = "com.example.lumisound"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lumisound"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Supabase config from local.properties (do not commit secrets)
        var supabaseUrl: String = (project.findProperty("SUPABASE_URL") as? String) ?: ""
        var supabaseAnonKey: String = (project.findProperty("SUPABASE_ANON_KEY") as? String) ?: ""
        if (supabaseUrl.isBlank()) {
            supabaseUrl = "https://dmwnegtvotnrajzpyfad.supabase.co"
        }
        if (supabaseAnonKey.isBlank()) {
            supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRtd25lZ3R2b3RucmFqenB5ZmFkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI1MDc5MzMsImV4cCI6MjA3ODA4MzkzM30.urUPVr7kS1JPeFU7IXHrSciwE_7vUm4B-2EiSKcu3P8"
        }
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("lumisound-release.jks")
            storePassword = "lumisound2024"
            keyAlias = "lumisound"
            keyPassword = "lumisound2024"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    kapt {
        correctErrorTypes = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        // Strong skipping mode — пропускает рекомпозицию нестабильных параметров
    }
    // Compose compiler metrics для отладки (опционально)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Ktor HTTP client for Supabase REST
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ExoPlayer for audio playback
    val media3Version = "1.2.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // uCrop for image cropping
    implementation("com.github.yalantis:ucrop:2.2.8")
    // AppCompat для UCrop темы
    implementation("androidx.appcompat:appcompat:1.6.1")
    
    // Accompanist Pager for carousel navigation
    implementation("com.google.accompanist:accompanist-pager:0.32.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.32.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}