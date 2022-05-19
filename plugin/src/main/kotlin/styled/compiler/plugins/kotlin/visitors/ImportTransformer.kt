package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import styled.compiler.plugins.kotlin.LogLevel
import styled.compiler.plugins.kotlin.context
import styled.compiler.plugins.kotlin.tryLog
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

private val refCount = AtomicInteger(0)
private var moduleVars: MutableList<Pair<IrField, IrProperty>> = mutableListOf()

/**
 * Visitor traverses through all the code
 */

class ImportTransformer(val files: List<File>) : IrElementTransformerVoid() {
    var initialized = false
    override fun visitFile(declaration: IrFile) = declaration.apply {
        if (initialized) return this
        ">>>${module.files.joinToString { it.name }}".writeLog()
        files.filter{!it.path.contains("Support")}.forEach { file ->
            tryLog("CSS Import ${file.path}", LogLevel.ALL) {
                val jsModule = context.referenceConstructors(FqName("kotlin.js.JsModule")).first()
                val definedExt = context.referenceProperties(FqName("kotlin.js.definedExternally")).first().owner.getter!!
                val jsNonModule = context.referenceConstructors(FqName("kotlin.js.JsNonModule")).first()
                val varName = "indexCss" + refCount.incrementAndGet()

                val cssFileProperty = context.irFactory.buildProperty {
                    origin = IrDeclarationOrigin.DEFINED
                    name = Name.identifier(varName)
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
                    name = Name.identifier(varName)
                    type = definedExt.returnType
                    visibility = DescriptorVisibilities.PRIVATE
                    isExternal = true
                    isFinal = true
                    isStatic = true
                }
                val getterFun = context.irFactory.buildFun {
                    isExternal = true
                    origin = IrDeclarationOrigin.GENERATED_SETTER_GETTER
                    name = Name.identifier("<get-$varName>")
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
                moduleVars.add(cssFileBackingField to cssFileProperty)

                cssFileBackingField.parent = this
                cssFileProperty.parent = this
                getterFun.parent = this

                val jsModuleAnnotation = IrConstructorCallImpl(0, 0, context.irBuiltIns.annotationType, jsModule, 0, 0, 1)
                jsModuleAnnotation.putValueArgument(0, file.path.toIrConst(context.irBuiltIns.stringType))
                cssFileProperty.annotations = listOf(
                    jsModuleAnnotation,
                    IrConstructorCallImpl(0, 0, context.irBuiltIns.annotationType, jsNonModule, 0, 0, 0),
                )

                declarations.add(cssFileBackingField)
                "Added declaration, now ${declarations.joinToString()}".writeLog()
                declarations.add(cssFileProperty)
                addDummy(cssFileProperty, this)
//    cssFileProperty.backingField = cssFileBackingField
            }
        }
        dump().writeLog()
        initialized = true
    }

    fun addDummy(prop: IrProperty, clazz: IrFile) {
        val function = IrFunctionImpl(
            -1,
            -1,
            IrDeclarationOrigin.DEFINED,
            IrSimpleFunctionSymbolImpl(),
            Name.identifier("\$dummyCss${refCount}"),
            DescriptorVisibilities.PRIVATE,
            Modality.FINAL,
            context.irBuiltIns.unitType,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        )

        function.parent = clazz
        clazz.declarations.add(function)
        moduleVars.removeFirstOrNull()
        val getter = prop.getter!!
        val call = IrCallImpl(-1, -1, getter.returnType, getter.symbol, 0, 0, origin = IrStatementOrigin.GET_PROPERTY)
        function.body = IrBlockBodyImpl(-1, -1, listOf(call))
        moduleVars.clear()
    }
}
