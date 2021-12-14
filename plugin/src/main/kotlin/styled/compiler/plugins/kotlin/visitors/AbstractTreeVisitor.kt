package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import kotlin.collections.set


class StackFrame {
    val variables = mutableMapOf<String, Any?>()
    private val previous: StackFrame? = null

    fun getVariable(name: String): Any? {
        return variables[name] ?: previous?.getVariable(name)
    }

    fun withVariable(name: String, value: Any?, block: () -> Unit) {
        variables[name] = value
        block()
        variables.remove(name)
    }
}


abstract class AbstractTreeVisitor<T> : IrElementVisitor<Unit, T> {
    val currentFrame: StackFrame = StackFrame()
    protected fun withCall(expression: IrCall, block: () -> Unit) {

    }
    override fun visitCall(expression: IrCall, data: T) {
        super.visitCall(expression, data)
    }

    override fun visitVariable(declaration: IrVariable, data: T) {
        super.visitVariable(declaration, data)
    }


    override fun visitBody(body: IrBody, data: T) {
        super.visitBody(body, data)
    }

    override fun visitElement(element: IrElement, data: T) {
        element.acceptChildren(this, data)
    }
}