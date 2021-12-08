package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import styled.compiler.plugins.kotlin.*

/**
 * Inserts classname instead of adding and injecting stylesheet
 *
 * Example
 * CALL 'public final fun unaryPlus (): kotlin.Unit [operator] declared in kotlinx.css.CSSBuilder' type=kotlin.Unit origin=UPLUS
 *      $this: GET_VAR '$this$css: kotlinx.css.CSSBuilder declared in <root>.Welcome.render.<anonymous>.<anonymous>' type=kotlinx.css.CSSBuilder origin=null
 *      $receiver: CALL 'public final fun <get-textContainer> (): @[ExtensionFunctionType] kotlin.Function1<kotlinx.css.CSSBuilder, kotlin.Unit>{ kotlinx.css.RuleSet } declared in <root>.WelcomeStyles' type=@[ExtensionFunctionType] kotlin.Function1<kotlinx.css.CSSBuilder, kotlin.Unit>{ kotlinx.css.RuleSet } origin=GET_PROPERTY
 *          $this: GET_OBJECT 'CLASS OBJECT name:WelcomeStyles modality:FINAL visibility:public superTypes:[styled.StyleSheet]' type=<root>.WelcomeStyles
 *
 * ---> gets transformed into --->
 * CALL 'public final fun unaryPlus (): kotlin.Unit [operator] declared in kotlinx.css.CSSBuilder' type=kotlin.Unit origin=UPLUS
 *      $this: GET_VAR '$this$css: kotlinx.css.CSSBuilder declared in <root>.Welcome.render.<anonymous>.<anonymous>' type=kotlinx.css.CSSBuilder origin=null
 *      $receiver: CONST String type=kotlin.String value="generated-classname"
 *
 * CALL 'public final fun <get-textContainer> (): @[ExtensionFunctionType] kotlin.Function1<kotlinx.css.CSSBuilder, kotlin.Unit>{ kotlinx.css.RuleSet } declared in <root>.WelcomeStyles' type=@[ExtensionFunctionType] kotlin.Function1<kotlinx.css.CSSBuilder, kotlin.Unit>{ kotlinx.css.RuleSet } origin=GET_PROPERTY
$this: CONST String type=kotlin.String value="WelcomeStyles-textContainer"

 */
//val IrDeclarationParent.isCssBuilder
//    get() = fqNameForIrSerialization.asString() == "kotlinx.css.CssBuilder"

val IrDeclarationContainer.addClassFun: IrSimpleFunction?
    get() = functionDecls.firstOrNull(IrFunction::isAddClassFun)?.symbol?.owner as? IrSimpleFunction

val IrFunction.isAddClassFun
    get() = isPlus()
            && allParameters.size == 2
            && allParameters.lastOrNull()?.type == context.irBuiltIns.stringType

val IrDeclarationContainer.functionDecls: List<IrFunction>
    get() = declarations.filterIsInstance<IrFunction>()

//val IrFunction.parentsNames: String
//    get() = parents.joinToString { it.fqNameForIrSerialization.asString() }

// name.asString() == "addClass"

fun IrFunction.getConstCssFunction() = (parent as? IrDeclarationContainer)?.addClassFun

// TODO get function names from kotlin-css from runtime to check function is library call
class StyleSheetTransformer : IrElementTransformer<StringBuilder> {
    private var className: String = ""
    private var name: String = ""
    override fun visitCall(expression: IrCall, data: StringBuilder): IrElement {
        expression.transformChildren(this, data)
        var updatedCall = expression
        val callee = expression.symbol.owner
        val cssFun = callee.getConstCssFunction()
        if (expression.symbol.owner.isGetter()) {
            className = createStyleSheetClassname(name, expression.name.replacePropertyAccessor())
        }
        if (callee.isPlus() && cssFun != null) {
            updatedCall = expression.transformWith(cssFun, className)
        }
        return updatedCall
    }

    override fun visitPropertyReference(expression: IrPropertyReference, data: StringBuilder): IrElement {
        className = createStyleSheetClassname(name, expression.symbol.owner.name.asString())
        return expression
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: StringBuilder): IrExpression {
        name = expression.symbol.owner.name.asString()
        return expression
    }
}