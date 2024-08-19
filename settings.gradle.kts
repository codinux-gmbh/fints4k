pluginManagement {
    val atomicfuVersion: String by settings

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("org.jetbrains.kotlinx.atomicfu") version atomicfuVersion
    }
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
