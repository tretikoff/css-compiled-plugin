package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import repro.deepcopy.generation.context
import repro.deepcopy.generation.createStyleSheetClassname
import repro.deepcopy.generation.fragment
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
 *
 * CALL 'public final fun <get-textContainer> (): @[ExtensionFunctionType] kotlin.Function1<kotlinx.css.CSSBuilder, kotlin.Unit>{ kotlinx.css.RuleSet } declared in <root>.WelcomeStyles' type=@[ExtensionFunctionType] kotlin.Function1<kotlinx.css.CSSBuilder, kotlin.Unit>{ kotlinx.css.RuleSet } origin=GET_PROPERTY
$this: CONST String type=kotlin.String value="WelcomeStyles-textContainer"

 */

class DumpVisitor : IrElementVisitor<Unit, StringBuilder> {
    override fun visitElement(element: IrElement, data: StringBuilder) {
        data.appendLine(element.dump())
    }
}

class StyleSheetTransformer : IrElementTransformer<StringBuilder> {
    private var className: String = ""
    private var name: String = ""
    override fun visitCall(expression: IrCall, data: StringBuilder): IrElement {
//        data.appendLine(">>>" + expression.attributeOwnerId.dump())
        expression.transformChildren(this, data)
        val descName = expression.symbol.descriptor.name.asString()
        if (expression.symbol.descriptor.name.asString().startsWith("<get")) {
            className = createStyleSheetClassname(name, descName.replacePropertyAccessor())
        }
//        expression.dispatchReceiver?.let {
//            if (it is IrGetValue) {
//                data.appendLine(it.dump())
//                IrCallImpl()
//            expression.dispatchReceiver =
//                context.referenceFunctions(FqName("kotlin.time.TimeSource.markNow"))
//                    .single()
//            }
//        }
//        expression.extensionReceiver?.let {
//            if (it is IrCall) {
//                expression.extensionReceiver = IrConstImpl(
//                    it.startOffset, it.endOffset, fragment.irBuiltins.stringType, IrConstKind.String, className
//                )
//            }
//        }
        return expression
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: StringBuilder): IrExpression {
        name = expression.symbol.descriptor.name.asString()
        return expression
//        return IrConstImpl(expression.startOffset, expression.endOffset, fragment.irBuiltins.stringType, IrConstKind.String, className)
    }
}