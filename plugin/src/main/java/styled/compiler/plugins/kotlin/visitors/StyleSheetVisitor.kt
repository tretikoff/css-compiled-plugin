package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
import styled.compiler.plugins.kotlin.Parameter
import styled.compiler.plugins.kotlin.createStyleSheetClassname
import styled.compiler.plugins.kotlin.isCssCall
import styled.compiler.plugins.kotlin.name

class StyleSheetVisitor(private var name: String) : IrElementVisitor<Unit, StringBuilder> {
    private var arguments = mapOf<String, Any?>()
    override fun visitElement(element: IrElement, data: StringBuilder) {
        when (element) {
            is IrDelegatingConstructorCall -> {
                arguments = // Skipping non-constant values as don't know how to evaluate them right now
                    element.getArgumentsWithIr()
                        // Skipping non-constant values as don't know how to evaluate them right now
                        .mapNotNull { (parameter, expr) ->
                            (expr as? IrConst<*>)?.let {e ->
                                parameter.name.asString() to e.value
                            }
                        }.toMap().also { name = it["name"]?.toString() ?: name }
            }
            is IrProperty -> {
                val propName = element.name.asString()
                arguments.let {
                    if (it.containsKey(propName)) return
                }
                data.appendLine(".${createStyleSheetClassname(name, propName)} {")
                element.acceptChildren(this, data)
                data.appendLine("}")
            }
            is IrCall -> {
                if (element.name == "css") {
                    element.transformChildren(CssTransformer(), data)
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