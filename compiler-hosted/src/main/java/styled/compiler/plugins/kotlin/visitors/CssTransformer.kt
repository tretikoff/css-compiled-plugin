package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import repro.deepcopy.generation.normalize

/** TODO getClassSelector {it::propertyName}
 * adjacentSibling(CommonMenuStyles.getClassSelector { it::menuSubtitle }) {
 *   paddingTop = 0.px
 * }
 */

/** TODO value.unit -> to value.px */
class CssTransformer : IrElementTransformer<StringBuilder> {
    override fun visitCall(expression: IrCall, data: StringBuilder): IrElement {
        val name = expression.symbol.descriptor.name.asString()
        if (name == "unaryPlus") {
            return expression.transform(StyleSheetTransformer(), data)
        } else {
            data.append(name.normalize(), ":")
            expression.acceptChildren(PropertyVisitor(), data)
            data.appendLine(";")
        }
        return expression
    }

    override fun visitElement(element: IrElement, data: StringBuilder): IrElement {
        element.transformChildren(this, data)
        return element
    }
}
