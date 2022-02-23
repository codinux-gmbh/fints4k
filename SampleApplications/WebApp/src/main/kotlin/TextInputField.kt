import react.*
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.input
import react.dom.html.ReactHTML.span

external interface TextInputFieldProps : Props {
  var label: String
  var valueChanged: (String) -> Unit
}


val TextInputField = FC<TextInputFieldProps> { props ->

  div {
    span { +"${props.label}: " }

    input {
      type = react.dom.html.InputType.text
      onChange = { event ->
        val enteredValue = event.target.value

        props.valueChanged(enteredValue)}
    }
  }

}