plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // Add the compose plugin from the second build.gradle.kts
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.percobaan6" // Main namespace
    compileSdk = 35 // Higher compileSdk

    defaultConfig {
        applicationId = "com.example.percobaan6" // Main application ID
        minSdk = 30 // Higher minSdk for broader compatibility if needed, or stick to 28 if callfunction specifically needs it
        // Keeping 30 as it's from the "main" file. If Compose needs 28, adjust.
        targetSdk = 35 // Consistent targetSdk
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true // From the first build.gradle.kts
        compose = true     // From the second build.gradle.kts
    }
}

dependencies {

    // Dependencies common to both or only in the first build.gradle.kts
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Specific dependency from the first build.gradle.kts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Dependencies from the second build.gradle.kts (Compose and GMS/Permissions)
    implementation(libs.play.services.location)
    implementation(libs.accompanist.permissions)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.onnxruntime.android)

    // Test dependencies common to both
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Test dependencies specific to Compose from the second build.gradle.kts
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}