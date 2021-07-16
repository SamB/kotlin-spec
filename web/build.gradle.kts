import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode

plugins {
    id("org.jetbrains.kotlin.js") // version "1.4.31"
}

group = "org.jetbrains.kotlin.spec"
version = "0.1"

val buildMode = findProperty("mode")?.toString() ?: "production" // production | development

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:0.13.0")

    implementation("io.kvision:jquery-kotlin:1.0.0")

    implementation(npm("jquery", "3.6.0"))
    implementation(npm("katex", "0.13.11"))
    implementation(npm("kotlin-playground", "1.24.2"))
}

kotlin {
    js {
        browser {
            webpackTask {
                mode = if (buildMode == "production") Mode.PRODUCTION
                       else Mode.DEVELOPMENT
                sourceMaps = mode == Mode.DEVELOPMENT
            }
        }

        binaries.executable()
    }
}
