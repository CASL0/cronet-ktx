plugins {
    alias(libs.plugins.org.jetbrains.kotlin.android)
    alias(libs.plugins.android.library)
    id("org.jetbrains.dokka")
    id("maven-publish")
}

android {
    namespace = "io.github.casl0.cronet"
    compileSdk = 33

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.org.jetbrains.kotlinx.coroutines.android)
    implementation(libs.org.chromium.net.cronet.api)
    implementation(libs.play.services.cronet)

    testImplementation(libs.junit)
    testImplementation(libs.org.hamcrest.all)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                this.groupId = "com.github.CASL0"
                this.artifactId = "cronet-ktx"
                this.version = "1.0.1"
                from(components["release"])
            }
        }
    }
}
