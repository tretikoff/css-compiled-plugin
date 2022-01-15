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

object Fonts : StyleSheet("Fonts", WelcomeStyles)

val Int.unit get() = (this * 8).px