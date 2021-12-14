package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.getPackageFragment

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