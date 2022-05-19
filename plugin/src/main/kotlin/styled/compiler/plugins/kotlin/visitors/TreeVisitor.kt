package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrMutableAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isAnnotation
import org.jetbrains.kotlin.name.FqName
import styled.compiler.plugins.kotlin.*
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

val logBuilder = StringBuilder()
fun String.writeLog() {
//    logBuilder.appendLine(this)
}

private enum class SplitCssStrategy { ALL_TO_FILE, FILE_TO_FILE }

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class TreeVisitor(private val filePrefix: String) : AbstractTreeVisitor<StringBuilder>() {
    var cssFiles = ArrayDeque<File>()
    private val splitCssStrategy = SplitCssStrategy.FILE_TO_FILE

    init {
        flush(append = false)
    }

    fun flush(append: Boolean = true) {
        logBuilder.flushToFile("dump.xlog", append)
    }

    lateinit var mainFile: IrFile
    private lateinit var currentFile: IrFile
    private var classNameId = AtomicInteger(0)

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
        when (splitCssStrategy) {
            SplitCssStrategy.ALL_TO_FILE -> {
                val mainCssFile = cssFiles.firstOrNull() ?: File(filePrefix, "index.css").apply { create(); cssFiles.push(this) }
                mainCssFile.appendText(css.toString() + "\n")
            }
            SplitCssStrategy.FILE_TO_FILE ->
                css.flushToFile("$name.css")?.let { file ->
                    cssFiles.push(file)
                }
        }
    }

    private fun String.generateClassname(): String {
        val hash = hashCode()
        return ('a' + abs(hash % 26)) + abs(hash % 1000).toString()
    }

    override fun visitCall(expression: IrCall, data: StringBuilder) {
        super.visitCall(expression, data)
        if (expression.isCssCall()) {
            tryLog("Tree css build failed -----------------") {
                val css = CssInfo()
                expression.accept(ReflCssCollector(CssRuleType.BLOCK), css)
                val classnames = css.joinToString(" ") { cssBuilder ->
                    val str = cssBuilder.toString()
                    val classname = str.generateClassname()
                    data.appendLine(".$classname {").append(str).appendLine("}")
                    classname
                }
                "data $data".writeLog()
                CssTransformer(classnames, isStylesheet = false).transformCall(expression)
            }
        } else if (expression.isSetCustomProperty()) {
//            val name = expression.getValueArgument(0)
//            val value = expression.getValueArgument(1)
//            try {
//                if (name != null && value != null) {
//                    val extractedName = name.extractValues().single()
//                    val extractedValue = value.extractValues().single()
//                    GlobalVariablesVisitor.cssVarValues[extractedName.toString()] = extractedValue.toString()
//                }
//            } catch (e: NoSuchElementException) {
//                // TODO
//            }
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
