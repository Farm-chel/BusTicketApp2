plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.20" // Добавляем версию
}

android {
    namespace = "com.example.busticketapp2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.busticketapp2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0 Demo"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/*.kotlin_module"
            )
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")

    // Google Maps
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")


    // ZXing для генерации QR-кодов
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.cardview:cardview:1.0.0")
// Для работы с интернетом
    implementation("androidx.core:core-ktx:1.10.1")

}