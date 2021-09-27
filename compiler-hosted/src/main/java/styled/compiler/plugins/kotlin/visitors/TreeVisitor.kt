package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssVisitor] to them
 */
class TreeVisitor : IrElementVisitor<Unit, StringBuilder> {
    private var incrementedClassName: Int = 0
        get() {
            return field.also { field++ }
        }
    private val generatedClassName: String
        get() {
            return "ksc-static-$incrementedClassName"
        }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        if (element is IrCall) {
            element.symbol.signature?.let { sig ->
                // TODO maybe somehow could fetch and compare called function name
                if (sig.render().startsWith("styled/css")) {
                    val className = generatedClassName
                    data.appendLine(".$className {")
                    element.acceptChildren(CssVisitor(), data);
                    data.appendLine("}")
                }
            }
        } else if (element is IrClass && element.getAllSuperclasses()
                .find { it.name.asString() == "StyleSheet" } != null
        ) {
            element.acceptChildren(StyleSheetVisitor(element.name.asString()), data)
            return
        }
        element.acceptChildren(this, data);
    }
}
