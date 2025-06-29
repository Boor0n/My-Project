// app/build.gradle.kts
plugins {
    id("com.android.application") // Оставляем id, так как это стандарт для application plugin
    alias(libs.plugins.kotlinAndroid) // Используем alias из Version Catalog
}

android {
    namespace = "com.booron.DIG_client"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.booron.DIG_client"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17 // Обновлено до 17
        targetCompatibility = JavaVersion.VERSION_17 // Обновлено до 17
    }
    kotlinOptions { // Этот блок нужен, если у вас Kotlin-код
        jvmTarget = "17"
    }
}

dependencies {
    // Стандартные зависимости
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Ваши новые зависимости
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("androidx.lifecycle:lifecycle-runtime:2.8.3")
    implementation("androidx.lifecycle:lifecycle-compiler:2.8.3")

    implementation("com.google.android.exoplayer:exoplayer:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
}