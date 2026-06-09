plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Mevcut eklentiler duracak, sadece altına bunu ekle:
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" // Kotlin versiyonunla uyumlu olmalı
}

android {
    namespace = "com.hasancankula.evtelemetry"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.hasancankula.evtelemetry"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val mapsApiKey: String = project.findProperty("MAPS_API_KEY") as String? ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Ktor Client Bağımlılıkları
    val ktorVersion = "2.3.11"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion") // WebSocket destekleyen güçlü CIO motoru
    implementation("io.ktor:ktor-client-websockets:$ktorVersion") // Canlı tünel bağlantısı için
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion") // JSON dönüşümleri için
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion") // Otomatik serileştirme

    // ViewModel'den Compose'a Flow akıtmak için yaşam döngüsü araçları
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.1")

    // Google Maps Jetpack Compose Entegrasyonu
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Jetpack Compose Navigation Entegrasyonu
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Material Extended İkonları (Geri butonu vb. için)
    implementation("androidx.compose.material:material-icons-extended")
}