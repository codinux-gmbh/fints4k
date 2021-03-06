package net.dankito.banking.ui.javafx.controls

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.scene.control.ContextMenu
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import net.dankito.banking.ui.javafx.dialogs.JavaFxDialogService
import net.dankito.banking.ui.model.IAccountTransaction
import net.dankito.banking.ui.model.TypedBankAccount
import net.dankito.banking.ui.model.parameters.TransferMoneyData
import net.dankito.banking.ui.model.responses.GetTransactionsResponse
import net.dankito.banking.ui.presenter.BankingPresenter
import tornadofx.*


open class AccountTransactionsView(private val presenter: BankingPresenter) : View() {

    protected val isAccountSelected = SimpleBooleanProperty(false)

    protected val transactionsFilter = SimpleStringProperty("")

    protected val balance = SimpleStringProperty("")

    protected val transactionsToDisplay = FXCollections.observableArrayList<IAccountTransaction>(listOf())


    protected var currentMenu: ContextMenu? = null


    init {
        presenter.addSelectedAccountsChangedListener { handleSelectedBankAccountsChanged(it) }

        presenter.addRetrievedAccountTransactionsResponseListener { response ->
            handleGetTransactionsResponseOffUiThread(response)
        }

        transactionsFilter.addListener { _, _, newValue -> updateTransactionsToDisplay(newValue) }

        handleSelectedBankAccountsChanged(presenter.selectedAccounts) // so that isAccountSelected and transactionsToDisplay get set
    }


    override val root = vbox {
        add(AccountTransactionsControlView(presenter, transactionsFilter, balance).apply {
            enableWhen(isAccountSelected)
        })

        add(AccountTransactionsTable(presenter, transactionsToDisplay).apply {
            setOnMouseClicked { tableClicked(it, this.selectionModel.selectedItem) }

            setOnContextMenuRequested { event -> showContextMenu(event, this) }
        })
    }


    protected open fun tableClicked(event: MouseEvent, selectedItem: IAccountTransaction?) {
        if (event.button == MouseButton.PRIMARY || event.button == MouseButton.MIDDLE) {
            currentMenu?.hide()
        }

        if(event.clickCount == 2 && event.button == MouseButton.PRIMARY) {
            if(selectedItem != null) {
                showTransactionDetailsDialog(selectedItem)
            }
        }
    }

    protected open fun showContextMenu(event: ContextMenuEvent, table: AccountTransactionsTable) {
        currentMenu?.hide()

        val selectedItem = table.selectionModel.selectedItem

        if (selectedItem != null) {
            currentMenu = createContextMenuForItems(selectedItem)
            currentMenu?.show(table, event.screenX, event.screenY)
        }
    }

    protected open fun createContextMenuForItems(selectedItem: IAccountTransaction): ContextMenu {
        val contextMenu = ContextMenu()

        contextMenu.apply {
            if (selectedItem.canCreateMoneyTransferFrom) {
                item(String.format(FX.messages["account.transactions.table.context.menu.new.transfer.to.same.recipient"], selectedItem.otherPartyName)) {
                    action { newTransferToSameTransactionParty(selectedItem) }
                }

                item(String.format(FX.messages["account.transactions.table.context.menu.new.transfer.with.same.data"], selectedItem.otherPartyName)) {
                    action { newTransferWithSameData(selectedItem) }
                }

                separator()
            }

            item(FX.messages["account.transactions.table.context.menu.show.transaction.details"]) {
                action {
                    showTransactionDetailsDialog(selectedItem)
                }
            }
        }

        return contextMenu
    }

    protected open fun showTransactionDetailsDialog(transaction: IAccountTransaction) {
        // TODO:
//        presenter.showTransactionDetailsWindow(transaction.item)
    }

    protected open fun newTransferToSameTransactionParty(transaction: IAccountTransaction) {
        presenter.showTransferMoneyDialog(TransferMoneyData.fromAccountTransactionWithoutAmountAndReference(transaction))
    }

    protected open fun newTransferWithSameData(transaction: IAccountTransaction) {
        presenter.showTransferMoneyDialog(TransferMoneyData.fromAccountTransaction(transaction))
    }


    protected open fun handleSelectedBankAccountsChanged(selectedBankAccounts: List<TypedBankAccount>) {
        runLater {
            isAccountSelected.value = selectedBankAccounts.isNotEmpty()

            updateTransactionsToDisplay()
        }
    }

    protected open fun updateTransactionsToDisplay() {
        updateTransactionsToDisplay(transactionsFilter.value)
    }

    protected open fun updateTransactionsToDisplay(filter: String) {
        transactionsToDisplay.setAll(presenter.searchSelectedAccountTransactions(filter))

        // TODO: if transactions are filtered calculate and show balance of displayed transactions?
        balance.value = presenter.formatAmount(presenter.balanceOfSelectedAccounts)
    }

    protected open fun handleGetTransactionsResponseOffUiThread(response: GetTransactionsResponse) {
        runLater { handleGetTransactionsResponseOnUiThread(response) }
    }

    protected open fun handleGetTransactionsResponseOnUiThread(response: GetTransactionsResponse) {
        response.retrievedData.forEach { retrievedData ->
            if (retrievedData.successfullyRetrievedData) {
                updateTransactionsToDisplay()
            }
            else if (response.userCancelledAction == false) { // if user cancelled entering TAN then don't show a error message
                JavaFxDialogService().showErrorMessageOnUiThread(
                    String.format(messages["account.transactions.control.view.could.not.retrieve.account.transactions"], retrievedData.account.displayName, response.errorToShowToUser)
                )
            }
        }
    }

}
