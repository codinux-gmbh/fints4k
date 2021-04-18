package net.dankito.banking.fints.rest.model

import net.dankito.banking.fints.model.EnterTanResult
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference


class EnterTanContext(
    val enterTanResult: AtomicReference<EnterTanResult>,
    val responseHolder: ResponseHolder<*>,
    val countDownLatch: CountDownLatch,
    val tanRequestedTimeStamp: Date = Date()
)