import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.dankito.banking.client.model.AccountTransaction
import net.codinux.banking.fints.model.TanChallenge
import react.*
import react.dom.*
import styled.styledDiv

external interface AccountTransactionsViewProps : Props {
  var presenter: Presenter
}

data class AccountTransactionsViewState(val balance: String, val transactions: Collection<AccountTransaction>, var showTransferMoneyView: Boolean = false,
                                        val enterTanChallenge: TanChallenge? = null) : State

@JsExport
class AccountTransactionsView(props: AccountTransactionsViewProps) : RComponent<AccountTransactionsViewProps, AccountTransactionsViewState>(props) {


  init {
    state = AccountTransactionsViewState("", listOf())

    props.presenter.enterTanCallback = { setState(AccountTransactionsViewState(state.balance, state.transactions, state.showTransferMoneyView, it)) }

    // due to CORS your bank's servers can not be requested directly from browser -> set a CORS proxy url in main.kt
    // TODO: set your credentials here
    GlobalScope.launch {
      props.presenter.retrieveAccountData("", "", "") { response ->
        response.customerAccount?.let { customer ->
          val balance = customer.accounts.sumOf { it.balance?.amount?.string?.replace(',', '.')?.toDoubleOrNull() ?: 0.0 } // i know, double is not an appropriate data type for amounts

          setState(AccountTransactionsViewState(balance.toString() + " " + (customer.accounts.firstOrNull()?.balance?.currency ?: ""), customer.accounts.flatMap { it.bookedTransactions }, state.enterTanChallenge))
        }
      }
    }
  }

  override fun RBuilder.render() {
    state.enterTanChallenge?.let { challenge ->
      child(EnterTanView::class) {
        attrs {
          presenter = props.presenter
          tanChallenge = challenge
        }
      }
    }

    button {
      span { +"Transfer Money" }
      attrs {
        onMouseUp = { setState { showTransferMoneyView = !showTransferMoneyView } }
      }
    }

    if (state.showTransferMoneyView) {
      child(TransferMoneyView::class) {
        attrs {
          presenter = props.presenter
        }
      }
    }

    p {
      +"Saldo: ${state.balance}"
    }

    div {
      state.transactions.forEach { transaction ->
        div {
          styledDiv {
            if (transaction.showOtherPartyName) {
              div { transaction.otherPartyName }
            }

            div {
              +transaction.reference
            }
          }
        }
      }
    }
  }
}