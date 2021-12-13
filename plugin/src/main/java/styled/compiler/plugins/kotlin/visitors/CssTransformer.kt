@file:Suppress("CanBeVal")

package styled.compiler.plugins.kotlin.visitors

import kotlinx.css.CssBuilder
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
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import styled.compiler.plugins.kotlin.*

/** TODO value.unit -> to value.px */
/** TODO function evaluation */

var cssBuilderParameter: IrValueParameter? = null
var css = CssBuilder()

fun IrExpression.extractValues(): Array<Any> {
    val values: List<Any> = listOf()
    acceptChildren(PropertyVisitor(values), java.lang.StringBuilder())
    return values.toTypedArray()
}

class CssTransformer(val className: String = "") : IrElementTransformer<StringBuilder> {
    private fun collectCss(block: () -> Unit): String {
        css = CssBuilder()
        return try {
            block()
            css.toString()
        } catch (e: Throwable) {
            e.stackTraceToString().writeLog()
            ""
        } finally {
            cssBuilderParameter = null
        }
    }

    override fun visitCall(expression: IrCall, data: StringBuilder): IrElement {
        val owner = expression.symbol.owner
        var updatedCall = expression
        val cssFun = cssBuilderParameter?.type?.classOrNull?.owner?.addClassFun
        if (owner.isPlus() && cssFun != null) {
            return expression.transform(StyleSheetTransformer(), data)
        } else if (expression.isCssCall()) {
            cssBuilderParameter = expression.getArgumentsWithIr().mapNotNull { (_, expr) ->
//                (expr as? IrFunctionExpressionImpl)?.let {
//                    IrInterpreter(fragment).interpret(it).dump().writeLog()
//                }
                (expr as? IrFunctionExpressionImpl)?.function?.extensionReceiverParameter
            }.firstOrNull()
            val str = collectCss {
                expression.transformChildren(this, data)
            }
            data.append(str)

        } else if (owner.isInCssLib()) {
            if (expression.isSetter()) {
                expression.dump().writeLog()
                val name = expression.name.replacePropertyAccessor()
                // https://stackoverflow.com/questions/48635210/how-to-obtain-properties-or-function-declared-in-kotlin-extensions-by-java-refle
                val c = Class.forName("kotlinx.css.StyledElementKt")
                val values = expression.extractValues()
                c.methods.first {
                    it.name == "set${name.capitalize()}"
                }.invoke(null, css, *values)
            }
            cssFun?.let { css ->
                updatedCall = expression.transformWith(css, className)
            }
        }
        return updatedCall
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