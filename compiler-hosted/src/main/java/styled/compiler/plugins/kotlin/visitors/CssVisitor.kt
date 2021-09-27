package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import repro.deepcopy.generation.normalize

class CssVisitor : IrElementVisitor<Unit, StringBuilder> {
    override fun visitElement(element: IrElement, data: StringBuilder) {
        if (element is IrCallImpl) { // TODO actually check if it is set property
            val name = element.symbol.descriptor.name.asString()
            if (name == "unaryPlus") {
//                element.transformChildren(StyleSheetTransformer(), data)
            } else {
                data.append(name.normalize(), ":")
                element.acceptChildren(PropertyVisitor(), data)
                data.appendLine(";")
            }
            return
        } else {
            element.acceptChildren(this, data)
        }
    }
}
