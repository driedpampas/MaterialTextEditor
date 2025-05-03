import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        load(versionPropsFile.inputStream())
    } else {
        setProperty("VERSION_CODE", "1")
        setProperty("VERSION_NAME", "1.0")
    }
}

val isCI = System.getenv("CI") == "true"

val newVersionCode = versionProps.getProperty("VERSION_CODE").toInt() + if (isCI) 0 else 1
versionProps.setProperty("VERSION_CODE", newVersionCode.toString())
if (!isCI) {
    versionProps.store(versionPropsFile.outputStream(), null)
}

val newVersionName = versionProps.getProperty("VERSION_NAME") ?: "dev"

android {
    namespace = "eu.org.materialtexteditor"
    compileSdk = 35

    defaultConfig {
        applicationId = "eu.org.materialtexteditor"
        minSdk = 31
        targetSdk = 35
        versionCode = newVersionCode
        versionName = newVersionName

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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
//    implementation(libs.androidx.compose.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}