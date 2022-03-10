package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import styled.compiler.plugins.kotlin.createStyleSheetClassname
import styled.compiler.plugins.kotlin.name
import styled.compiler.plugins.kotlin.tryLog

// TODO unaryPlus for stylesheet inside another stylesheet
// TODO every file to different css file
// TODO save css file paths which were traversed
class StyleSheetVisitor(private var name: String) : IrElementVisitor<Unit, StringBuilder> {
    var className = name
    private var arguments = mapOf<String, Any?>()
    override fun visitElement(element: IrElement, data: StringBuilder) {
        when (element) {
            is IrDelegatingConstructorCall -> {
                arguments = element.getArgumentsWithIr()
                    // Skipping non-constant values as don't know how to evaluate them right now
                    .mapNotNull { (parameter, expr) ->
                        (expr as? IrConst<*>)?.let { e ->
                            parameter.name.asString() to e.value
                        }
                    }.toMap().also { name = it["name"]?.toString() ?: name }
            }
            is IrProperty -> {
                val propName = element.name.asString()
                arguments.let {
                    if (it.containsKey(propName)) return
                }
                className = createStyleSheetClassname(name, propName)
                element.acceptChildren(this, data)
            }
            is IrCall -> {
                if (element.name == "css") {
                    tryLog("Style sheet css traverse _______________") {
                        val css = StringBuilder()
                        element.accept(CssCollector(className), css)
                        data.append(css)
                    }
//                    element.transform(CssTransformer(className, isStylesheet = true), null)
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

