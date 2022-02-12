package net.dankito.utils.multiplatform.extensions

import kotlinx.datetime.TimeZone


val TimeZone.Companion.europeBerlin: TimeZone
 get() = TimeZone.of("Europe/Berlin")