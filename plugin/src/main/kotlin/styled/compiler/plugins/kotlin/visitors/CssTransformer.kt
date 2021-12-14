@file:Suppress("CanBeVal")

package styled.compiler.plugins.kotlin.visitors

import kotlinx.css.CssBuilder
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
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import styled.compiler.plugins.kotlin.*
import styled.compiler.plugins.kotlin.exceptions.ValueExtractionException
import kotlin.reflect.KClass
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.safeCast

/** TODO value.unit -> to value.px */

var cssBuilderParameter: IrValueParameter? = null
var css = CssBuilder()

fun Class<*>.invokeMethod(instance: Any?, name: String, vararg values: Any?): Any? {
    when (name) { // TODO maybe move .unit to css library
        "times" -> if (instance is Int) return instance.times(values.first() as Int)
        "div" -> if (instance is Int) return instance.div(values.first() as Int)
    }
    val normalizedName = name.normalizeGetSet()
    val method = methods.first { m ->
        m.name == normalizedName &&
                m.parameterCount == values.size &&
                m.parameters.zip(values).all { (param, value) ->
                    if (value == null) true else param.type.kotlin.safeCast(value) != null
                }
    }
    return method.invoke(instance, *values)
}

class CssTransformer(private val className: String = "") : IrElementTransformer<StringBuilder> {
    private fun collectCss(block: () -> Unit): String? {
        css = CssBuilder("  ")
        return try {
            block()
            css.toString()
        } catch (e: Throwable) {
            e.stackTraceToString().writeLog()
            null
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
        } else if (expression.isCssCall() || expression.name == "css") {
            cssBuilderParameter = expression.getArgumentsWithIr().mapNotNull { (_, expr) ->
                (expr as? IrFunctionExpressionImpl)?.function?.extensionReceiverParameter
            }.firstOrNull()
            val str = collectCss {
                expression.transformChildren(this, data)
            }
            str?.let {
                data.appendLine(".$className {").append(str).appendLine("}")
            }
        } else if (owner.isInCssLib()) {
            // https://stackoverflow.com/questions/48635210/how-to-obtain-properties-or-function-declared-in-kotlin-extensions-by-java-refle
            val styledElementClass = Class.forName("kotlinx.css.StyledElementKt")
            val values = expression.extractValues()
            try {
                if (expression.extensionReceiver != null) {
                    styledElementClass.invokeMethod(null, expression.name, css, *values)
                } else {
                    styledElementClass.invokeMethod(null, expression.name, *values)
                }
            } catch (e: Throwable) {
                e.stackTraceToString().writeLog()
            }
            cssFun?.let { css ->
                updatedCall = expression.transformWith(css, className)
            }
        } else {
//            "$$$${owner.packageStr} ${owner.name} ${owner.dump()}".writeLog()
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