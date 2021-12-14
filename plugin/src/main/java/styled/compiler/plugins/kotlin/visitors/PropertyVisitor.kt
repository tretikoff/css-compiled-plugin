package styled.compiler.plugins.kotlin.visitors

import kotlinx.css.Align
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import styled.compiler.plugins.kotlin.name
import styled.compiler.plugins.kotlin.normalizeGetSet
import styled.compiler.plugins.kotlin.writeLog
import kotlin.reflect.full.companionObject

// TODO get computable values like (8 * 8).px
// We can check if return is LinearDimension
//        function.body?.let { body ->
//        body.statements.forEach {
//            builder.appendLine(it.dump())
//        }
//     }

// TODO get string builder values

fun Array<Any?>.types(): Array<Class<*>> {
    return map { it!!::class.java }.toTypedArray()
}

fun IrExpression.extractDirectValues(): Array<Any?> {
    val values = mutableListOf<Any?>()
    accept(PropertyVisitor(values), StringBuilder())
    return values.toTypedArray()
}

fun IrExpression.extractValues(): Array<Any?> {
    val values = mutableListOf<Any?>()
    acceptChildren(PropertyVisitor(values), StringBuilder())
    return values.toTypedArray()
}

fun IrFile.classForExtensions(): Class<*> {
    val name = "${fqName.asString()}.${name.replace(".kt", "Kt")}"
    return Class.forName(name)
}

fun String.isCompanion() = endsWith(".Companion")

class PropertyVisitor(private val values: MutableList<Any?>) : IrElementVisitor<Unit, StringBuilder> {
    override fun visitCall(expression: IrCall, data: StringBuilder) {
        "Call ${expression.dump()} with receiver ${expression.dispatchReceiver?.dump()} and ${expression.extensionReceiver?.dump()}".writeLog()
        val subValues = expression.extractValues()
        expression.dispatchReceiver?.let { it ->
            if (!it.type.isPrimitiveType()) {
                val clazzName = it.type.classFqName?.asString()
                clazzName?.let {
                    if (clazzName.isCompanion()) {
                        val clazz = Class.forName(clazzName.replace(".Companion", "")).kotlin.companionObject!!.java

                        val name = expression.name.normalizeGetSet()
                        val value = clazz.methods.firstOrNull { it.name == name }?.invoke(null, *subValues)
                        values.add(value)
                    }
                }
            }
            return
        }
        expression.extensionReceiver?.let { it ->
            val extValues = it.extractDirectValues()
            try {
                val clazz = expression.symbol.owner.fileOrNull?.classForExtensions()
                val method = clazz?.methods?.firstOrNull { it.name == expression.name.normalizeGetSet() }
                val value = method?.invoke(null, *extValues)
                values.add(value)
            } catch (e: Throwable) {
                e.stackTraceToString().writeLog()
            }

//                extensionClass?.methods?.forEach { it.name.writeLog() }
//                val clazz = if (it.type.isPrimitiveType()) {
//                    it.type.getPrimitiveType()?.getClass()
//                } else {
//                    it.type.classOrNull?.owner?.fileOrNull?.classForExtensions()
//                }
//                if (clazz != null && expression.name.isGetter()) {
//                    clazz.methods.forEach { it.name.writeLog() }
//                }
            return
        }
        val extensionClass = expression.symbol.owner.fileOrNull?.classForExtensions()
        extensionClass?.let { clazz ->
            val value = clazz.methods.first { it.name == expression.name }.invoke(null, *subValues)
            values.add(value)
            return
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: StringBuilder) {
        val clazzName = expression.type.classFqName?.asString()
        val clazz = Class.forName(clazzName)
        val subvalues = expression.extractValues()
        val constructor = clazz.getConstructor(*subvalues.map { it!!::class.java }.toTypedArray())
        val value = constructor.newInstance(*subvalues)
        values.add(value)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: StringBuilder) {
        val clazz = Class.forName(expression.type.classFqName?.asString())
        Align.valueOf(expression.name)
        val enumValue = clazz.getMethod("valueOf", String::class.java).invoke(null, expression.name)
        values.add(enumValue)
    }

    override fun <T> visitConst(expression: IrConst<T>, data: StringBuilder) {
        values.add(expression.value)
        "Visited const $values".writeLog()
    }

    override fun visitGetValue(expression: IrGetValue, data: StringBuilder) {
        "Get value ${expression.dump()}".writeLog()
//        val varName = expression.symbol.owner.name.asString()
//        val value = GlobalVariablesVisitor.varValues[varName]
//        if (value != null) {
//            data.append(value)
//        } else {
//            expression.acceptChildren(this, data)
//        }
    }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }
}