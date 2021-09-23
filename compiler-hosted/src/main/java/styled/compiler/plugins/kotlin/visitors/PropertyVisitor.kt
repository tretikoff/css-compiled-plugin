package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetEnumValueImpl
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
import repro.deepcopy.generation.builder
import repro.deepcopy.generation.getConstValues
import repro.deepcopy.generation.normalize

class PropertyVisitor : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        when (element) {
            is IrGetEnumValueImpl -> builder.append(element.symbol.descriptor.name.asString().normalize())
            is IrConstructorCallImpl -> {
                val str = element.getArgumentsWithIr()
                    .map { it.second }
                    .filterIsInstanceMapNotNull<IrConstImpl<*>, String?> { it.value?.toString() }
                    .first() // TODO somehow get non-const values
                builder.append(str?.normalize())
            }
            is IrGetValue -> {
                // child is IrCall
                element.acceptChildren(this, null)
            }
            is IrCall -> {
                val values = element.getConstValues()
                val name = element.symbol.descriptor.name.asString().normalize()
                val declaration = when (name) {
                    "rgb" -> "rgb(${values.joinToString(", ")})"
                    else -> " ${values.firstOrNull() ?: ""}$name"
                }
                builder.append(declaration)
            }
            else -> {
//                    builder.appendLine(element.javaClass)
//                    builder.appendLine(element.dump())
            }
        }
    }
}