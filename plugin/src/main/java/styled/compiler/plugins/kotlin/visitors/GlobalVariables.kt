package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class GlobalVariables(private val prefix: String = "") : IrElementVisitor<Unit, StringBuilder> {
    companion object {
        val varValues = mutableMapOf<String, String>()
    }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        when (element) {
            is IrVariable -> {
                val name = element.name.asString()
                val builder = StringBuilder()
                element.acceptChildren(PropertyVisitor(), builder)
                varValues[name] = builder.toString()
            }
            is IrField -> {
                val name = element.name.asString()
                val builder = StringBuilder()
                element.acceptChildren(PropertyVisitor(), builder)
                varValues["$prefix.$name"] = builder.toString()
            }
            is IrClass -> {
                if (element.isObject || element.isCompanion) {
                    // if element is object we store prefix to differentiate variables from different objects.
                    // TODO support prefixes for regular variables
                    val name = "${element.packageFqName}.${element.name}"
                    element.acceptChildren(GlobalVariables("$prefix.$name"), data)
                } else {
                    element.acceptChildren(this, data)
                }
            }
            else -> {
                element.acceptChildren(this, data)
            }
        }
    }
}
