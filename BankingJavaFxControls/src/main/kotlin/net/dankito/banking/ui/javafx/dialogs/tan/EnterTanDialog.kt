package net.dankito.banking.ui.javafx.dialogs.tan

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Insets
import javafx.geometry.Pos
import net.dankito.banking.javafx.dialogs.tan.controls.ChipTanFlickerCodeView
import net.dankito.banking.ui.javafx.dialogs.JavaFxDialogService
import net.dankito.banking.ui.model.Account
import net.dankito.banking.ui.model.responses.BankingClientResponse
import net.dankito.banking.ui.model.tan.*
import net.dankito.banking.ui.presenter.MainWindowPresenter
import net.dankito.utils.javafx.ui.dialogs.Window
import tornadofx.*


open class EnterTanDialog(
    protected val account: Account,
    protected val challenge: TanChallenge,
    protected val presenter: MainWindowPresenter,
    protected val tanEnteredCallback: (EnterTanResult) -> Unit
) : Window() {

    companion object {
        private val ButtonHeight = 40.0
        private val ButtonWidth = 150.0
    }


    protected val dialogService = JavaFxDialogService()


    protected val selectedTanProcedure = SimpleObjectProperty<TanProcedure>(account.selectedTanProcedure ?: account.supportedTanProcedures.firstOrNull())

    protected val selectedTanMedium = SimpleObjectProperty<TanMedium>(account.tanMediaSorted.firstOrNull())

    protected val enteredTan = SimpleStringProperty("")


    init {
        selectedTanProcedure.addListener { _, _, newValue ->
            tanEnteredCallback(EnterTanResult.userAsksToChangeTanProcedure(newValue))

            close()
        }

        selectedTanMedium.addListener { _, _, newValue ->
            if (newValue.status != TanMediumStatus.Used) {
                tanEnteredCallback(EnterTanResult.userAsksToChangeTanMedium(newValue) { response ->
                    runLater { handleChangeTanMediumResponseOnUiThread(newValue, response) }
                })
            }
        }
    }


    override val root = vbox {
        paddingAll = 4.0

        form {
            fieldset {
                field(messages["enter.tan.dialog.select.tan.procedure"]) {
                    combobox(selectedTanProcedure, account.supportedTanProcedures) {
                        cellFormat {
                            text = it.displayName
                        }
                    }
                }

                if (account.tanMediaSorted.isNotEmpty()) {
                    field(messages["enter.tan.dialog.select.tan.medium"]) {
                        combobox(selectedTanMedium, account.tanMediaSorted) {
                            cellFormat {
                                text = it.displayName
                            }
                        }
                    }
                }
            }
        }

        (challenge as? FlickerCodeTanChallenge)?.let { flickerCodeTanChallenge ->
            val flickerCode = flickerCodeTanChallenge.flickerCode
            if (flickerCode.decodingSuccessful) {
                hbox {
                    alignment = Pos.CENTER

                    vboxConstraints {
                        marginLeftRight(30.0)
                        marginBottom = 12.0
                    }

                    add(ChipTanFlickerCodeView(flickerCode))
                }
            }
            else {
                showDecodingTanChallengeFailedError(flickerCode.decodingError)
            }
        }

        hbox {
            maxWidth = 400.0

            label(challenge.messageToShowToUser) {
                isWrapText = true
            }

            vboxConstraints {
                marginTopBottom(6.0)
            }
        }

        hbox {
            alignment = Pos.CENTER_LEFT

            label(messages["enter.tan.dialog.enter.tan.label"])

            textfield(enteredTan) {
                prefHeight = 30.0
                prefWidth = 110.0

                runLater {
                    requestFocus()
                }

                hboxConstraints {
                    marginLeft = 6.0
                }
            }
        }

        hbox {
            alignment = Pos.CENTER_RIGHT

            button(messages["cancel"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                action { cancelledEnteringTan() }

                hboxConstraints {
                    margin = Insets(6.0, 0.0, 4.0, 0.0)
                }
            }

            button(messages["ok"]) {
                prefHeight = ButtonHeight
                prefWidth = ButtonWidth

                isDefaultButton = true

                action { finishedEnteringTan() }

                hboxConstraints {
                    margin = Insets(6.0, 4.0, 4.0, 12.0)
                }
            }

            vboxConstraints {
                marginTop = 4.0
            }
        }
    }


    protected open fun showDecodingTanChallengeFailedError(error: Exception?) {
        dialogService.showErrorMessage(String.format(messages["enter.tan.dialog.error.could.not.decode.tan.image"], error?.localizedMessage),
            null, error, currentStage)
    }

    protected open fun handleChangeTanMediumResponseOnUiThread(newUsedTanMedium: TanMedium, response: BankingClientResponse) {
        if (response.isSuccessful) {
            dialogService.showInfoMessageOnUiThread(String.format(messages["enter.tan.dialog.tan.medium.successfully.changed"],
                newUsedTanMedium.displayName), null, currentStage)

            close()
        }
        else {
            dialogService.showErrorMessageOnUiThread(String.format(messages["enter.tan.dialog.tan.error.changing.tan.medium"],
                newUsedTanMedium.displayName, response.errorToShowToUser), null, response.error, currentStage)
        }
    }


    protected open fun finishedEnteringTan() {
        if (enteredTan.value.isNullOrEmpty()) {
            cancelledEnteringTan()
        }
        else {
            tanEnteredCallback(EnterTanResult.userEnteredTan(enteredTan.value))

            close()
        }
    }

    protected open fun cancelledEnteringTan() {
        tanEnteredCallback(EnterTanResult.userDidNotEnterTan())

        close()
    }

}