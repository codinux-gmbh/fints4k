plugins {
    kotlin("multiplatform")
}


kotlin {
    targetHierarchy.default()

    linuxX64()
    mingwX64()
    macosX64()
    macosArm64()


    sourceSets {

        val nativeMain by getting {
            dependencies {
                implementation(project(":fints4k"))

                implementation("com.github.ajalt.clikt:clikt:3.5.4")

                // only needed for writing files to output
                implementation("com.soywiz.korlibs.korio:korio:3.4.0")
            }
        }

    }
}