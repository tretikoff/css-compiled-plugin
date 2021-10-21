package styled.compiler.plugins.kotlin.visitors

import kotlinx.css.toCustomProperty
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.asString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isGetter
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
import repro.deepcopy.generation.getConstValues
import repro.deepcopy.generation.normalize
import repro.deepcopy.generation.replacePropertyAccessor
import repro.deepcopy.generation.writeDump
import styled.compiler.plugins.kotlin.isToColorProperty

// TODO get computable values like (8 * 8).px
// We can check if return is LinearDimension
//        function.body?.let { body ->
//        body.statements.forEach {
//            builder.appendLine(it.dump())
//        }
//     }

// TODO get string builder values
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
            }
        }
        val declaration = when (val normalizedName = name.normalize()) {
            "rgb" -> "rgb(${values.joinToString(", ")})"
            else -> {
                "${values.firstOrNull() ?: ""}$normalizedName"
            }
        }
        return declaration
    }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        when (element) {
            is IrGetEnumValueImpl -> data.append(element.symbol.owner.name.asString().normalize())
            is IrConstructorCallImpl -> {
                val str = element.getArgumentsWithIr()
                    .map { it.second }
                    .filterIsInstanceMapNotNull<IrConstImpl<*>, String?> { it.value?.toString() }
                    .firstOrNull() // TODO somehow get non-const values
                if (str != null) {
                    data.append(str.normalize())
                } else {
                    element.acceptChildren(this, data)
                }
            }
            is IrConst<*> -> {
                data.append(element.value)
            }
            is IrCall -> {
                val declaration = element.runtimeDeclaration()
                if (declaration.isEmpty()) {
                    element.acceptChildren(this, data)
                } else {
                    data.append(declaration)
                }
            }
            is IrGetValue -> {
                val varName = element.symbol.owner.name.asString()
                val value = GlobalVariablesVisitor.varValues[varName]
                if (value != null) {
                    data.append(value)
                } else {
                    element.acceptChildren(this, data)
                }
            }
            else -> element.acceptChildren(this, data)
            /**
             *  FUN_EXPR type=@[ExtensionFunctionType] kotlin.Function1<kotlinx.css.CssBuilder, kotlin.Unit> origin=LAMBDA
            FUN LOCAL_FUNCTION_FOR_LAMBDA name:<anonymous> visibility:local modality:FINAL <> ($receiver:kotlinx.css.CssBuilder) returnType:kotlin.Unit
            $receiver: VALUE_PARAMETER name:$this$adjacentSibling type:kotlinx.css.CssBuilder
            BLOCK_BODY
            CALL 'public final fun <set-paddingTop> (<set-?>: kotlinx.css.LinearDimension): kotlin.Unit declared in kotlinx.css' type=kotlin.Unit origin=EQ
            $receiver: GET_VAR '$this$adjacentSibling: kotlinx.css.CssBuilder declared in <root>.Welcome.render.<anonymous>.<anonymous>.<anonymous>' type=kotlinx.css.CssBuilder origin=null
            <set-?>: CALL 'public final fun <get-px> (): kotlinx.css.LinearDimension declared in kotlinx.css' type=kotlinx.css.LinearDimension origin=GET_PROPERTY
            $receiver: CONST Int type=kotlin.Int value=0
             */
        }
    }
}