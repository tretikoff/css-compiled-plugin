@file:Suppress("CanBeVal")

package styled.compiler.plugins.kotlin.visitors

import kotlinx.css.CssBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import styled.compiler.plugins.kotlin.*

class CssCollector(private val className: String) : IrElementVisitor<Unit, StringBuilder> {
    private lateinit var css: CssBuilder

    private fun collectCss(block: () -> Unit): String? {
        css = CssBuilder("  ")
        return try {
            block()
            css.toString()
        } catch (e: Throwable) {
            e.stackTraceToString().writeLog()
            null
        }
    }

    override fun visitCall(expression: IrCall, data: StringBuilder) {
        val owner = expression.symbol.owner
        if (expression.isCssCall() || expression.name == "css") {
            val str = collectCss {
                expression.acceptChildren(this, data)
            }
            str?.let {
                data.appendLine(".$className {").append(str).appendLine("}")
            }
        } else if (owner.isInCssLib()) {
            // https://stackoverflow.com/questions/48635210/how-to-obtain-properties-or-function-declared-in-kotlin-extensions-by-java-refle
            val classes = listOf(Class.forName("kotlinx.css.StyledElementKt"), Class.forName("kotlinx.css.CssBuilder"))
            // TODO custom common-code CssBuilder extensions
            // TODO extensions with blocks and ampersands
            val values = expression.extractValues()
            try {
                val clazz = classes.firstOrNull { it.containsMethod(expression.name.normalizeGetSet()) } ?: return
                if (expression.extensionReceiver != null) {
                    clazz.invokeMethod(null, expression.name, css, *values)
                } else {
                    clazz.invokeMethod(null, expression.name, *values)
                }
            } catch (e: Throwable) {
                e.stackTraceToString().writeLog()
            }
        } else {
//            "$$$${owner.packageStr} ${owner.name} ${owner.dump()}".writeLog()
        }
    }

    private fun Class<*>.containsMethod(name: String): Boolean {
        for (method in methods) {
            if (method.name == name) return true
        }
        return false
    }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }
}