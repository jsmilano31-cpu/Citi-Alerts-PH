plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.citialertsph"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.citialertsph"
        minSdk = 29
        targetSdk = 36
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

    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // HTTP client for API requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON parsing (Gson)
    implementation("com.google.code.gson:gson:2.10.1")

    // Image loading library
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // RecyclerView (for lists)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // CardView (for UI components)
    implementation("androidx.cardview:cardview:1.0.0")

    // SwipeRefreshLayout (for pull-to-refresh)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ViewPager2 (for fragments)
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Fragment navigation
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")

    // Location services (for maps/location features)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Permission handling
    implementation("pub.devrel:easypermissions:3.0.0")

    // Date/Time picker
    implementation("com.wdullaer:materialdatetimepicker:4.2.3")

    // File picker for document uploads
    implementation("com.github.dhaval2404:imagepicker:2.1")

    implementation("de.hdodenhof:circleimageview:3.1.0")
// Or the latest version

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

}