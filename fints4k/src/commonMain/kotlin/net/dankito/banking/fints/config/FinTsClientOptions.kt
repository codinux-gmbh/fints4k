package net.dankito.banking.fints.config

import net.dankito.banking.fints.model.ProductData

data class FinTsClientOptions(
    val version: String = "1.0.0", // TODO: get version dynamically
    val productName: String = "15E53C26816138699C7B6A3E8"
) {

    val product: ProductData by lazy { ProductData(productName, version) }

}