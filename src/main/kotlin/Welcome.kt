import kotlinx.css.Align
import kotlinx.css.alignItems
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.attrs
import styled.css
import styled.styledDiv
import styled.styledInput

external interface WelcomeRProps : RProps {
    var name: String
}

data class WelcomeRState(val name: String) : RState

@JsExport
class Welcome(props: WelcomeRProps) : RComponent<WelcomeRProps, WelcomeRState>(props) {
    init {
        state = WelcomeRState(props.name)
    }

    override fun RBuilder.render() {
        styledDiv {
            css {
                +"some-external-classname"
                +WelcomeStyles.textContainer
            }
            +"Hello, ${state.name}"
        }
        styledInput {
            css {
                +WelcomeStyles.textInput
                alignItems = Align.flexEnd
            }
            attrs {
                type = InputType.text
                value = state.name
                onChangeFunction = { event ->
                    setState(
                        WelcomeRState(name = (event.target as HTMLInputElement).value)
                    )
                }
            }
        }
    }
}

