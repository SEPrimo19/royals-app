import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
}

// Read secrets from local.properties so no keys are ever committed to source.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun secret(key: String, fallback: String = ""): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: fallback)

android {
    namespace = "com.grace.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.grace.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "SUPABASE_URL", "\"${secret("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${secret("SUPABASE_ANON_KEY")}\"")
        buildConfigField("String", "BIBLE_API_KEY", "\"${secret("BIBLE_API_KEY")}\"")
        // Google Web OAuth Client ID — used by native One Tap to obtain a
        // verifiable ID token. Same value as configured in Supabase Dashboard
        // → Auth → Providers → Google. Empty string means One Tap is disabled
        // and the button will surface a "not configured" error.
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${secret("GOOGLE_WEB_CLIENT_ID")}\"")
    }

    // Release signing — reads keystore params from local.properties so the
    // path + passwords are never committed. If any param is missing (e.g.
    // a fresh clone without a keystore yet), the signing config is skipped
    // and `./gradlew assembleRelease` falls back to unsigned output. Debug
    // builds always use the platform-provided debug keystore.
    val releaseKeystorePath = secret("RELEASE_KEYSTORE_PATH")
    val releaseKeystorePassword = secret("RELEASE_KEYSTORE_PASSWORD")
    val releaseKeyAlias = secret("RELEASE_KEY_ALIAS")
    val releaseKeyPassword = secret("RELEASE_KEY_PASSWORD")
    val hasReleaseSigning = releaseKeystorePath.isNotBlank()
        && releaseKeystorePassword.isNotBlank()
        && releaseKeyAlias.isNotBlank()
        && releaseKeyPassword.isNotBlank()

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            // No proguard mapping in debug — skip the upload step so the
            // Crashlytics task doesn't warn on every build.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
        }
        release {
            // R8 minification disabled for this project. Two reasons:
            //   1) R8 (both full + legacy mode) was crashing with
            //      ConcurrentModificationException during
            //      :app:minifyReleaseWithR8 on our combination of
            //      kotlinx.serialization + Compose + Hilt + Supabase.
            //   2) Obfuscation provides ZERO security value here — the
            //      source is published on GitHub under MIT. Hiding the
            //      compiled bytecode while the Kotlin source is one
            //      click away is just busywork that makes builds fail.
            //
            // Tradeoffs of leaving this off:
            //   - APK ~60-70 MB instead of ~30 MB (larger download)
            //   - Slightly slower cold-start (negligible at our scale)
            //   - Log.d / Log.v are NOT stripped → consider explicit
            //     audit before shipping if any debug logs contain PII
            //
            // If you ever Play-Store this (where APK size matters more
            // and you can't tolerate the failure), revisit: try a newer
            // R8 version, refine the kotlinx.serialization keep rules,
            // or migrate to a leaner DI / serialization stack.
            isMinifyEnabled = false
            // ProGuard config kept in place for the day we re-enable
            // minification. Inert while isMinifyEnabled is false.
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Crashlytics map upload only matters when classes are
            // obfuscated. Disable so the task doesn't warn.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            // Wire the release signing config if local.properties supplied
            // the keystore params. Otherwise leave unsigned — Gradle will
            // warn on assembleRelease but won't fail.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

ksp {
    // Required because GraceDatabase uses exportSchema = true.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.core.ktx)

    // Lifecycle & ViewModel
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.runtime.ktx)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.android)

    // Networking (Bible API)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)

    // Media
    implementation(libs.lottie.compose)
    implementation(libs.coil.compose)

    // Storage & Background
    implementation(libs.datastore)
    implementation(libs.work.runtime)

    // Security
    implementation(libs.security.crypto)

    // Widget
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // Coroutines
    implementation(libs.coroutines.android)

    // QR generation (Event attendance feature)
    implementation(libs.zxing.core)

    // In-app QR scanner — Google Code Scanner. Play Services hosts the
    // camera UI, so no CAMERA permission needed.
    implementation(libs.play.code.scanner)

    // Native Google Sign-in via Credential Manager (One Tap bottom sheet).
    implementation(libs.supabase.compose.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.googleid)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
