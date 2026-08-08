plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.gytxtx.openjbd"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gytxtx.openjbd"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.fragment)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)

    testImplementation(libs.junit)
}
