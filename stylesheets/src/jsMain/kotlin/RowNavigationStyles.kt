package runtime.html

import kotlinx.css.*
import styled.StyleSheet

open class RowNavigationStyles(_name: String = "RowNavigationStyles") : StyleSheet(_name) {
    private val contentDefault by css {
        alignSelf = Align.stretch
        alignItems = Align.center

        width = 100.pct
    }

    open val content: RuleSet
        get() = contentDefault
}

const val name = ""
fun sidebarMenuItemStyles(): RowNavigationStyles = object : RowNavigationStyles("Sidebar.Menu.${name}") {
    override val content: RuleSet by css {
        +super.content
    }
}

object red: StyleSheet("") {
    val article by css {
        color = Color.red
    }
}

// TODO no generating when supertypes are not present
object X : RowNavigationStyles("Sidebar.Menu.${name}") {
    override val content: RuleSet by css {
        +super.content
    }
}