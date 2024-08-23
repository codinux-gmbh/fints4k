pluginManagement {
    val kotlinVersion: String by settings
    val atomicfuVersion: String by settings

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
//        id("org.jetbrains.kotlin.multiplatform") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("org.jetbrains.kotlinx.atomicfu") version atomicfuVersion
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}


rootProject.name = "fints4kProject"


include(":fints4k")


// Sample applications

//include "AndroidApp"
//findProject(":AndroidApp")?.projectDir = file("SampleApplications/AndroidApp")
//findProject(":AndroidApp")?.name = "AndroidApp"
//
//include "WebApp"
//findProject(":WebApp")?.projectDir = file("SampleApplications/WebApp")
//findProject(":WebApp")?.name = "WebApp"
//
//include "CorsProxy"
//findProject(":CorsProxy")?.projectDir = file("SampleApplications/CorsProxy")
//findProject(":CorsProxy")?.name = "CorsProxy"

include("NativeApp")
findProject(":NativeApp")?.projectDir = file("SampleApplications/NativeApp")
findProject(":NativeApp")?.name = "NativeApp"

