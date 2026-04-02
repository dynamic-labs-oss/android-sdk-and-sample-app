import org.gradle.authentication.http.BasicAuthentication

pluginManagement {
    val jfrogUsername: String? = System.getenv("JFROG_USERNAME")
    val jfrogToken: String? = System.getenv("JFROG_ACCESS_TOKEN")
    val hasJfrogCredentials = !jfrogUsername.isNullOrBlank() && !jfrogToken.isNullOrBlank()

    repositories {
        if (hasJfrogCredentials) {
            maven {
                name = "JFrog"
                url = uri("https://fbinfra555artifactory.jfrog.io/artifactory/fireblocks-gradle")
                credentials {
                    username = jfrogUsername
                    password = jfrogToken
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.1"
        id("org.jetbrains.kotlin.android") version "2.1.0"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    }
}

dependencyResolutionManagement {
    val jfrogUsername: String? = System.getenv("JFROG_USERNAME")
    val jfrogToken: String? = System.getenv("JFROG_ACCESS_TOKEN")
    val hasJfrogCredentials = !jfrogUsername.isNullOrBlank() && !jfrogToken.isNullOrBlank()

    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        if (hasJfrogCredentials) {
            maven {
                name = "JFrog"
                url = uri("https://fbinfra555artifactory.jfrog.io/artifactory/fireblocks-gradle")
                credentials {
                    username = jfrogUsername
                    password = jfrogToken
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "DynamicSDKExample"
