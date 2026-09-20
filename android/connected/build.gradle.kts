import java.util.Properties

plugins { id("com.android.application"); kotlin("android") }
val localConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val configuredOrigin = providers.gradleProperty("photohouseOrigin").orElse(localConfig.getProperty("photohouseOrigin", "")).get()
// Only an origin is configurable; no credentials or trust overrides are build inputs.
require(configuredOrigin.none { it == '\n' || it == '\r' || it == '"' || it == '\\' }) { "Invalid configured origin" }
val protectedNativeV2Enabled = providers.gradleProperty("photohouseProtectedNativeV2Enabled").orElse("false").get()
require(protectedNativeV2Enabled in listOf("true", "false"))
val discoveryEnabled = providers.gradleProperty("photohousePhoneDiscoveryEnabled").orElse("false").get()
require(discoveryEnabled in listOf("true", "false")) { "Invalid phone discovery switch" }
val photoDeliveryEnabled = providers.gradleProperty("photohousePhonePhotoDeliveryEnabled").orElse("false").get()
require(photoDeliveryEnabled in listOf("true", "false"))
val preparedVideoEnabled = providers.gradleProperty("photohousePhonePreparedVideoEnabled").orElse("false").get()
require(preparedVideoEnabled in listOf("true", "false"))
require(preparedVideoEnabled != "true" || protectedNativeV2Enabled == "true")
// Home mode has independent routing. Neither field can carry account credentials.
val homeOrigin = providers.gradleProperty("photohousePhoneHomeOrigin").orElse(localConfig.getProperty("photohousePhoneHomeOrigin", "")).get()
val homeAddress = providers.gradleProperty("photohousePhoneHomeLanAddress").orElse(localConfig.getProperty("photohousePhoneHomeLanAddress", "")).get()
for (value in listOf(homeOrigin, homeAddress)) require(value.none { it == '\n' || it == '\r' || it == '"' || it == '\\' })
val homeDiscoveryEnabled = providers.gradleProperty("photohousePhoneHomeDiscoveryEnabled").orElse("false").get()
require(homeDiscoveryEnabled in listOf("true", "false"))
val tagLookupEnabled = providers.gradleProperty("photohouseHomeTagLookupEnabled").orElse("false").get()
require(tagLookupEnabled in listOf("true", "false"))
val calendarEnabled = providers.gradleProperty("photohouseHomeCalendarEnabled").orElse("false").get()
require(calendarEnabled in listOf("true", "false"))
require(calendarEnabled != "true" || tagLookupEnabled == "true")
val storyFixtureFlag = providers.gradleProperty("photohouseStoryFixtureEnabled").orElse("false").get()
require(storyFixtureFlag in listOf("true", "false"))
val storyFixtureEnabled = storyFixtureFlag == "true"
android {
    if (storyFixtureEnabled) {
        sourceSets.getByName("debug").java.srcDir("../story-fixture-ui/src/main/java")
        sourceSets.getByName("debug").manifest.srcFile("../story-fixture-ui/src/main/AndroidManifest.xml")
        sourceSets.getByName("androidTest").java.srcDir("../story-fixture-ui/src/androidTest/java")
    }
    sourceSets.getByName("main").res.srcDir("../branding/res")
    namespace = "dev.photohouse.connected"
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    defaultConfig {
        buildConfigField("boolean", "PHOTOHOUSE_HOME_CALENDAR_ENABLED", calendarEnabled)
        buildConfigField("boolean", "PHOTOHOUSE_HOME_TAG_LOOKUP_ENABLED", tagLookupEnabled)
        applicationId = "dev.photohouse.connected"
        minSdk = 26
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 14
        versionName = "0.15-fullscreen-photos"
        buildConfigField("boolean", "PHOTOHOUSE_HOME_DISCOVERY_ENABLED", homeDiscoveryEnabled)
        buildConfigField("String", "PHOTOHOUSE_HOME_ORIGIN", "\"$homeOrigin\"")
        buildConfigField("String", "PHOTOHOUSE_HOME_LAN_ADDRESS", "\"$homeAddress\"")
        buildConfigField("boolean", "PHOTOHOUSE_PROTECTED_NATIVE_V2_ENABLED", protectedNativeV2Enabled)
        buildConfigField("boolean", "PHOTOHOUSE_DISCOVERY_ENABLED", discoveryEnabled)
        buildConfigField("boolean", "PHOTOHOUSE_PHOTO_DELIVERY_ENABLED", photoDeliveryEnabled)
        buildConfigField("boolean", "PHOTOHOUSE_PREPARED_VIDEO_ENABLED", preparedVideoEnabled)
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
    if (storyFixtureEnabled) debugImplementation(project(":story-fixture-core"))
    implementation(project(":live-core"))
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
