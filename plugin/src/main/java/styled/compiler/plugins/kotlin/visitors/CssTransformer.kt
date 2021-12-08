@file:Suppress("CanBeVal")

package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import styled.compiler.plugins.kotlin.*

/** TODO value.unit -> to value.px */

var cssFun: IrSimpleFunction? = null
var cssBuilderParameter: IrValueParameter? = null

class CssTransformer(val className: String = "", val indent: String = "  ") : IrElementTransformer<StringBuilder> {
    override fun visitCall(expression: IrCall, data: StringBuilder): IrElement {
        val owner = expression.symbol.owner
        var updatedCall = expression
        if (owner.isPlus() && cssFun != null) {
            return expression.transform(StyleSheetTransformer(), data)
        } else if (expression.isCssCall()) {
            cssBuilderParameter = expression.getArgumentsWithIr().mapNotNull { (_, expr) ->
                (expr as? IrFunctionExpressionImpl)?.function?.extensionReceiverParameter
            }.firstOrNull()
            cssFun = cssBuilderParameter?.type?.classOrNull?.owner?.addClassFun
            expression.transformChildren(this, data)
            cssFun = null
        } else if (owner.isInCssLib()) {
            data.appendDecl(expression)
            cssFun?.let { cssFun ->
                updatedCall = expression.transformWith(cssFun, className)
            }
        }
        return updatedCall
    }

    private fun StringBuilder.appendDecl(declCall: IrCall) {
        val name = declCall.name.normalize()
        val propBuilder = StringBuilder()
        declCall.acceptChildren(PropertyVisitor(), propBuilder)
        appendLine("$indent$name:$propBuilder;")
    }

    override fun visitElement(element: IrElement, data: StringBuilder): IrElement {
        element.transformChildren(this, data)
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
    val receiver = dispatchReceiver ?: (
            IrGetValueImpl(0, 0, cssBuilderParameter!!.type, cssBuilderParameter!!.symbol))
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