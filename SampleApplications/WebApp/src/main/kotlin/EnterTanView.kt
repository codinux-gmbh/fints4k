import io.ktor.util.encodeBase64
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import kotlinx.html.style
import net.codinux.banking.fints.model.ImageTanChallenge
import net.codinux.banking.fints.model.TanChallenge
import org.w3c.dom.HTMLInputElement
import react.Props
import react.RBuilder
import react.RComponent
import react.State
import react.dom.*

external interface EnterTanViewProps : Props {
  var presenter: Presenter
  var tanChallenge: TanChallenge
}

data class EnterTanViewState(val enteredTan: String? = null) : State

@JsExport
class EnterTanView(props: EnterTanViewProps) : RComponent<EnterTanViewProps, EnterTanViewState>(props) {

  override fun RBuilder.render() {
    p {
      +"Enter TAN:"
    }

    if (props.tanChallenge is ImageTanChallenge) {
      val tanImage = (props.tanChallenge as ImageTanChallenge).image
      if (tanImage.decodingSuccessful) {
        val base64Encoded = tanImage.imageBytes.encodeBase64()

        img(src = "data:${tanImage.mimeType};base64, $base64Encoded") { }
      }
    }

    p {
      props.tanChallenge.messageToShowToUser
    }

    div {
      span { +"TAN:" }

      input {
        attrs {
          type = InputType.text
          onChangeFunction = { event ->
            val enteredTan = (event.target as HTMLInputElement).value

            setState(EnterTanViewState(enteredTan))
          }
        }
      }

      button {
        span { +"Done" }
        attrs {
          onMouseUp = {
            state.enteredTan?.let {
              props.tanChallenge.userEnteredTan(it)
            } ?: run { props.tanChallenge.userDidNotEnterTan() }
          }
        }
      }
    }
  }

}