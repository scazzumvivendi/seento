import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// MDS is a separately obtained SDK and must never be committed to this repository.
// Gradle's gradle.properties format cannot include a dotenv file, so load the
// optional project-local .env here. An explicit -PmdslibAar still takes priority.
val envFile = rootProject.file(".env")
val envValues = Properties().apply {
    if (envFile.isFile) envFile.inputStream().use(::load)
}
val mdslibAarPath = providers.gradleProperty("mdslibAar")
    .orElse(envValues.getProperty("MDSLIB_AAR") ?: "")
val mdslibAar = mdslibAarPath.map { rootProject.file(it) }

// Keep upload-key credentials out of source control. See README.md for the
// local signing.properties format used to produce Play-uploadable bundles.
val signingFile = rootProject.file("signing.properties")
val signingValues = Properties().apply {
    if (signingFile.isFile) signingFile.inputStream().use(::load)
}

check(mdslibAarPath.get().isNotBlank()) {
    "MDS SDK missing. Set MDSLIB_AAR in the root .env or pass " +
        "-PmdslibAar=/absolute/path/to/mdslib-3.33.7-release.aar"
}

check(mdslibAar.get().isFile) {
    "MDS SDK not found at ${mdslibAar.get()}"
}

android {
    namespace = "com.scazzumvivendi.seento"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.scazzumvivendi.seento"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (signingFile.isFile) {
                signingConfig = signingConfigs.create("releaseUpload").apply {
                    storeFile = rootProject.file(signingValues.getProperty("storeFile"))
                    storePassword = signingValues.getProperty("storePassword")
                    keyAlias = signingValues.getProperty("keyAlias")
                    keyPassword = signingValues.getProperty("keyPassword")
                }
            }
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.common)
    testImplementation(libs.junit)
    testImplementation(libs.coroutinesTestLib)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.reorderable)
    implementation(files(mdslibAar.get()))
    implementation("com.polidea.rxandroidble2:rxandroidble:1.10.2")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.8")
}
