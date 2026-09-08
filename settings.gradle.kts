pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // Official Mapbox Maven Repository (Configured with your Secret Token)
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication {
                create<BasicAuthentication>("basic")
            }
            credentials {
                username = "mapbox"
                password = "sk.eyJ1IjoiYmFrZWRyb3AxIiwiYSI6ImNtdGdzNTRxbjFxZWgyenM2dGFxZmJkb2IifQ.hX9vC_WyQhPzMPozj5anUA"
            }
        }
    }
}

rootProject.name = "BakeDrop"
include(":app")