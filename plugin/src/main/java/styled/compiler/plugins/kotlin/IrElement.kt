package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.getAllSuperclasses

fun IrCall.isCssCall(): Boolean {
    return symbol.signature?.render()?.startsWith("styled/css") ?: false
}

fun IrCall.isSetCustomProperty(): Boolean {
    return symbol.owner.name.asString() == "setCustomProperty"
}

fun IrCall.isToColorProperty(): Boolean {
    return symbol.owner.name.asString() == "toColorProperty"
}

fun IrClass.isStyleSheet(): Boolean {
    return getAllSuperclasses().find { it.name.asString() == "StyleSheet" } != null
}

fun IrFunction.isPlus(): Boolean {
    return name.asString() == "unaryPlus"
}

fun IrSimpleFunction.isGetter(): Boolean {
    return name.asString().startsWith("<get")
}

fun IrSimpleFunction.isPropertyGetter(): Boolean {
    val propertyAttributes = arrayOf("getClassName", "getClassSelector")
    return propertyAttributes.any { it == name.asString() }
}