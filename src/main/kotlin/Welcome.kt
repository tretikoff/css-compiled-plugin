
import react.Props
import kotlinx.css.*
import react.RBuilder
import react.RComponent
import react.State
import runtime.html.*
import styled.css
import styled.styledDiv

@JsExport
class Welcome(props: Props) : RComponent<Props, State>(props) {
    override fun RBuilder.render() {
        styledDiv {
            css {
                backgroundColor = Color.aquamarine
                height = 30.px
            }
            +"Hello, dsl"
        }
    }
}