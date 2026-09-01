import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
    ndkVersion = "25.1.8937393"
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
        }
    }
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.mengine.axwz"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    ndk {
        abiFilters.add("arm64-v8a")
    }
  }

  // Both keystores are gitignored, so on CI and on any fresh clone neither file
  // exists. Referencing one unconditionally made even `assembleDebug` fail at
  // :app:validateSigningDebug — the build never reached packaging. Each config is
  // now registered only when its keystore is actually present; otherwise the
  // build type falls back to AGP's auto-generated debug key (debug) or produces
  // an unsigned APK (release), which is far more useful than no APK at all.
  //
  // Credentials come from the environment, with the historical literals as a
  // fallback so an existing local keystore keeps working unchanged.
  val releaseKeystore = file("${rootDir}/release.keystore")
  val debugKeystore = file("${rootDir}/debug.keystore")

  signingConfigs {
    if (releaseKeystore.exists()) {
      create("release") {
        storeFile = releaseKeystore
        storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: "mengine123"
        keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: "release"
        keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: "mengine123"
      }
    }
    if (debugKeystore.exists()) {
      create("debugConfig") {
        storeFile = debugKeystore
        storePassword = System.getenv("DEBUG_STORE_PASSWORD") ?: "android"
        keyAlias = System.getenv("DEBUG_KEY_ALIAS") ?: "androiddebugkey"
        keyPassword = System.getenv("DEBUG_KEY_PASSWORD") ?: "android"
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release")
    }
    debug {
      // findByName returns null when the keystore is absent; AGP then uses its
      // own debug key, which is exactly what a debug build wants.
      signingConfigs.findByName("debugConfig")?.let { signingConfig = it }
    }
  }
  
  sourceSets {
    getByName("debug") {
      java.srcDirs("build/generated/ksp/debug/kotlin")
    }
    getByName("release") {
      java.srcDirs("build/generated/ksp/release/kotlin")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  lint {
    abortOnError = false
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
  implementation("io.github.java-diff-utils:java-diff-utils:4.12")
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation("net.zetetic:android-database-sqlcipher:4.5.4")
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.playintegrity)
  implementation(libs.firebase.functions)
  implementation("com.google.firebase:firebase-appdistribution:16.0.0-beta14")
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation(libs.onnxruntime.android)
  implementation("io.noties.markwon:core:4.6.2")
  implementation("io.noties.markwon:ext-strikethrough:4.6.2")
  implementation("io.noties.markwon:ext-tables:4.6.2")
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  testImplementation("androidx.work:work-testing:2.9.0")
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  ksp(libs.androidx.room.compiler)
  ksp(libs.moshi.kotlin.codegen)
  implementation("ch.acra:acra-core:5.11.3")
  implementation("ch.acra:acra-mail:5.11.3")
  implementation("ch.acra:acra-toast:5.11.3")
}
