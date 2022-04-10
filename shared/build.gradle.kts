plugins {
    kotlin("multiplatform")
    id("CssGradlePlugin")
}

val cssVersion = "1.0.0-pre.337"

kotlin {
    js(IR)
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
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
