pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "PhotoHouseFixture"
include(":protocol", ":core", ":app", ":live-core", ":connected", ":tv", ":home-core")
include(":story-fixture-core")
