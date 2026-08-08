plugins {
    alias(libs.plugins.android.application) apply false
}

tasks.register("deployDebug") {
    group = "deployment"
    description = "Build the debug APK and install it on a connected Android device."
    dependsOn(":app:installDebug")
}
