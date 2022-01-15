rootProject.name = "css-plugin-test"

include("plugin")
include("stylesheets")
include("shared")
pluginManagement {
    repositories {
        maven(url = "./gradle-plugin")
        gradlePluginPortal()
    }
}