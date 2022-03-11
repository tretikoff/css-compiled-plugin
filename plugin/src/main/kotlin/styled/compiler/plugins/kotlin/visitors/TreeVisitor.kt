package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.FqName
import styled.compiler.plugins.kotlin.*
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.set

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class TreeVisitor(private val filePrefix: String) : AbstractTreeVisitor<StringBuilder>() {
    var cssFiles = ArrayDeque<File>()

    lateinit var mainFile: IrFile
    private lateinit var currentFile: IrFile
    private var classNameId = AtomicInteger(0)
    private val generatedClassName: String
        get() = "ksc-static-${classNameId.incrementAndGet()}"

    // Every css from file with filename is written to resources/static/filename.css
    override fun visitFile(declaration: IrFile, data: StringBuilder) {
        if (!::mainFile.isInitialized) {
            mainFile = declaration
        }
        currentFile = declaration

        val css = StringBuilder()
        declaration.acceptChildren(this, css)
        declaration.saveCss(css)
    }

    private fun IrFile.saveCss(css: StringBuilder) {
        if (css.isNotEmpty()) {
            val cssFile = Paths.get(filePrefix, "$name.css").toFile().create()
            cssFile.writeText(css.toString())
            cssFiles.push(cssFile)
        }
    }

    override fun visitCall(expression: IrCall, data: StringBuilder) {
        super.visitCall(expression, data)
        if (expression.isCssCall()) {
            tryLog("Tree css build failed -----------------") {
                val css = StringBuilder()
                expression.accept(CssCollector(generatedClassName), css)
                data.append(css)
            }
            CssTransformer(generatedClassName, isStylesheet = false).transformCall(expression)
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

    override fun visitElement(element: IrElement, data: StringBuilder) {
        if (element is IrMutableAnnotationContainer && element.annotations.any { it.isMain() }) {
//        if (declaration.annotations.any { it.isMain() }) {
            "^^^^^^^^${currentFile.name}".writeLog()
            mainFile = currentFile
        }
        if (element is IrClass || element is IrCall || element is IrFile) return // TODO wtf? how to do better?
        element.acceptChildren(this, data)
    }

    private fun IrConstructorCall.isMain() = isAnnotation(FqName("kotlin.js.JsExport")) // Heuristics to check if current file is main app file
}
