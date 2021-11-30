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
    publications {
        create<MavenPublication>("CssGradlePlugin") {
            groupId = "CssGradlePlugin"
            artifactId = "CssGradlePlugin"
            version = "0.1"
            from(components["kotlin"])
        }
    }
    repositories {
        maven {
            url = uri(".")
        }
    }
}