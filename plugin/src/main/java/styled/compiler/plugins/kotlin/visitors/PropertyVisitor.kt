package styled.compiler.plugins.kotlin.visitors

import kotlinx.css.toCustomProperty
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
import repro.deepcopy.generation.getConstValues
import repro.deepcopy.generation.normalize
import repro.deepcopy.generation.replacePropertyAccessor
import repro.deepcopy.generation.writeDump
import styled.compiler.plugins.kotlin.isInCssLib
import styled.compiler.plugins.kotlin.isToColorProperty

// TODO get computable values like (8 * 8).px
// We can check if return is LinearDimension
//        function.body?.let { body ->
//        body.statements.forEach {
//            builder.appendLine(it.dump())
//        }
//     }

// TODO get string builder values

/**
 * FUN name:<get-unit> visibility:public modality:FINAL <> ($receiver:kotlin.Int) returnType:kotlinx.css.LinearDimension
correspondingProperty: PROPERTY name:unit visibility:public modality:FINAL [val]
$receiver: VALUE_PARAMETER name:<this> type:kotlin.Int
BLOCK_BODY
RETURN type=kotlin.Nothing from='public final fun <get-unit> (): kotlinx.css.LinearDimension declared in <root>'
CALL 'public final fun <get-px> (): kotlinx.css.LinearDimension declared in kotlinx.css' type=kotlinx.css.LinearDimension origin=GET_PROPERTY
$receiver: CALL 'public final fun times (other: kotlin.Int): kotlin.Int [operator] declared in kotlin.Int' type=kotlin.Int origin=MUL
$this: GET_VAR '<this>: kotlin.Int declared in <root>.<get-unit>' type=kotlin.Int origin=null
other: CONST Int type=kotlin.Int value=8
 */
class PropertyVisitor : IrElementVisitor<Unit, StringBuilder> {
    private fun IrCall.runtimeDeclaration(): String {
        val values = getConstValues()
        if (isToColorProperty()) {
            extensionReceiver?.let { it ->
                val builder = StringBuilder()
                it.accept(this@PropertyVisitor, builder)
                return builder.toString().toCustomProperty()
            }
        }
        val function: IrFunction = symbol.owner
        val receiver = function.dispatchReceiverParameter
        val name = function.name.asString().replacePropertyAccessor()

        // get variable value from global variables
        if (function.isGetter) {
            if (receiver != null) {
                val key = "${receiver.type.asString()}.${name}"
                val varValue = GlobalVariablesVisitor.varValues[key]
                if (varValue != null) return varValue
            } else if (!function.isInCssLib()) {
                // handle extension function call
                val rec = extensionReceiver
                if (rec is IrConst<*>) {
                    val builder = StringBuilder()
                    function.accept(SimpleExpressionVisitor(rec.value), builder)
                    if (builder.isNotEmpty()) return builder.toString().also {
                        ">>>>$it".writeDump()
                    }
                }
            }
        }
        val declaration = when (val normalizedName = name.normalize()) {
            "rgb" -> "rgb(${values.joinToString(", ")})"
            else -> "${values.firstOrNull() ?: ""}$normalizedName" // TODO
        }
        return declaration
    }

    override fun visitCall(expression: IrCall, data: StringBuilder) {
        val declaration = expression.runtimeDeclaration()
        if (declaration.isEmpty()) {
            expression.acceptChildren(this, data)
        } else {
            data.append(" $declaration")
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: StringBuilder) {
        val str = expression.getArgumentsWithIr()
            .map { it.second }
            .filterIsInstanceMapNotNull<IrConstImpl<*>, String?> { it.value?.toString() }
            .firstOrNull() // TODO somehow get non-const values
        if (str != null) {
            data.append(str.normalize())
        } else {
            expression.acceptChildren(this, data)
        }
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: StringBuilder) {
        data.append(expression.symbol.owner.name.asString().normalize())
    }

    override fun <T> visitConst(expression: IrConst<T>, data: StringBuilder) {
        data.append(expression.value)
    }

    override fun visitGetValue(expression: IrGetValue, data: StringBuilder) {
        val varName = expression.symbol.owner.name.asString()
        val value = GlobalVariablesVisitor.varValues[varName]
        if (value != null) {
            data.append(value)
        } else {
            expression.acceptChildren(this, data)
        }
    }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }
}