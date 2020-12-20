package net.dankito.banking.ui.model.issues

import kotlinx.serialization.Serializable


@Serializable
class CreateTicketRequestDto(
    val issueDescription: String,
    val applicationName: String,
    val format: IssueDescriptionFormat = IssueDescriptionFormat.PlainText,
    val osName: String? = null,
    val osVersion: String? = null,
    val deviceManufacturer: String? = null,
    val deviceModel: String? = null
)