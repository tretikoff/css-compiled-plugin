plugins {
    kotlin("js")
//    id("CssGradlePlugin")
}

val css_version = "1.0.0-pre.265-kotlin-1.5.31"

dependencies {
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:$css_version")
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
