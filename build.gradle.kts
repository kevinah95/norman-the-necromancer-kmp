plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    val nodeJs = rootProject.extensions.getByType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>()
    val npm = rootProject.extensions.getByType<org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension>()
    nodeJs.packageManagerExtension.set(npm)
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootPlugin> {
    val nodeJs = rootProject.extensions.getByType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsRootExtension>()
    val npm = rootProject.extensions.getByType<org.jetbrains.kotlin.gradle.targets.wasm.npm.WasmNpmExtension>()
    nodeJs.packageManagerExtension.set(npm)
}
