buildscript {
    repositories {
        maven(url = "./gradle-plugin")
    }
}
val kotlin_version = "1.6.20"
val react_version = "18.1.0-pre.337"
val react_dom_version = "18.1.0-pre.337"
val styled_next_version = "1.1.0-pre.337"

plugins {
    kotlin("js") version "1.6.20"
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
    implementation(kotlin("stdlib-js"))
    testImplementation(kotlin("test"))
}

kotlin {
    js(IR) {
        useCommonJs()
        binaries.executable()
        browser {
            dceTask {
                dceOptions.devMode = true
            }
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
}