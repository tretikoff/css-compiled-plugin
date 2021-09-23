package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import repro.deepcopy.generation.builder
import repro.deepcopy.generation.classnameBuilder

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssVisitor] to them
 */
class TreeVisitor : IrElementVisitorVoid {
    private var incrementedClassName: Int = 0
        get() {
            return field.also { field++ }
        }
    private val generatedClassName: String
        get() {
            return "ksc-static-$incrementedClassName"
        }

    override fun visitElement(element: IrElement) {
        if (element is IrCall) {
            element.symbol.signature?.let { sig ->
                // TODO maybe somehow could fetch and compare called function name
                if (sig.render().startsWith("styled/css")) {
                    val className = generatedClassName
                    classnameBuilder.add(className)
                    builder.appendLine(".$className {")
                    element.acceptChildren(CssVisitor(), null);
                    builder.appendLine("}")
                }
            }
        } else if (element is IrClass && element.getAllSuperclasses()
                .find { it.name.asString() == "StyleSheet" } != null
        ) {
            // TODO process element
            element.acceptChildren(StyleSheetVisitor(element.name.asString()), null)
            return
        }
        element.acceptChildren(this, null);
    }
}
