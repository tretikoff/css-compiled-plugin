import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.Props
import react.RBuilder
import react.RComponent
import react.State
import react.dom.attrs
import styled.*

external interface WelcomeProps : Props {
    var name: String
}

data class WelcomeState(val name: String) : State

@JsExport
class Welcome(props: WelcomeProps) : RComponent<WelcomeProps, WelcomeState>(props) {
    init {
        state = WelcomeState(props.name)
    }

    override fun RBuilder.render() {
//        val listItemActiveBackgroundColor = Color("#e0eeff")
        styledDiv {
            css {
                +"some-external-classname"
                +WelcomeStyles.textContainer
                backgroundColor = Color("#111111")
//                backgroundColor = listItemActiveBackgroundColor
//                color = textColor
                marginLeft = 3.px
//                borderColor = XTheme.theme.skeletonBackgroundColor
            }
            +"Hello, ${state.name}"
        }
        styledInput {
            css {
                +"#${WelcomeStyles.getClassName { it::textProperty }}"
                +WelcomeStyles.getClassSelector { it::textInput }
//                adjacentSibling(WelcomeStyles.getClassSelector { it::textInput }) {
//                    paddingTop = 0.px
//                }
                alignItems = Align.flexEnd
            }
            attrs {
                type = InputType.text
                value = state.name
                onChangeFunction = { event ->
                    setState(
                        WelcomeState(name = (event.target as HTMLInputElement).value)
                    )
                }
            }
        }
    }
}

