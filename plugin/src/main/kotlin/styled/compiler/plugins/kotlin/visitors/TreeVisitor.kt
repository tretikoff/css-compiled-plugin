package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import styled.compiler.plugins.kotlin.*
import java.util.concurrent.atomic.*
import kotlin.collections.any
import kotlin.collections.set
import kotlin.collections.single

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class TreeVisitor : AbstractTreeVisitor<StringBuilder>() {
    lateinit var mainFile: IrFile
    private lateinit var currentFile: IrFile
    private var classNameId = AtomicInteger(0)
    private val generatedClassName: String
        get() = "ksc-static-${classNameId.incrementAndGet()}"

    override fun visitFile(declaration: IrFile, data: StringBuilder) {
        if (!::mainFile.isInitialized) {
            mainFile = declaration
        }
        currentFile = declaration
        declaration.acceptChildren(this, data)
    }

    override fun visitCall(expression: IrCall, data: StringBuilder) {
        super.visitCall(expression, data)
        if (expression.isCssCall()) {
            try {
                val css = StringBuilder()
                expression.accept(CssCollector(generatedClassName), css)
                data.append(css)
            } catch (e: Throwable) {
                "-----------------".writeLog()
            }
            expression.transform(CssTransformer(generatedClassName), null)
        } else if (expression.isSetCustomProperty()) {
            val name = expression.getValueArgument(0)
            val value = expression.getValueArgument(1)
            try {
                if (name != null && value != null) {
                    val extractedName = name.extractValues().single()
                    val extractedValue = value.extractValues().single()
                    GlobalVariablesVisitor.cssVarValues[extractedName.toString()] = extractedValue.toString()
                }
            } catch (e: NoSuchElementException) {
                // TODO
            }
        } else {
            expression.acceptChildren(this, data)
        }
    }

    private fun String.isValid() = this != "<no name provided>" // TODO

    override fun visitClass(declaration: IrClass, data: StringBuilder) {
        super.visitClass(declaration, data)
        if (declaration.isStyleSheet()) {
            val name = declaration.name.asString()
            "$$$$name".writeLog()
            if (name.isValid())
                declaration.acceptChildren(StyleSheetVisitor(name), data)
        } else {
            declaration.acceptChildren(this, data)
        }
    }

    private fun IrConstructorCall.isMain() = isAnnotation(FqName("kotlin.js.JsExport")) // Heuristics to check if current file is main app file

    override fun visitElement(element: IrElement, data: StringBuilder) {
        if (element is IrMutableAnnotationContainer && element.annotations.any { it.isMain() }) {
            "^^^^^^^^${currentFile.name}".writeLog()
            mainFile = currentFile
        }
        element.acceptChildren(this, data)
    }
}
