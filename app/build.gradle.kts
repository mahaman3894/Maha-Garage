import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "maha.garage"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mahaman.myapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 104
        versionName = "1.1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
            arg("room.expandProjection", "true")
        }
    }

    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) {
            FileInputStream(f).use { load(it) }
        }
    }
    val storeFilePath = localProps.getProperty("RELEASE_STORE_FILE") ?: System.getenv("RELEASE_STORE_FILE") ?: ""
    val storePasswordVal = localProps.getProperty("RELEASE_STORE_PASSWORD") ?: System.getenv("RELEASE_STORE_PASSWORD") ?: ""
    val keyAliasVal = localProps.getProperty("RELEASE_KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS") ?: ""
    val keyPasswordVal = localProps.getProperty("RELEASE_KEY_PASSWORD") ?: System.getenv("RELEASE_KEY_PASSWORD") ?: ""

    signingConfigs {
        create("release") {
            val keyFile = rootProject.file(storeFilePath)
            if (keyFile.exists() && storePasswordVal.isNotEmpty()) {
                storeFile = keyFile
                storePassword = storePasswordVal
                keyAlias = keyAliasVal
                keyPassword = keyPasswordVal
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            }
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.all {
            it.jvmArgs("-Dfile.encoding=UTF-8")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.1")
    // Play app-update pulls Fragment 1.1.0; ActivityResult requires 1.3.0 or newer.
    implementation("androidx.fragment:fragment:1.8.9")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.navigation:navigation-compose:2.8.5")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.datastore:datastore-preferences:1.1.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Google ML Kit On-Device Text Recognition (한국어 & 숫자)
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")

    // CameraX (영수증 촬영 뷰파인더)
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Retrofit2 & OkHttp & Moshi (공공데이터 차량 제원 API 통신)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

    val firebaseBom = platform("com.google.firebase:firebase-bom:33.8.0")
    implementation(firebaseBom)
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20180813")
    testImplementation("androidx.compose.ui:ui-graphics")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.exifinterface:exifinterface:1.3.7")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

tasks.register<JavaExec>("runVerifyTests") {
    dependsOn("compileDebugKotlin", "compileDebugUnitTestKotlin")
    workingDir = rootDir
    classpath = files(
        configurations.getByName("debugUnitTestRuntimeClasspath"),
        configurations.getByName("debugRuntimeClasspath"),
        tasks.named("compileDebugKotlin").get().outputs.files,
        tasks.named("compileDebugUnitTestKotlin").get().outputs.files,
        android.bootClasspath
    )
    mainClass.set("maha.garage.VerifyRunnerKt")
}
