package net.dankito.banking.fints.rest.model

import java.util.concurrent.CountDownLatch


class ResponseHolder<T>() {

    private var responseReceivedLatch = CountDownLatch(1)

    constructor(error: String) : this() {
        setError(error)
    }


    var response: T? = null
        private set

    var error: String? = null
        private set

    var enterTanRequest: EnteringTanRequested? = null
        private set


    fun setResponse(response: T) {
        this.response = response

        signalResponseReceived()
    }

    fun setError(error: String) {
        this.error = error

        signalResponseReceived()
    }

    fun setEnterTanRequest(enterTanRequest: EnteringTanRequested) {
        this.enterTanRequest = enterTanRequest

        signalResponseReceived()
    }


    fun waitForResponse() {
        responseReceivedLatch.await()
    }

    fun resetAfterEnteringTan() {
        this.enterTanRequest = null

        responseReceivedLatch = CountDownLatch(1)
    }

    private fun signalResponseReceived() {
        responseReceivedLatch.countDown()
    }


    override fun toString(): String {
        return "Error: $error, TAN requested: $enterTanRequest, success: $response"
    }

}