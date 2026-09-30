plugins {
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.notebook.notes.checklist"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.notebook.notes.checklist"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures{
        viewBinding = true
        buildConfig = true
        dataBinding = true

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {


    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.firebase.crashlytics)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // dagger hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    // room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // ui
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    // images
    implementation(libs.coil)
    implementation(libs.androidsvg)
    // json
    implementation(libs.gson)
    // background work
    implementation(libs.androidx.work.runtime.ktx)
    // security
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.emoji2)
    implementation(libs.androidx.emoji2.views)
    implementation(libs.androidx.emoji2.views.helper)

    //in App update
    implementation(libs.play.app.update.ktx)

    // RevenueCat — only billing backend (no purchases-ui)
    implementation(libs.revenuecat.purchases)

    // Firebase (Remote Config). google-services.json is optional; managers fall back to defaults.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation("com.intuit.sdp:sdp-android:1.0.3")
    // Ads
    //Shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0") //*
    //Google
    implementation ("com.google.android.gms:play-services-ads:24.9.0")//*

    implementation ("com.facebook.android:audience-network-sdk:6.+")//*
    //mediation
    implementation ("com.google.ads.mediation:facebook:6.19.0.1")//*

    implementation("androidx.work:work-runtime:2.10.2")//*
    implementation("androidx.work:work-runtime-ktx:2.10.2")//*
    implementation("com.google.guava:guava:33.3.1-android")//*
    implementation("com.sun.jersey:jersey-core:1.19.4")
    //lottie
    implementation ("com.airbnb.android:lottie:6.5.2")//*

    implementation ("com.facebook.android:audience-network-sdk:6.+")//*
    //mediation
    implementation ("com.google.ads.mediation:facebook:6.19.0.1")//*

}