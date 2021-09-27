package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import repro.deepcopy.generation.replacePropertyAccessor

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
 */
class StyleSheetTransformer : IrElementTransformer<StringBuilder> {
    private var className: String = ""
    private var name: String = ""
    override fun visitCall(expression: IrCall, data: StringBuilder): IrElement {
        data.appendLine(expression.dump())
        expression.transformChildren(this, data)
        val descName = expression.symbol.descriptor.name.asString()
        if (expression.symbol.descriptor.name.asString().startsWith("<get")) {
            className = "${name}_${descName.replacePropertyAccessor()}"
        }
//        expression.putValueArgument(0, irString())
        return expression
//        expression.dispatchReceiver?.let {
//            // TODO not it.type but kotlin.String
//            val receiver = IrConstImpl(it.startOffset, it.endOffset, fragment.irBuiltins.stringType, IrConstKind.String, className)
//            expression.dispatchReceiver = receiver
//        }
//        return expression
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: StringBuilder): IrExpression {
        name = expression.symbol.descriptor.name.asString()
        data.appendLine(expression.dump())
        return expression
    }
}