package net.dankito.banking.client.model


open class CustomerCredentials(
    open val bankCode: String,
    open val loginName: String,
    open val password: String,
    open val finTsServerAddress: String // TODO: get rid of this
) {

    internal constructor() : this("", "", "", "") // for object deserializers

}