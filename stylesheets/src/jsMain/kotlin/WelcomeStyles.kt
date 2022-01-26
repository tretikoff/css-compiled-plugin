package runtime.html

import kotlinx.css.*
import styled.StyleSheet

object WelcomeStyles : StyleSheet("WelcomeStyles", isStatic = true) {
    val textContainer by css {
        padding(5.px)
        grow(Grow.GROW_SHRINK)

        backgroundColor = rgb(8, 97, 22)
        color = rgb(56, 246, 137)
    }

    val textInput by css {
        margin(vertical = 5.px)
        padding(15.px, 18.px)
        fontWeight = FontWeight.w500
        fontSize = 2.unit
    }
    val textProperty by css {
        alignContent = Align.end
    }
}

open class FontBase(name: String = "name") : StyleSheet(name) {
    open val container by css {
        position = Position.absolute
        top = 0.px
        bottom = 0.px
    }
}

object Fonts : FontBase() {  // TODO
    override val container by css {
        +super.container

        specific {
            top = 5.px
            bottom = 10.px
        }
    }
}

val Int.unit get() = (this * 8).px