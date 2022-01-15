package styled.compiler.plugins.kotlin

import kotlinx.css.hyphenize
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
import java.util.*
import kotlin.reflect.full.safeCast

fun IrCall.isCssCall(): Boolean {
    return symbol.signature?.render()?.startsWith("styled/css") ?: false
}

val IrCall.name: String
    get() = symbol.owner.name.asString()

val IrGetEnumValue.name: String
    get() = symbol.owner.name.asString()

val IrElement.packageStr: String
    get() = getPackageFragment()?.fqName?.asString() ?: ""

fun IrElement.isInCssLib() = packageStr == "kotlinx.css"

fun IrCall.isSetCustomProperty(): Boolean {
    return name == "setCustomProperty"
}

fun IrCall.isToColorProperty(): Boolean {
    return name == "toColorProperty"
}

fun IrCall.isMultiply(): Boolean {
    return name == "times";
}

fun IrClass.isStyleSheet(): Boolean {
    return getAllSuperclasses().find { it.name.asString() == "StyleSheet" } != null
}

fun IrFunction.isPlus() = name.asString() == "unaryPlus"

fun IrFunction.isGetter() = name.asString().isGetter()
fun String.isGetter() = startsWith("<get")
fun IrCall.isSetter() = name.isSetter()
fun String.isSetter() = startsWith("<set")

fun IrFunction.isPropertyGetter(): Boolean {
    val propertyAttributes = arrayOf("getClassName", "getClassSelector")
    return propertyAttributes.any { it == name.asString() }
}

fun IrCall.getConstValues(): Collection<String?> {
    return this.getArgumentsWithIr()
        .map { it.second }
        .filterIsInstanceMapNotNull<IrConstImpl<*>, String?> { it.value?.toString() }
}

fun String.replacePropertyAccessor(): String {
    return this.replace("<get-", "").replace("<set-", "").replace(">", "")
}

fun String.capitalize() =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.toCamelCase() =
    split('-').joinToString("", transform = String::capitalize).replaceFirstChar { it.lowercase() }

fun String.normalizeGetSet() = replace("<", "").replace(">", "").toCamelCase()


fun String.normalize(): String {
    return this.replacePropertyAccessor().hyphenize()
}

fun createStyleSheetClassname(name: String, propertyName: String): String {
    return "$name-$propertyName"
}

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
