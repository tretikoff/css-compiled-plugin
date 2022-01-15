import kotlinx.css.*
import kotlinx.html.InputType
import kotlinx.html.js.onChangeFunction
import org.w3c.dom.HTMLInputElement
import react.Props
import react.RBuilder
import react.RComponent
import react.State
import react.dom.attrs
import runtime.html.WelcomeStyles
import styled.*

external interface WelcomeProps : Props {
    var name: String
}

data class WelcomeState(val name: String) : State

@JsExport
class Welcome(props: WelcomeProps) : RComponent<WelcomeProps, WelcomeState>(props) {
    private val propColor = Color("#222222")
    init {
        state = WelcomeState(props.name)
    }

    override fun RBuilder.render() {
        var varColor = Color("#e0eeff")
        styledDiv {
            varColor = Color("#ffffff")
            css {
                +"some-external-classname"
                +WelcomeStyles.textContainer
                backgroundColor = varColor
                color = propColor
                marginLeft = 3.px
//                borderColor = XTheme.theme.skeletonBackgroundColor
            }
            +"Hello, ${state.name}"
        }
        styledInput {
            css {
                +"#${WelcomeStyles.getClassName { it::textProperty }}"
                +WelcomeStyles.getClassSelector { it::textInput }
//                adjacentSibling("div") {
//                    paddingTop = 0.px
//                }
//                adjacentSibling(WelcomeStyles.getClassSelector { it::textInput }) {
//                    paddingTop = 1.px
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

