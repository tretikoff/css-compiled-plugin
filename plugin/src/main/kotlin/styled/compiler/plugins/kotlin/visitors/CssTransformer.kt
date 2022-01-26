@file:Suppress("CanBeVal")

package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import styled.compiler.plugins.kotlin.*

private var cssBuilderParameter: IrValueParameter? = null

class CssTransformer(private val className: String, private val isStylesheet: Boolean = false) : IrElementTransformerVoid() {
    private fun IrCall.acceptWithParameter() {
        try {
            cssBuilderParameter = getArgumentsWithIr().firstNotNullOfOrNull { (_, expr) ->
                (expr as? IrFunctionExpressionImpl)?.function?.extensionReceiverParameter
            }
            transformChildrenVoid(this@CssTransformer)
            cssBuilderParameter = null
        } catch (e: Exception) {
            e.stackTraceToString().writeLog()
        }
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val owner = expression.symbol.owner
        var updatedCall = expression
        val cssFun = cssBuilderParameter?.type?.classOrNull?.owner?.addClassFun
        if (owner.isPlus() && cssFun != null) {
            return expression.transform(StyleSheetCallTransformer(), null)
        } else if (expression.isCssCall() || expression.name == "css") {
            expression.acceptWithParameter()
        } else if (owner.isInCssLib() && cssFun != null) {
            when (mode) {
                Mode.FULL -> updatedCall = expression.transformWith(cssFun, className)
                Mode.STYLESHEET_STATIC -> if (isStylesheet) {
                    updatedCall = expression.transformWith(cssFun, className)
                }
            }
        }
        return updatedCall
    }

    override fun visitElement(element: IrElement): IrElement {
        element.transformChildrenVoid(this)
        return element
    }
}

fun IrCall.transformWith(cssFun: IrSimpleFunction, className: String): IrCall {
    val updatedCall = IrCallImpl(
        symbol = cssFun.symbol,
        startOffset = startOffset,
        endOffset = endOffset,
        type = type,
        typeArgumentsCount = 0,
        valueArgumentsCount = 0,
    )
    val receiver =
        dispatchReceiver ?: IrGetValueImpl(0, 0, cssBuilderParameter!!.type, cssBuilderParameter!!.symbol)
    updatedCall.dispatchReceiver = receiver
    updatedCall.extensionReceiver = IrConstImpl(
        extensionReceiver?.startOffset ?: 0,
        extensionReceiver?.endOffset ?: 0,
        fragment.irBuiltins.stringType,
        IrConstKind.String,
        className
    )
    return updatedCall
}
