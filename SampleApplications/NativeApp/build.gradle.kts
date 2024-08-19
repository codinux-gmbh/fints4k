plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlinx.atomicfu")
}


kotlin {
    linuxX64()
    mingwX64()
    macosX64()
    macosArm64()

    applyDefaultHierarchyTemplate()


    val kotlinxSerializationVersion: String by project

    sourceSets {

        val nativeMain by getting {
            dependencies {
                implementation(project(":fints4k"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

                implementation("com.github.ajalt.clikt:clikt:3.5.4")

                // only needed for writing files to output
                implementation("com.soywiz.korlibs.korio:korio:3.4.0")
            }
        }

    }
}