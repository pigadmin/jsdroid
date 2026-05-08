plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.kotlin.ksp)
    id("androidx.room")
}

android {
    namespace = "com.jiuzhuan.jsdroid"
    compileSdk = 35

    defaultConfig {
        buildConfigField("String", "BASE_URL",
        "\"http://192.168.1.6:8888/\"")
        buildConfigField("int", "TYPE", "10")
        buildConfigField("String", "WEB_API_URL", "\"http://110.42.110.223:1024/24k/v1/api\"")
        buildConfigField("Boolean", "IS_REQUIRE_PACKNAME", "false")
        applicationId = "com.jiuzhuan.jsdroidH"

        minSdk = 29
        targetSdk = 35
        versionCode = 260507
        versionName = "w260507"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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
        buildConfig = true
        dataBinding = true
        viewBinding = true
        compose = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation("org.dynalang:dynalink:0.5")

    implementation("org.mozilla:rhino:1.8.0")
    implementation("org.mozilla:rhino-xml:1.8.0")
    implementation("org.mozilla:rhino-tools:1.8.0")

    implementation("com.jakewharton.android.repackaged:dalvik-dx:14.0.0_r21")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.blankj.utilcodex)
    implementation("com.tencent:mmkv:2.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.opencv:opencv:4.11.0")

    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-rxjava2:2.6.1")
    // implementation("androidx.room:room-rxjava3:2.6.1")
    // implementation("androidx.room:room-guava:2.6.1")
    // implementation("androidx.room:room-paging:2.6.1")

    annotationProcessor("androidx.room:room-compiler:2.6.1")
    // testImplementation("androidx.room:room-testing:2.6.1")
    implementation("org.greenrobot:eventbus:3.3.1")
}
