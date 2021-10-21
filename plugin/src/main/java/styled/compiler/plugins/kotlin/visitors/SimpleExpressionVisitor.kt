package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrErrorExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import repro.deepcopy.generation.normalize
import repro.deepcopy.generation.writeDump
import styled.compiler.plugins.kotlin.isGetter
import styled.compiler.plugins.kotlin.isInCssLib
import styled.compiler.plugins.kotlin.isMultiply
import styled.compiler.plugins.kotlin.name

// TODO get computable values more generically
class SimpleExpressionVisitor(private val argument: Any?) : IrElementVisitor<Unit, StringBuilder> {
    override fun visitFunction(declaration: IrFunction, data: StringBuilder) {
        declaration.body?.let { body ->
            body.statements.forEach {
                if (it !is IrErrorExpression) {
                    it.dump().writeDump()
                }
                if (it is IrReturn) {
                    return it.acceptChildren(this, data)
                }
            }
        }
    }

    override fun visitCall(expression: IrCall, data: StringBuilder) {
        if (expression.isMultiply()) {
            expression.getValueArgument(0)?.dump()?.writeDump()
            expression.getValueArgument(1)?.dump()?.writeDump()
        } else {
            expression.dispatchReceiver?.acceptChildren(this, data)
            expression.extensionReceiver?.acceptChildren(this, data)
        }
        if (expression.symbol.owner.isGetter() && expression.symbol.owner.isInCssLib()) {
            data.append(expression.name.normalize())
        }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: StringBuilder) {
        val a1 = argument as? Int
        val a2 = expression.value as? Int
        if (a1 != null && a2 != null) {
            data.append(a1 * a2)
        }
    }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }
}