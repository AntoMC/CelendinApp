plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.amc.celendinapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.amc.celendinapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += "es"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                // Usamos el archivo estándar en lugar del 'optimize' para asegurar compatibilidad
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            // Añade esta línea si usas Kotlin (ayuda con la reflexión)
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }


    // Opcional: Esto ayuda a que el APK sea aún más pequeño filtrando procesadores
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    splits {
        abi {
            isEnable = true // Activa la división por arquitectura
            reset() // Limpia las configuraciones por defecto
            // Añade solo las que necesites (arm64 es la más común hoy)
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false // No crees un APK gordo con todo
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // El jefe que controla pesos

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation.layout)

    // ICONOS LIGEROS
    implementation(libs.iconos.core)

    // RED Y DATOS
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation(libs.browser)

}