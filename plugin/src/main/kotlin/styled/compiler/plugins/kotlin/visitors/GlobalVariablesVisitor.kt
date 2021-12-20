package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import styled.compiler.plugins.kotlin.packageStr

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class GlobalVariablesVisitor(private val prefix: String = "") : IrElementVisitorVoid {
    companion object {
        val varValues = mutableMapOf<String, String>()
        val cssVarValues = mutableMapOf<String, String>()
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrVariable -> {
                val name = element.name.asString()
                val builder = StringBuilder()
//                element.acceptChildren(PropertyVisitor(), builder)
                varValues[name] = builder.toString()
            }
            is IrField -> {
                val pack = element.packageStr
                val name = element.name.asString()
                val builder = StringBuilder()
//                element.acceptChildren(PropertyVisitor(), builder)
                varValues["$pack$prefix.$name"] = builder.toString()
            }
            is IrClass -> {
                val name = element.name.asStringStripSpecialMarkers()
                element.acceptChildrenVoid(GlobalVariablesVisitor("$prefix.$name"))
            }
            else -> {
                element.acceptChildrenVoid(this)
            }
        }
    }
}
