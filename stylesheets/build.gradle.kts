plugins {
    kotlin("multiplatform")
    id("CssGradlePlugin")
}

val kotlin_version = "1.6.20"
val styled_next_version = "1.1.0-pre.337"

kotlin {
    js(IR)
    sourceSets {
        val jsMain by getting {
            dependencies {
                    implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlin-wrappers:kotlin-styled-next:$styled_next_version")
            }
        }
        create("jsAsync_PDFViewer")
    }
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
