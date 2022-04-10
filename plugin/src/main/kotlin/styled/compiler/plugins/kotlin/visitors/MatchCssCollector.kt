@file:Suppress("CanBeVal")

package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import styled.compiler.plugins.kotlin.*

class MatchCssCollector(private val className: String) : IrElementVisitor<Unit, StringBuilder> {
    private fun collectCss(block: () -> String): String? {
        return try {
            block()
        } catch (e: Throwable) {
            e.stackTraceToString().writeLog()
            null
        }
    }

    override fun visitCall(expression: IrCall, data: StringBuilder) {
        if (expression.symbol.owner.isInCssLib()) {
            val value = when (val arg = expression.getValueArgument(0)) {
                is IrCall -> (arg.extensionReceiver as IrConst<*>).value
                is IrConstructorCall -> (arg.getValueArgument(0) as IrConst<*>).value
                else -> throw IllegalArgumentException("Illegal declaration")
            }
            when (expression.name.replacePropertyAccessor()) {
                "padding" -> data.append("padding: ${value}px;\n")
                "color" -> data.append("color: $value;\n")
                "backgroundColor" -> data.append("background-color: $value;\n")
            }
        }
    }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }
}