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
    }
}

rootProject.name = "Artiface"

include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:preferences")
include(":core:testing")
include(":feature:onboarding")
include(":feature:camera")
include(":feature:preview")
include(":feature:processing")
include(":feature:result")
include(":feature:gallery")
include(":feature:settings")
