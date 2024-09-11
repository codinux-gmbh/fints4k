package net.codinux.banking.fints.transactions.swift.model

class SwiftMessageBlock(
    initialFields: List<Pair<String, String>>? = null
) {

    private val fields = LinkedHashMap<String, MutableList<String>>()

    private val fieldsInOrder = mutableListOf<Pair<String, String>>()

    val hasFields: Boolean
        get() = fields.isNotEmpty()

    val fieldCodes: Collection<String>
        get() = fields.keys

    init {
        initialFields?.forEach { (fieldCode, fieldValue) ->
            addField(fieldCode, fieldValue)
        }
    }


    fun addField(fieldCode: String, fieldValueLines: List<String>, rememberOrderOfFields: Boolean = false) {
        val fieldValue = fieldValueLines.joinToString("\n")

        addField(fieldCode, fieldValue, rememberOrderOfFields)
    }

    fun addField(fieldCode: String, fieldValue: String, rememberOrderOfFields: Boolean = false) {
        fields.getOrPut(fieldCode) { mutableListOf() }.add(fieldValue)

        if (rememberOrderOfFields) {
            fieldsInOrder.add(Pair(fieldCode, fieldValue))
        }
    }


    fun getFieldsInOrder(): List<Pair<String, String>> = fieldsInOrder.toList() // make a copy

    fun getMandatoryField(fieldCode: String): String =
        getMandatoryFieldValue(fieldCode).first()

    fun getOptionalField(fieldCode: String): String? =
        getOptionalFieldValue(fieldCode)?.first()

    fun getMandatoryRepeatableField(fieldCode: String): List<String> =
        getMandatoryFieldValue(fieldCode)

    fun getOptionalRepeatableField(fieldCode: String): List<String>? =
        getOptionalFieldValue(fieldCode)

    private fun getMandatoryFieldValue(fieldCode: String): List<String> =
        fields[fieldCode] ?: fields.entries.firstOrNull { it.key.startsWith(fieldCode) }?.value
            ?: throw IllegalStateException("Block contains no field with code '$fieldCode'. Available fields: ${fields.keys}")

    private fun getOptionalFieldValue(fieldCode: String): List<String>? = fields[fieldCode]


    override fun toString() =
        if (fieldsInOrder.isNotEmpty()) {
            fieldsInOrder.joinToString("\n")
        } else {
            fields.entries.joinToString("\n") { "${it.key}${it.value}" }
        }

}