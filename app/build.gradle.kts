plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // id("com.google.gms.google-services") // Firebase 연동을 위해 google-services.json 필요 시 활성화
}

android {
    namespace = "com.example.smartpot"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartpot"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        mlModelBinding = true // TFLite 연동 편의성
    }
}

dependencies {
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.android.identity:identity-credential-android:20231002")
    implementation("androidx.databinding:viewbinding:8.2.0")
    
    // Firebase (IoT 데이터 실시간 수집)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // ML Kit (AI 감정 인식 및 이미지 라벨링)
    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("com.google.mlkit:image-labeling:17.0.7")

    // TensorFlow Lite (식물 상태 진단)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    // CameraX
    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.kakao.sdk:v2-user:2.15.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.navercorp.nid:oauth:5.3.0")
}