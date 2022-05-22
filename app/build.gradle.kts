val kotlin_version = "1.6.21"
//https://developer.android.google.cn/studio/intro/studio-config?hl=zh-cn
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("surgery")
}
// ./gradlew transformDebugClassesWithAsm
//apply<ExamplePlugin>()

android {
    compileSdkVersion(BuildConfig.compileSdkVersion)

    defaultConfig {
        applicationId = "spark.surgery"
        minSdkVersion(BuildConfig.minSdkVersion)
        targetSdkVersion(BuildConfig.targetSdkVersion)
        versionCode = 1
        versionName = "1.0"

        kapt {
            arguments { arg("AROUTER_MODULE_NAME", project.name) }
        }

    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    namespace = "spark.surgery"
}
//https://github.com/gradle/kotlin-dsl-samples
dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.6.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("androidx.navigation:navigation-fragment-ktx:2.4.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.4.2")
    implementation("com.alibaba:arouter-api:1.5.2")
    kapt("com.alibaba:arouter-compiler:1.5.2")
}

