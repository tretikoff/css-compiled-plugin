package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import styled.compiler.plugins.kotlin.isCssCall
import styled.compiler.plugins.kotlin.isStyleSheet

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
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
        when (element) {
            is IrCall -> if (element.isCssCall()) {
                val className = generatedClassName
                data.appendLine(".$className {")
                element.transform(CssTransformer(className), data)
                data.appendLine("}")
            }
            is IrClass -> if (element.isStyleSheet()) {
                element.acceptChildren(StyleSheetVisitor(element.name.asString()), data)
                return
            }
        }
        element.acceptChildren(this, data);
    }
}
