package net.dankito.utils.multiplatform

// required so that Joda time zones get loaded, see:
// https://github.com/Kotlin/kotlinx-datetime#note-about-time-zones-in-js
// https://github.com/Kotlin/kotlinx-datetime/blob/master/core/js/test/JsJodaTimeZoneModule.kt

@JsModule("@js-joda/timezone")
@JsNonModule
external object JsJodaTimeZoneModule

private val jsJodaTz = JsJodaTimeZoneModule