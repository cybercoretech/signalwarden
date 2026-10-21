plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseStoreFile = System.getenv("SIGNALWARDEN_RELEASE_STORE_FILE")
val releaseStorePassword = System.getenv("SIGNALWARDEN_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("SIGNALWARDEN_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("SIGNALWARDEN_RELEASE_KEY_PASSWORD")
val releaseSigningValues = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
val releaseSigningConfigured = releaseSigningValues.all { !it.isNullOrBlank() }
// Release builds get these from tools/release.sh; plain builds stay "-dev".
val signalwardenVersionCode = providers.gradleProperty("signalwardenVersionCode").orNull?.toIntOrNull() ?: 1
val signalwardenVersionName = providers.gradleProperty("signalwardenVersionName").orNull ?: "0.1.0-dev"
if (releaseSigningValues.any { !it.isNullOrBlank() } && !releaseSigningConfigured) {
    throw GradleException("Release signing requires all SIGNALWARDEN_RELEASE_* environment variables.")
}

android {
    namespace = "dev.cybercore.signalwarden"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "dev.cybercore.signalwarden"
        minSdk = 30
        targetSdk = 36
        versionCode = signalwardenVersionCode
        versionName = signalwardenVersionName
    }
    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                storeType = "PKCS12"
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    lint {
        warningsAsErrors = true
        informational += setOf("OldTargetApi", "AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
}
