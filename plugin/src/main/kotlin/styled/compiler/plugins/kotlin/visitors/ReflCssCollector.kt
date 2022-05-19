@file:Suppress("CanBeVal")

package styled.compiler.plugins.kotlin.visitors

import kotlinx.css.CssBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import styled.compiler.plugins.kotlin.*

enum class CssRuleType { BLOCK, ATOMIC }
typealias CssInfo = ArrayList<CssBuilder>

class ReflCssCollector(private val ruleType: CssRuleType = CssRuleType.BLOCK) : IrElementVisitor<Unit, CssInfo> {
    override fun visitCall(expression: IrCall, data: CssInfo) {
        val owner = expression.symbol.owner
        if (expression.isCssCall() || expression.name == "css") {
            expression.acceptChildren(this, data)
        } else if (owner.isInCssLib() && !owner.isPlus()) {
            val classes = listOf(Class.forName("kotlinx.css.StyledElementKt"), Class.forName("kotlinx.css.CssBuilder"))
            val values = expression.extractValues()
            val cssBuilder = when (ruleType) {
                CssRuleType.BLOCK -> data.firstOrNull() ?: CssBuilder("  ").also { data.add(it) }
                CssRuleType.ATOMIC -> CssBuilder("  ").also { data.add(it) }
            }
            tryLog("") {
                val clazz = classes.firstOrNull { it.containsMethod(expression.name.normalizeGetSet()) } ?: return
                if (expression.extensionReceiver != null) {
                    clazz.invokeMethod(null, expression.name, cssBuilder, *values)
                } else {
                    clazz.invokeMethod(null, expression.name, *values)
                }
            }
        } else {
//            "$$$${owner.packageStr} ${owner.name} ${owner.dump()}".writeLog()
        }
    }

    private fun Class<*>.containsMethod(name: String): Boolean {
        for (method in methods) {
            if (method.name == name) return true
        }
        return false
    }

    override fun visitElement(element: IrElement, data: CssInfo) {
        element.acceptChildren(this, data)
    }
}