package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import styled.compiler.plugins.kotlin.*
import java.io.File

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */

fun IrFile.importStaticCss(cssFile: File) {
    val filename = cssFile.path // TODO static
    filename.writeLog()
    val jsModule = context.referenceConstructors(FqName("kotlin.js.JsModule")).first()
    val definedExt = context.referenceProperties(FqName("kotlin.js.definedExternally")).first().owner.getter!!
    val cssFileProperty = context.irFactory.buildProperty {
        origin = IrDeclarationOrigin.DEFINED
        name = Name.identifier("indexCss")
        visibility = DescriptorVisibilities.PUBLIC
        modality = Modality.FINAL
        isDelegated = false
        isLateinit = false
        isConst = false
        isVar = false
        isExternal = true
    }
    val cssFileBackingField = context.irFactory.buildField {
        origin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD
        name = Name.identifier("indexCss")
        type = definedExt.returnType
        visibility = DescriptorVisibilities.PRIVATE
        isExternal = true
        isFinal = true
        isStatic = true
    }
    val getterFun = context.irFactory.buildFun {
        origin = IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        name = Name.identifier("<get-indexCss>")
        returnType = definedExt.returnType
    }
    cssFileBackingField.initializer = IrExpressionBodyImpl(0, 0) {
        expression =
            IrCallImpl(0, 0, definedExt.returnType, definedExt.symbol, 0, 0, IrStatementOrigin.GET_PROPERTY)
    }
    getterFun.body = IrBlockBodyImpl(0, 0) {
        statements.add(
            0, IrReturnImpl(
                0, 0, context.irBuiltIns.nothingType, getterFun.symbol,
                IrGetFieldImpl(0, 0, cssFileBackingField.symbol, cssFileBackingField.type)
            )
        )
    }
    getterFun.correspondingPropertySymbol = cssFileProperty.symbol
    cssFileProperty.getter = getterFun

    val jsNonModule = context.referenceConstructors(FqName("kotlin.js.JsNonModule")).first()
    cssFileBackingField.parent = this
    cssFileProperty.parent = this
    getterFun.parent = this

    val jsModuleAnnotation = IrConstructorCallImpl(0, 0, context.irBuiltIns.annotationType, jsModule, 0, 0, 1)
    jsModuleAnnotation.putValueArgument(0, filename.toIrConst(context.irBuiltIns.stringType))
    cssFileProperty.annotations = listOf(
        jsModuleAnnotation,
        IrConstructorCallImpl(0, 0, context.irBuiltIns.annotationType, jsNonModule, 0, 0, 0),
    )

    declarations.add(cssFileBackingField)
    declarations.add(0, cssFileProperty)
}
