package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import styled.compiler.plugins.kotlin.isCssCall
import styled.compiler.plugins.kotlin.isSetCustomProperty
import styled.compiler.plugins.kotlin.isStyleSheet
import java.util.concurrent.atomic.AtomicInteger

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class TreeVisitor : AbstractTreeVisitor<StringBuilder>() {
    private var classNameId = AtomicInteger(0)
    private val generatedClassName: String
        get() = "ksc-static-${classNameId.incrementAndGet()}"


    override fun visitCall(expression: IrCall, data: StringBuilder) {
        super.visitCall(expression, data)
        if (expression.isCssCall()) {
            expression.transform(CssTransformer(generatedClassName), data)
        } else if (expression.isSetCustomProperty()) {
            val name = expression.getValueArgument(0)
            val value = expression.getValueArgument(1)
            try {
                if (name != null && value != null) {
                    val extractedName = name.extractValues().single()
                    val extractedValue = value.extractValues().single()
                    GlobalVariablesVisitor.cssVarValues[extractedName.toString()] = extractedValue.toString()
                }
            } catch (e: NoSuchElementException) {
                // TODO
            }
        }
    }

    override fun visitClass(declaration: IrClass, data: StringBuilder) {
        super.visitClass(declaration, data)
        if (declaration.isStyleSheet()) {
            declaration.acceptChildren(StyleSheetVisitor(declaration.name.asString()), data)
        }
    }

    override fun visitElement(element: IrElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }
}
