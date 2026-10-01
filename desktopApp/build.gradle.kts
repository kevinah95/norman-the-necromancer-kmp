import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "io.github.kevinah95.norman_the_necromancer.MainKt"

        val korGeJvmArgs = listOf(
            "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
            "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
            "--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED",
            "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED",
            "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED",
            "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"
        )

        jvmArgs.addAll(korGeJvmArgs)

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.kevinah95.norman_the_necromancer"
            packageVersion = "1.0.0"
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs(
        "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        "--add-opens=java.desktop/com.apple.eawt=ALL-UNNAMED",
        "--add-opens=java.desktop/com.apple.eawt.event=ALL-UNNAMED",
        "--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED",
        "--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED"
    )
}