import java.util.Properties

plugins { id("com.android.application"); kotlin("android") }
val localConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val configuredOrigin = providers.gradleProperty("photohouseTvOrigin").orElse(localConfig.getProperty("photohouseTvOrigin", "")).get()
val configuredLanAddress = providers.gradleProperty("photohouseTvLanAddress").orElse(localConfig.getProperty("photohouseTvLanAddress", "")).get()
val configuredCatalogVersion = providers.gradleProperty("photohouseTvCatalogVersion").orElse(localConfig.getProperty("photohouseTvCatalogVersion", "1")).get()
require(configuredCatalogVersion in listOf("1", "2")) { "Unsupported TV catalog version" }
val configuredDiscovery = providers.gradleProperty("photohouseTvDiscoveryEnabled").orElse(localConfig.getProperty("photohouseTvDiscoveryEnabled", "false")).get()
require(configuredDiscovery in listOf("true", "false")) { "Invalid discovery switch" }
require(configuredDiscovery != "true" || configuredCatalogVersion == "2") { "Discovery requires catalog v2" }
// Private endpoint routing only; no credentials or certificate trust overrides.
require(configuredLanAddress.isEmpty() || configuredOrigin.isNotEmpty()) { "LAN address requires an HTTPS origin" }
require(configuredLanAddress.isEmpty() || configuredLanAddress.matches(Regex("[0-9.]{7,15}"))) { "Invalid LAN address" }
require(configuredOrigin.none { it == '\n' || it == '\r' || it == '"' || it == '\\' }) { "Invalid configured origin" }
android {
    sourceSets.getByName("main").res.srcDir("../branding/res")
    namespace = "dev.photohouse.tv"
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    defaultConfig {
        applicationId = "dev.photohouse.tv"
        minSdk = 26
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 10
        versionName = "0.10-tv-playback-diagnostics"
        buildConfigField("int", "PHOTOHOUSE_CATALOG_VERSION", configuredCatalogVersion)
        buildConfigField("boolean", "PHOTOHOUSE_DISCOVERY_ENABLED", configuredDiscovery)
        buildConfigField("String", "PHOTOHOUSE_ORIGIN", "\"$configuredOrigin\"")
        buildConfigField("String", "PHOTOHOUSE_LAN_ADDRESS", "\"$configuredLanAddress\"")
    }
    androidComponents { beforeVariants(selector().withBuildType("release")) { it.enable = false } }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = true; checkReleaseBuilds = false }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation(project(":home-core"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
