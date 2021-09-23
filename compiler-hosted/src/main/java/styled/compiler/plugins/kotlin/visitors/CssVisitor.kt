package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import repro.deepcopy.generation.builder
import repro.deepcopy.generation.normalize

class CssVisitor : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        if (element is IrCallImpl) { // TODO actually check if it is set property
            val name = element.symbol.descriptor.name.asString()
            if (name == "unaryPlus") {
                builder.appendLine(element.dump())
                return // TODO if stylesheet - change to unaryPlus(string)
            } else {
                builder.append(name.normalize(), ": ")
                element.acceptChildren(PropertyVisitor(), null)
                builder.appendLine(";")
            }
            return
        }
        element.acceptChildren(this, null)
    }
}
