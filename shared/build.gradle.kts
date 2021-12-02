plugins {
    kotlin("js")
    id("CssGradlePlugin")
}

dependencies {
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-pre.247-kotlin-1.5.31")
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
