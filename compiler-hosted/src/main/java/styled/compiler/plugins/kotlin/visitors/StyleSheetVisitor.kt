package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
import repro.deepcopy.generation.Parameter

class StyleSheetVisitor(private var name: String) : IrElementVisitor<Unit, StringBuilder> {
    private var arguments = mapOf<String, Any?>()
    override fun visitElement(element: IrElement, data: StringBuilder) {
        when (element) {
            // TODO check somehow that the constructor is always before other statements
            is IrDelegatingConstructorCall -> {
                arguments = element.getArgumentsWithIr()
                    // Skipping non-constant values as don't know how to evaluate them right now
                    .filterIsInstanceMapNotNull<Pair<IrValueParameter, IrConstImpl<*>>, Parameter> { (parameter, expr) ->
                        parameter.name.asString() to expr.value
                    }.toMap().also { name = it["name"]?.toString() ?: name }
            }
            is IrProperty -> {
                val propName = element.name.asString()
                arguments.let {
                    if (it.containsKey(propName)) return
                }
                data.appendLine("$name-$propName {")
                element.acceptChildren(this, data)
                data.appendLine("}")
            }
            is IrCall -> {
                val name = element.symbol.descriptor.name.asString()
                if (name == "css") {
                    element.acceptChildren(CssVisitor(), data);
                } else {
                    element.acceptChildren(this, data);
                }
            }
            else -> {
                element.acceptChildren(this, data)
            }
        }
    }
}