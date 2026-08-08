plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

tasks.register("deployDebug") {
    group = "deployment"
    description = "Build the debug APK and install it on a connected Android device."
    dependsOn(":app:installDebug")
}
