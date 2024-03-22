

plugins {
    id("com.android.application")

}

android {
    namespace = "com.example.myapplication"
    compileSdk = 34


    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 34
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
    androidResources {
        noCompress += listOf("tflite")
    }

}

dependencies {


    implementation("com.google.android.gms:play-services-tasks:18.1.0")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //face-detection
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation ("com.google.android.gms:play-services-mlkit-face-detection:17.1.0")
    // image classification
    implementation("com.google.mlkit:image-labeling:17.0.8")
    implementation("com.google.mlkit:image-labeling-custom:17.0.2")
    // tensorflow
    implementation ("org.tensorflow:tensorflow-lite:2.4.0")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.4.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.2.0")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    //noinspection GradleCompatible
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

}
