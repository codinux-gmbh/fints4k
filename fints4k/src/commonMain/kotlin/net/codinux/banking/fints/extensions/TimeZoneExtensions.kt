package net.codinux.banking.fints.extensions

import kotlinx.datetime.TimeZone


val TimeZone.Companion.EuropeBerlin: TimeZone
 get() = TimeZone.of("Europe/Berlin")