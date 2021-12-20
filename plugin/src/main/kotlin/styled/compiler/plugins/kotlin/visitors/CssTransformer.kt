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
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import styled.compiler.plugins.kotlin.*
import kotlin.reflect.full.safeCast

var cssBuilderParameter: IrValueParameter? = null
var css = CssBuilder()

fun Class<*>.invokeMethod(instance: Any?, name: String, vararg values: Any?): Any? {
    val normalizedName = name.normalizeGetSet()
    try {
        val method = methods.first { m ->
            m.name == normalizedName &&
                    m.parameterCount == values.size &&
                    m.parameters.zip(values).all { (param, value) ->
                        if (value == null) true else param.type.kotlin.safeCast(value) != null
                    }
        }
        return method.invoke(instance, *values)
    } catch (e: NoSuchElementException) {
        "Method not found: $name with values ${values.joinToString()}".writeLog()
        throw e
    }
}

class CssTransformer(private val className: String = "", private val isStylesheet: Boolean = false) :
    IrElementTransformer<StringBuilder> {
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
            val classes = listOf(Class.forName("kotlinx.css.StyledElementKt"), Class.forName("kotlinx.css.CssBuilder"))
            // TODO custom common-code CssBuilder extensions
            // TODO extensions with blocks and ampersands
            val values = expression.extractValues()
            try {
                val clazz = classes.firstOrNull { it.containsMethod(expression.name.normalizeGetSet()) } ?: return updatedCall
                if (expression.extensionReceiver != null) {
                    clazz.invokeMethod(null, expression.name, css, *values)
                } else {
                    clazz.invokeMethod(null, expression.name, *values)
                }
            } catch (e: Throwable) {
                e.stackTraceToString().writeLog()
            }
            cssFun?.let { css ->
                when (mode) {
                    Mode.FULL -> updatedCall = expression.transformWith(css, className)
                    Mode.STYLESHEET_STATIC -> if (isStylesheet) {
                        updatedCall = expression.transformWith(css, className)
                    }
                }
            }
        } else {
//            "$$$${owner.packageStr} ${owner.name} ${owner.dump()}".writeLog()
        }
        return updatedCall
    }

    private fun Class<*>.containsMethod(name: String): Boolean {
        for (method in methods) {
            if (method.name == name) return true
        }
        return false
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