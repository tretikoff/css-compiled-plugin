package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import repro.deepcopy.generation.writeDump
import styled.compiler.plugins.kotlin.isSetCustomProperty
import styled.compiler.plugins.kotlin.packageStr

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class GlobalVariablesVisitor(private val prefix: String = "") : IrElementVisitor<Unit, StringBuilder> {
    companion object {
        val varValues = mutableMapOf<String, String>()
        val cssVarValues = mutableMapOf<String, String>()
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
                val pack = element.packageStr
                val name = element.name.asString()
                val builder = StringBuilder()
                element.acceptChildren(PropertyVisitor(), builder)
                varValues["$pack$prefix.$name"] = builder.toString()
            }
            is IrClass -> {
                // TODO support prefixes for regular variables - something like package.Class1.Class2.(...).funName1.(...).varName
                val name = element.name.asStringStripSpecialMarkers()
                element.acceptChildren(GlobalVariablesVisitor("$prefix.$name"), data)
            }
            else -> {
                element.acceptChildren(this, data)
            }
        }
    }
}
