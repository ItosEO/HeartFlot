plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val gitCommitCount: Int = listOf("git", "rev-list", "--count", "HEAD").execute(project.rootDir).trim().toInt()

fun List<String>.execute(workingDir: File): String {
    return try {
        ProcessBuilder(this)
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
            .inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        logger.warn("Failed to execute git command: ${e.message}")
        "unknown" // fallback value
    }
}
android {
    namespace = "com.itos.heartflot"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.itos.heartflot"
        minSdk = 28
        targetSdk = 36
        versionCode = gitCommitCount
        versionName = "1.0"
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
        compose = true
        buildConfig = true
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

    implementation("com.github.Kyant0:Capsule:main-SNAPSHOT")
}