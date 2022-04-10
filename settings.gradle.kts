rootProject.name = "css-plugin-test"

include("plugin")
include("stylesheets")
include("shared")
pluginManagement {
    repositories {
        maven(url = "./gradle-plugin")
        gradlePluginPortal()
    }
//    resolutionStrategy {
//        eachPlugin {
//            if (requested.id.id == "kotlin2js") {
//                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
//            }
//        }
//    }
}