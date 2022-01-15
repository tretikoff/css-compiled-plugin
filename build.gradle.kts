buildscript {
    repositories {
        maven(url = "./gradle-plugin")
    }
}
val kotlin_version = "1.6.0"
val react_version = "17.0.2-pre.278-kotlin-$kotlin_version"
val react_dom_version = "17.0.2-pre.278-kotlin-$kotlin_version"
val styled_next_version = "1.0-pre.278-kotlin-$kotlin_version"
plugins {
    kotlin("js") version "1.6.0"
    id("CssGradlePlugin") version "0.1"
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

dependencies {
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$react_version")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$react_dom_version")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-styled-next:$styled_next_version")
    implementation(project(":shared"))
    implementation(project(":stylesheets"))
    testImplementation(kotlin("test"))
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