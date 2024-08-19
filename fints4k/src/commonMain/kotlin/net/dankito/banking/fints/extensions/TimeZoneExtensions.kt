package net.dankito.banking.fints.extensions

import kotlinx.datetime.TimeZone


val TimeZone.Companion.europeBerlin: TimeZone
 get() = TimeZone.of("Europe/Berlin")