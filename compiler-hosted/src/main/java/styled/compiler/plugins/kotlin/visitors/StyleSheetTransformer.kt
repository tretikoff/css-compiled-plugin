package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
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

fun Name.isPlus(): Boolean {
    return asString() == "unaryPlus"
}

fun Name.isGetter(): Boolean {
    return asString().startsWith("<get")
}

fun IrFunction.getConstCssFunction(): IrFunctionSymbol {
    val function = (parent as? IrDeclarationContainer)?.declarations
        ?.filterIsInstance<IrFunction>()
        ?.firstOrNull { it.name.isPlus() && it.allParameters.size == 2 && it.allParameters.last().type == fragment.irBuiltins.stringType }
    return function?.symbol ?: throw IllegalStateException("Couldn't find constant css function implementation")
}

// TODO transform function
class StyleSheetTransformer : IrElementTransformer<StringBuilder> {
    private var className: String = ""
    private var name: String = ""
    override fun visitCall(expression: IrCall, data: StringBuilder): IrElement {
        expression.transformChildren(this, data)
        var updatedCall = expression
        if (expression.symbol.descriptor.name.isPlus()) {
            val callee = expression.symbol.owner
            val actualFunction = callee.getConstCssFunction().owner as IrSimpleFunction
            updatedCall = IrCallImpl(
                symbol = actualFunction.symbol,
                origin = expression.origin,
                startOffset = expression.startOffset,
                endOffset = expression.endOffset,
                type = expression.type.remapTypeParameters(callee, actualFunction),
                typeArgumentsCount = expression.typeArgumentsCount,
                valueArgumentsCount = expression.valueArgumentsCount,
                superQualifierSymbol = expression.superQualifierSymbol
            )
            updatedCall.copyTypeAndValueArgumentsFrom(expression)
            updatedCall.extensionReceiver?.let {
                if (it is IrCall) {
                    updatedCall.extensionReceiver = IrConstImpl(
                        it.startOffset, it.endOffset, fragment.irBuiltins.stringType, IrConstKind.String, className
                    )
                }
            }
        }
        val descName = expression.symbol.descriptor.name.asString()
        if (expression.symbol.descriptor.name.isGetter()) {
            className = createStyleSheetClassname(name, descName.replacePropertyAccessor())
        }
        return updatedCall
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: StringBuilder): IrExpression {
        name = expression.symbol.descriptor.name.asString()
        return expression
//        return IrConstImpl(expression.startOffset, expression.endOffset, fragment.irBuiltins.stringType, IrConstKind.String, className)
    }
}