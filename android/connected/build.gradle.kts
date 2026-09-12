import java.util.Properties

plugins { id("com.android.application"); kotlin("android") }
val localConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val configuredOrigin = providers.gradleProperty("photohouseOrigin").orElse(localConfig.getProperty("photohouseOrigin", "")).get()
// Only an origin is configurable; no credentials or trust overrides are build inputs.
require(configuredOrigin.none { it == '\n' || it == '\r' || it == '"' || it == '\\' }) { "Invalid configured origin" }
val discoveryEnabled = providers.gradleProperty("photohousePhoneDiscoveryEnabled").orElse("false").get()
require(discoveryEnabled in listOf("true", "false")) { "Invalid phone discovery switch" }
val photoDeliveryEnabled = providers.gradleProperty("photohousePhonePhotoDeliveryEnabled").orElse("false").get()
require(photoDeliveryEnabled in listOf("true", "false"))
android {
    sourceSets.getByName("main").res.srcDir("../branding/res")
    namespace = "dev.photohouse.connected"
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    defaultConfig {
        applicationId = "dev.photohouse.connected"
        minSdk = 26
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 5
        versionName = "0.6-phone-on-demand"
        buildConfigField("boolean", "PHOTOHOUSE_DISCOVERY_ENABLED", discoveryEnabled)
        buildConfigField("boolean", "PHOTOHOUSE_PHOTO_DELIVERY_ENABLED", photoDeliveryEnabled)
        buildConfigField("String", "PHOTOHOUSE_ORIGIN", "\"$configuredOrigin\"")
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
    implementation(project(":live-core"))
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
