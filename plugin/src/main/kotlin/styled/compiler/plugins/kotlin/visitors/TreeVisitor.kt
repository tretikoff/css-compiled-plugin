package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import styled.compiler.plugins.kotlin.*
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger

val logBuilder = StringBuilder()
fun String.writeLog() {
    logBuilder.appendLine(this)
}

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class TreeVisitor(private val filePrefix: String) : AbstractTreeVisitor<StringBuilder>() {
    var cssFiles = ArrayDeque<File>()

    init {
        flush(append = false)
    }

    fun flush(append: Boolean = true) {
        logBuilder.flushToFile("dump.xlog", append)
    }

    lateinit var mainFile: IrFile

    // TODO create new class
    var classWithImports: IrClass? = null
    private lateinit var currentFile: IrFile
    private var classNameId = AtomicInteger(0)
    private val generatedClassName: String
        get() = "ksc-static-${classNameId.incrementAndGet()}"

    // Every css from file with filename is written to resources/static/filename.css
    override fun visitFile(declaration: IrFile, data: StringBuilder) {
        super.visitFile(declaration, data)
        if (!::mainFile.isInitialized) {
            mainFile = declaration
        }
        currentFile = declaration

        val css = StringBuilder()
        declaration.acceptChildren(this, css)
        logBuilder.flushToFile("${declaration.name}.xlog")
        declaration.saveCss(css)
    }

    private fun StringBuilder.flushToFile(filename: String, append: Boolean = false): File? {
        return if (isNotEmpty()) {
            val file = Paths.get(filePrefix, filename).toFile().create()
            if (append) file.appendText(toString()) else file.writeText(toString())
            clear()
            file
        } else null
    }

    private fun IrFile.saveCss(css: StringBuilder) {
        css.flushToFile("$name.css")?.let { file ->
            cssFiles.push(file)
        }
    }

    override fun visitCall(expression: IrCall, data: StringBuilder) {
        super.visitCall(expression, data)
        if (expression.isCssCall()) {
//            tryLog("Tree css build failed -----------------") {
//                val css = StringBuilder()
//                expression.accept(ReflCssCollector(generatedClassName), css)
//                data.append(css)
//            }
            CssTransformer(generatedClassName, isStylesheet = false).transformCall(expression)
        } else if (expression.isSetCustomProperty()) {
            val name = expression.getValueArgument(0)
            val value = expression.getValueArgument(1)
            try {
//                if (name != null && value != null) {
//                    val extractedName = name.extractValues().single()
//                    val extractedValue = value.extractValues().single()
//                    GlobalVariablesVisitor.cssVarValues[extractedName.toString()] = extractedValue.toString()
//                }
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

//        declaration.dump().writeLog()
        if (declaration.name.asString().contains("ChatContactComponent")) {
            declaration.dump().writeLog()
        } else {
            "_".writeLog()
        }
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
        if (element is IrClass || element is IrCall || element is IrFile) return // TODO do better?
        element.acceptChildren(this, data)
    }

    private fun IrConstructorCall.isMain() = isAnnotation(FqName("kotlin.js.JsExport")) // Heuristics to check if current file is main app file
}
