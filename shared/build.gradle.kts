plugins {
    kotlin("multiplatform")
    id("CssGradlePlugin")
}

val cssVersion = "1.0.0-pre.278-kotlin-1.6.0"

kotlin {
    js(IR)
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin-wrappers:kotlin-css:$cssVersion")
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
