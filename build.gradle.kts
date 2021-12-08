buildscript {
    repositories {
        maven(url = "./gradle-plugin")
    }
}
plugins {
    kotlin("js") version "1.5.31"
    `maven-publish`
    id("CssGradlePlugin") version "0.1"

    id("com.gradle.plugin-publish") version "0.11.0" apply false
}
version = "0.1"
group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    maven(url = "./gradle-plugin")
    mavenCentral()
}

subprojects {
    repositories {
        maven(url = "./gradle-plugin")
        maven(url = "../gradle-plugin")
        mavenCentral()
    }
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        resolution("@webpack-cli/serve", "1.5.2")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react:17.0.2-pre.256-kotlin-1.5.31")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:17.0.2-pre.256-kotlin-1.5.31")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-styled:5.3.1-pre.256-kotlin-1.5.31")
    implementation(project(":shared"))
    testImplementation(kotlin("test"))
//    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-pre.256-kotlin-1.5.31")
//    api(npm("inline-style-prefixer", "^6.0.0"))
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
}

// TODO css functions replace