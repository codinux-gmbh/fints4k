package net.dankito.utils.multiplatform.os


enum class OsType {

    JVM,

    Android,

    iOS,

    JavaScript, // TODO: differenciate between Browser and NodeJS

    Native // TODO: get if running on Linux, Windows or macOs

}