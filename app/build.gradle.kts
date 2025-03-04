plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.translateanywhere"
    compileSdk = 35

    
    defaultConfig {
        applicationId = "com.example.translateanywhere"
        minSdk = 24
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation (libs.play.services.mlkit.language.id)
    implementation (libs.translate)
    implementation (libs.firebase.database)
    implementation (libs.porcupine.android)
    implementation(libs.okhttp)
    implementation(libs.generativeai)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation (libs.generativeai.v070)
    implementation (libs.guava)
    implementation (libs.reactive.streams)
    implementation (libs.firebase.auth)
    implementation (libs.play.services.auth)


}