import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val appVersionName = providers.gradleProperty("app.versionName").orElse("1.0.0").get()
val appVersionCode = providers.gradleProperty("app.versionCode").orElse("10000").get()

val appDisplayName = "Norman The Necromancer"
val appBundleId = "io.github.kevinah95.norman_the_necromancer"
val appDescription = "A 2D retro pixel-art roguelike action game where defeat is just the beginning."
val appVendor = "Kevin A. Hernandez Rostran"

kotlin {
    jvmToolchain(21)
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

        buildTypes.release.proguard {
            optimize.set(false)
            configurationFiles.from(project.file("proguard-rules.pro"))
        }

        nativeDistributions {
            modules("java.management", "jdk.unsupported")
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = appDisplayName
            packageVersion = appVersionName
            description = appDescription
            vendor = appVendor
            copyright = "Copyright 2026 $appVendor"

            macOS {
                packageName = appDisplayName
                dockName = appDisplayName
                bundleID = appBundleId
                packageVersion = appVersionName
                dmgPackageVersion = appVersionName
                packageBuildVersion = appVersionCode
                appCategory = "public.app-category.games"
            }

            windows {
                packageVersion = appVersionName
                msiPackageVersion = appVersionName
                menuGroup = appDisplayName
                dirChooser = true
                perUserInstall = true
            }

            linux {
                packageName = "norman-the-necromancer"
                menuGroup = appDisplayName
                appCategory = "Game"
            }
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