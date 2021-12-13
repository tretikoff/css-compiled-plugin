package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import styled.compiler.plugins.kotlin.isCssCall
import styled.compiler.plugins.kotlin.isSetCustomProperty
import styled.compiler.plugins.kotlin.isStyleSheet
import java.util.concurrent.atomic.AtomicInteger

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class TreeVisitor : IrElementVisitor<Unit, StringBuilder> {
    private var classNameId = AtomicInteger(0)
    private val generatedClassName: String
        get() = "ksc-static-${classNameId.incrementAndGet()}"

    override fun visitElement(element: IrElement, data: StringBuilder) {
        when (element) {
            is IrCall -> if (element.isCssCall()) {
                element.transform(CssTransformer(generatedClassName), data)
            } else if (element.isSetCustomProperty()) {
                /**
                 * $$$$$$STRING_CONCATENATION type=kotlin.String
                CONST String type=kotlin.String value="skeleton-background-color"
                GET_VAR 'suffix: kotlin.String declared in framework.x.XTheme.Companion.setDark' type=kotlin.String origin=null

                $$$$$$CALL 'public open fun <get-skeletonBackgroundColorRaw> (): kotlinx.css.Color declared in framework.x.XTheme.DarkTheme' type=kotlinx.css.Color origin=GET_PROPERTY
                $this: CALL 'public final fun <get-dark> (): framework.x.XTheme.DarkTheme declared in framework.x.XTheme.Companion' type=framework.x.XTheme.DarkTheme origin=GET_PROPERTY
                $this: GET_VAR '<this>: framework.x.XTheme.Companion declared in framework.x.XTheme.Companion.setDark' type=framework.x.XTheme.Companion origin=null
                 */
                /** private fun CssBuilder.setDefault() {
                 *      setCustomProperty("skeleton-background-color", theme.skeletonBackgroundColorRaw)
                 *  }*/
                /**
                 * override val skeletonBackgroundColorRaw = CommonTheme.textColor.withAlpha(0.05)
                 */
                val name = element.getValueArgument(0)
                val value = element.getValueArgument(1)
                if (name != null && value != null) {
                    val nameBuilder = StringBuilder()
                    name.accept(PropertyVisitor(), nameBuilder)
                    val valueBuilder = StringBuilder()
                    value.accept(PropertyVisitor(), valueBuilder)
                    GlobalVariablesVisitor.cssVarValues[nameBuilder.toString()] = valueBuilder.toString()
                }
            }
            is IrClass -> if (element.isStyleSheet()) {
                element.acceptChildren(StyleSheetVisitor(element.name.asString()), data)
                return
            }
        }
        element.acceptChildren(this, data);
    }
}
