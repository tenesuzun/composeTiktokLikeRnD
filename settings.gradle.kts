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

        // Byteplus denemesi için
        maven {
            url = uri("https://artifact.byteplus.com/repository/public/")
        }
        maven {
            url = uri("https://artifact.bytedance.com/repository/maven/")
        }
        maven {
            url = uri("https://artifact.bytedance.com/repository/Volcengine/")
        }

        // For versions earlier than 1.41.200.x, you also need to add the following repository
        // maven {
        //     url "https://artifact.bytedance.com/repository/Volcengine/"
        //  }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Byteplus denemesi için
        maven {
            url = uri("https://artifact.byteplus.com/repository/public/")
        }
        maven {
            url = uri("https://artifact.bytedance.com/repository/maven/")
        }
        maven {
            url = uri("https://artifact.bytedance.com/repository/Volcengine/")
        }
    }
}

rootProject.name = "atvRnD"
include(":app")
 