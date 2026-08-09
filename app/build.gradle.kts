import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Razorpay's publishable Key ID lives in local.properties (gitignored — see that file's header)
// instead of hardcoded here like the Supabase keys above, since this repo has since moved to
// keeping per-developer/per-environment secrets out of version control. Add a line like
// `RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxx` to your local.properties to enable the premium checkout;
// without it, the "Buy Now" button surfaces a clear "not configured" error instead of crashing.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val razorpayKeyId: String = localProperties.getProperty("RAZORPAY_KEY_ID", "")
val posthogApiKey: String = localProperties.getProperty("POSTHOG_API_KEY", "")
// The Google Cloud "Web application" OAuth client ID — same one already configured under
// Supabase Dashboard > Authentication > Providers > Google for the website. Android's Credential
// Manager needs it (not an Android-type client ID) to request a Google ID token; see
// local.properties for where to find/add it. Empty by default so a missing key surfaces a clear
// "not configured" message instead of crashing, same treatment as RAZORPAY_KEY_ID above.
val googleWebClientId: String = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics.plugin)
}

android {
    namespace = "com.quizmaker.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.quizmakeronline.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Quiz Maker's production Supabase project — same backend the website uses.
        buildConfigField("String", "SUPABASE_URL", "\"https://wybjydjifaahelwfzawj.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind5Ymp5ZGppZmFhaGVsd2Z6YXdqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzODk5NDUsImV4cCI6MjA1OTk2NTk0NX0.qAxPEfEU7EQlqlKAQZ1FF1Z926C_j6iUoa_2eqF2-dE\"")
        buildConfigField("String", "SHARE_BASE_URL", "\"https://app.yunolms.com\"")
        buildConfigField("String", "RAZORPAY_KEY_ID", "\"$razorpayKeyId\"")
        buildConfigField("String", "POSTHOG_API_KEY", "\"$posthogApiKey\"")
        buildConfigField("String", "POSTHOG_HOST", "\"https://us.i.posthog.com\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // The Crashlytics plugin auto-uploads the R8 mapping file whenever isMinifyEnabled is
            // true, so crash reports show real method names instead of obfuscated ones — no extra
            // config needed here.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.functions)
    implementation(libs.ktor.client.android)

    implementation(libs.coil.compose)

    implementation(libs.datastore.preferences)

    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)

    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.razorpay.checkout)

    implementation(libs.fastexcel.reader)
    implementation(libs.fastexcel.writer)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)

    implementation(libs.posthog.android)

    // "Sign in with Google" via Credential Manager (not Firebase/Play Services' old GoogleSignIn API).
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)
}
