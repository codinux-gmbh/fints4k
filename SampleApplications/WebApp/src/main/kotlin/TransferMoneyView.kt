import react.*
import react.dom.*

external interface TransferMoneyViewProps : Props {
  var presenter: Presenter
}

class TransferMoneyViewState(var recipientName: String = "", var recipientAccountIdentifier: String = "", var recipientBankIdentifier: String? = null,
                             var reference: String = "", var amount: String = "", var instantPayment: Boolean = false) : State


@JsExport
class TransferMoneyView(props: TransferMoneyViewProps) : RComponent<TransferMoneyViewProps, TransferMoneyViewState>(props) {

  override fun RBuilder.render() {
    div {
      TextInputField {
        attrs {
          label = "Recipient name"
          valueChanged = { newValue -> setState { recipientName = newValue } }
        }
      }

      TextInputField {
        attrs {
          label = "IBAN"
          valueChanged = { newValue -> setState { recipientAccountIdentifier = newValue } }
        }
      }

      TextInputField {
        attrs {
          label = "Amount"
          valueChanged = { newValue -> setState { amount = newValue } }
        }
      }

      TextInputField {
        attrs {
          label = "Reference"
          valueChanged = { newValue -> setState { reference = newValue } }
        }
      }


      button {
        span { +"Transfer" }
        attrs {
          onMouseUp = {
            props.presenter.transferMoney(state.recipientName, state.recipientAccountIdentifier, state.recipientBankIdentifier, state.reference, state.amount, state.instantPayment) { } }
        }
      }
    }
  }

}