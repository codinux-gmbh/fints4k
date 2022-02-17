import net.dankito.banking.fints.FinTsClientDeprecated
import net.dankito.banking.fints.model.AccountTransaction
import net.dankito.banking.fints.model.AddAccountParameter
import react.RBuilder
import react.RComponent
import react.Props
import react.State
import react.dom.*
import styled.styledDiv

external interface AccountTransactionsViewProps : Props {
  var client: FinTsClientDeprecated
}

data class AccountTransactionsViewState(val balance: String, val transactions: Collection<AccountTransaction>) : State

@JsExport
class AccountTransactionsView(props: AccountTransactionsViewProps) : RComponent<AccountTransactionsViewProps, AccountTransactionsViewState>(props) {


  init {
    state = AccountTransactionsViewState("", listOf())

    // due to CORS your bank's servers can not be requested directly from browser -> set a CORS proxy url in main.kt
    // TODO: set your credentials here
    props.client.addAccountAsync(AddAccountParameter("", "", "", "")) { response ->
      if (response.successful) {
        val balance = response.retrievedData.sumOf { it.balance?.amount?.string?.replace(',', '.')?.toDoubleOrNull() ?: 0.0 } // i know, double is not an appropriate data type for amounts

        setState(AccountTransactionsViewState(balance.toString() + " " + (response.retrievedData.firstOrNull()?.balance?.currency ?: ""), response.retrievedData.flatMap { it.bookedTransactions }))
      }
    }
  }

  override fun RBuilder.render() {
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