plugins {
    kotlin("jvm") version "1.6.0"
    id("java-gradle-plugin")
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("gradle-plugin-api"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

group="CssGradlePlugin"

gradlePlugin {
    plugins {
        create("CssGradlePlugin") {
            id = "CssGradlePlugin"
            implementationClass = "CssGradlePlugin"
            version = "0.1"
        }
    }
}


publishing {
    repositories {
        maven {
            url = uri(".")
        }
    }
}