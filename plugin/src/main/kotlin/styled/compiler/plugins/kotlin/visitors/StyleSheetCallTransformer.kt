package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import styled.compiler.plugins.kotlin.*

val IrDeclarationContainer.addClassFun: IrSimpleFunction?
    get() = functionDecls.firstOrNull(IrFunction::isAddClassFun)?.symbol?.owner as? IrSimpleFunction

val IrFunction.isAddClassFun
    get() = isPlus()
            && allParameters.size == 2
            && allParameters.lastOrNull()?.type == context.irBuiltIns.stringType

val IrDeclarationContainer.functionDecls: List<IrFunction>
    get() = declarations.filterIsInstance<IrFunction>()

fun IrFunction.getConstCssFunction() = (parent as? IrDeclarationContainer)?.addClassFun

/**
 * Inserts classname instead of adding and injecting stylesheet
 */
class StyleSheetCallTransformer : IrElementTransformerVoid() {
    private var className: String = ""
    private var name: String = ""

    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        var updatedCall = expression
        val callee = expression.symbol.owner
        val cssFun = callee.getConstCssFunction()
        if (callee.isGetter()) {
            className = createStyleSheetClassname(name, expression.name.replacePropertyAccessor())
        }
        if (callee.isPlus() && cssFun != null) {
            "transforming $className call".writeLog()
            updatedCall = expression.transformWith(cssFun, className)
        }
        return updatedCall
    }

    override fun visitPropertyReference(expression: IrPropertyReference): IrExpression {
        className = createStyleSheetClassname(name, expression.symbol.owner.name.asString())
        return expression
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        name = expression.symbol.owner.name.asString()
        return expression
    }
}