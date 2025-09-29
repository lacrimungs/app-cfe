plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.proyecto"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.proyecto"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

        implementation(platform("com.google.firebase:firebase-bom:32.3.1"))
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-database-ktx")
        implementation("com.google.firebase:firebase-firestore-ktx")
        implementation("com.google.firebase:firebase-analytics-ktx")
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation("androidx.fragment:fragment:1.3.6")
        implementation("androidx.navigation:navigation-fragment:2.7.1")
        implementation("androidx.navigation:navigation-ui:2.7.1")
        implementation("com.google.android.material:material:1.9.0")
        implementation("com.google.android.gms:play-services-location:21.3.0")
        implementation("com.android.volley:volley:1.2.1")
        implementation("com.squareup.picasso:picasso:2.71828")
        implementation("androidx.cardview:cardview:1.0.0")
        implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
        implementation("androidx.navigation:navigation-ui-ktx:2.3.5")
        implementation ("com.squareup.okhttp3:okhttp:4.9.0")
        implementation ("androidx.appcompat:appcompat:1.4.0")
        implementation ("androidx.work:work-runtime-ktx:2.9.0")

        implementation ("androidx.recyclerview:recyclerview:1.3.2")
        implementation ("androidx.appcompat:appcompat:1.7.0")
        implementation ("com.google.android.material:material:1.12.0")

    implementation(libs.legacy.support.v4)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// Aplica el plugin de Google Services
apply(plugin = "com.google.gms.google-services")

