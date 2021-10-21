package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import repro.deepcopy.generation.normalize
import styled.compiler.plugins.kotlin.isCssCall
import styled.compiler.plugins.kotlin.isPlus

/** TODO value.unit -> to value.px */

class CssTransformer(val className: String = "", val indent: String = "  ") : IrElementTransformer<StringBuilder> {
    override fun visitCall(expression: IrCall, data: StringBuilder): IrElement {
        val owner = expression.symbol.owner
        if (owner.isPlus()) {
            return expression.transform(StyleSheetTransformer(), data)
        } else if (expression.isCssCall()) {
            expression.transformChildren(this, data)
            // TODO get all the classnames from children to one string
            // TODO change the call of children of css function to unary plus call with this string
//            data.appendLine(expression.dump())
//            var updatedCall = expression
//            callee.symbol.owner.todo(data)
//            val actualFunction = callee.getConstCssFunction().owner as IrSimpleFunction
//            data.append("$>>>" + actualFunction.name)
//                updatedCall = IrCallImpl(
//                    symbol = actualFunction.symbol,
//                    origin = expression.origin,
//                    startOffset = expression.startOffset,
//                    endOffset = expression.endOffset,
//                    type = expression.type.remapTypeParameters(callee, actualFunction),
//                    typeArgumentsCount = expression.typeArgumentsCount,
//                    valueArgumentsCount = expression.valueArgumentsCount,
//                    superQualifierSymbol = expression.superQualifierSymbol
//                )
//                updatedCall.copyTypeAndValueArgumentsFrom(expression)
//                updatedCall.extensionReceiver?.let {
//                    if (it is IrCall) {
//                        if (it.symbol.owner.name.isPropertyGetter()) {
//                            (it.extensionReceiver as? IrGetObjectValue)?.let { styleSheetObj ->
//                                name = styleSheetObj.symbol.owner.name.asString()
//                            }
//                        }
//                        updatedCall.extensionReceiver = IrConstImpl(
//                            it.startOffset, it.endOffset, fragment.irBuiltins.stringType, IrConstKind.String, className
//                        )
//                    }
//                }
        } else {
            val name = owner.name.asString().normalize()
            val propBuilder = StringBuilder()
            expression.acceptChildren(PropertyVisitor(), propBuilder)
            data.appendLine("$indent$name:$propBuilder;")
        }
        return expression
    }

    override fun visitElement(element: IrElement, data: StringBuilder): IrElement {
        element.transformChildren(this, data)
        return element
    }
}
