plugins {
    id("kotlin")
}
val kotlin_version = "1.6.0"
val css_version = "1.0.0-pre.278-kotlin-$kotlin_version"

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlin_version")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:$css_version")
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
