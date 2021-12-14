package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.getPrimitiveType
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.statements
import styled.compiler.plugins.kotlin.exceptions.ValueExtractionException
import styled.compiler.plugins.kotlin.name
import styled.compiler.plugins.kotlin.writeLog
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject

fun IrElement.extractDirectValues(): Array<Any?> {
    val values = mutableListOf<Any?>()
    accept(PropertyVisitor(values), StringBuilder())
    return values.toTypedArray()
}

fun IrElement.extractValues(): Array<Any?> {
    val values = mutableListOf<Any?>()
    acceptChildren(PropertyVisitor(values), StringBuilder())
    return values.toTypedArray()
}

fun IrFile.classForExtensions(): Class<*> {
    val clazzName = name.replace(".kt", "Kt")
    val packageName = fqName.asString()
    val fullName = if (packageName.isEmpty()) clazzName else "$packageName.$clazzName"
//    try {
    return Class.forName(fullName)
//    } catch (e: Throwable) {
//        val url = URL(path)
//        val loader = URLClassLoader(arrayOf(url))
//        return loader.loadClass(fullName)
//    }
}


fun PrimitiveType?.getClazz(): KClass<*> {
    return when (this) {
        PrimitiveType.BOOLEAN -> Boolean::class
        PrimitiveType.CHAR -> Char::class
        PrimitiveType.BYTE -> Byte::class
        PrimitiveType.SHORT -> Short::class
        PrimitiveType.INT -> Int::class
        PrimitiveType.FLOAT -> Float::class
        PrimitiveType.LONG -> Long::class
        PrimitiveType.DOUBLE -> Double::class
        null -> throw ValueExtractionException("Unknown primitive type")
    }
}

fun IrType.isCompanionCall(): Boolean {
    return classFqName?.asString()?.contains(".Companion") ?: false
}

fun IrType.getClazz(): KClass<*>? {
    val clazzName = classFqName?.asString()?.replace(".Companion", "")
    return clazzName?.let {
        if (isPrimitiveType()) {
            return getPrimitiveType().getClazz()
        }
        return Class.forName(clazzName).kotlin
    }
}

class PropertyVisitor(private val values: MutableList<Any?>) : AbstractTreeVisitor<StringBuilder>() {
    override fun visitCall(expression: IrCall, data: StringBuilder) = withCall(expression) {
        super.visitCall(expression, data)
        val subValues = expression.extractValues()
        expression.dispatchReceiver?.let { it ->
            val clazz = it.type.getClazz() ?: return@withCall
            val value = if (it.type.isCompanionCall()) {
                val companion =
                    clazz.companionObject ?: throw ValueExtractionException("No companion on class $clazz")
                companion.java.invokeMethod(companion.objectInstance, expression.name, *subValues)
            } else {
                clazz.java.invokeMethod(subValues[0]!!, expression.name, *(subValues.copyOfRange(1, subValues.size)))
            }
            values.add(value)
            return@withCall
        }
        try {
            expression.executeExtension(subValues)
        } catch (e: ClassNotFoundException) {
            expression.symbol.owner.dump().writeLog()
            val extReceiverValue = expression.extensionReceiver?.extractDirectValues()?.firstOrNull() ?: return@withCall
            currentFrame.withVariable("<this>", extReceiverValue) {
                expression.symbol.owner.body?.let { body ->
                    val bvalues = body.extractDirectValues()
                    values.add(bvalues.single())
                }
            }
        }
    }

    override fun visitBody(body: IrBody, data: StringBuilder) {
        super.visitBody(body, data)
        body.statements.forEach {
            if (it is IrReturn) {
                values.add(it.extractValues().single())
            } else {
                throw ValueExtractionException("Cannot interpret long statement bodies")
            }
        }
    }

    private fun IrCall.executeExtension(subValues: Array<Any?>) {
        val extensionClass = symbol.owner.fileOrNull?.classForExtensions()
        extensionReceiver?.let { it ->
            val extValues = it.extractDirectValues()
            try {
                val value = extensionClass?.invokeMethod(null, name, *extValues)
                values.add(value)
            } catch (e: Throwable) {
                e.stackTraceToString().writeLog()
            }
            return
        }
        extensionClass?.let { clazz ->
            val value = clazz.invokeMethod(null, name, *subValues)
            values.add(value)
            return
        }
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: StringBuilder) {
        super.visitConstructorCall(expression, data)
        val clazzName = expression.type.classFqName?.asString()
        val clazz = Class.forName(clazzName)
        val subvalues = expression.extractValues()
        val constructor = clazz.getConstructor(*subvalues.map { it!!::class.java }.toTypedArray())
        val value = constructor.newInstance(*subvalues)
        values.add(value)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue, data: StringBuilder) {
        super.visitGetEnumValue(expression, data)
        val clazz = Class.forName(expression.type.classFqName?.asString())
        val enumValue = clazz.getMethod("valueOf", String::class.java).invoke(null, expression.name)
        values.add(enumValue)
    }

    override fun <T> visitConst(expression: IrConst<T>, data: StringBuilder) {
        super.visitConst(expression, data)
        values.add(expression.value)
    }

    override fun visitGetValue(expression: IrGetValue, data: StringBuilder) {
        super.visitGetValue(expression, data)
        val name = expression.symbol.owner.name.asString()
        "Get value $name ${expression.dump()}".writeLog()
        currentFrame.getVariable(name)?.let {
            values.add(it)
        }
    }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }
}