package net.dankito.banking.fints.config

import net.dankito.banking.fints.model.ProductData

data class FinTsClientOptions(

    /**
     * If FinTS messages sent to and received from bank servers and errors should be collected. They are then accessible
     * via [net.dankito.banking.fints.response.client.FinTsClientResponse.messageLog].
     *
     * Set to false by default.
     */
    val collectMessageLog: Boolean = false,

    /**
     * If set to true then [net.dankito.banking.fints.callback.FinTsClientCallback.messageLogAdded] get fired when a
     * FinTS message get sent to bank server, a FinTS message is received from bank server or an error occurred.
     *
     * Defaults to false.
     */
    val fireCallbackOnMessageLogs: Boolean = false,

    /**
     * If sensitive data like user name, password, login name should be removed from FinTS messages before being logged.
     *
     * Defaults to true.
     */
    val removeSensitiveDataFromMessageLog: Boolean = true,
    val version: String = "1.0.0", // TODO: get version dynamically
    val productName: String = "15E53C26816138699C7B6A3E8"
) {

    val product: ProductData by lazy { ProductData(productName, version) }

}