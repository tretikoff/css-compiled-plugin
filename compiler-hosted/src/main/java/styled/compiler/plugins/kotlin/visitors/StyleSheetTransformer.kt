package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import repro.deepcopy.generation.normalize

/**
 * Inserts classname instead of adding and injecting stylesheet
 */
class StyleSheetInsertionTransformer : IrElementTransformerVoid() {
    private var className: String = ""
    private var name: String = ""
    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildren(this, null)
        val descName = expression.symbol.descriptor.name.asString()
        if (expression.symbol.descriptor.name.asString().startsWith("<get")) {
            className = "$name-${descName.normalize()}"
        }
        expression.dispatchReceiver?.let {
            // TODO not it.type but kotlin.String
            val receiver = IrConstImpl(it.startOffset, it.endOffset, it.type, IrConstKind.String, className)
            expression.dispatchReceiver = receiver
        }
        return expression
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        name = expression.symbol.descriptor.name.asString()
        return expression
    }
}