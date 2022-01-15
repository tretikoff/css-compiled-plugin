package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import styled.compiler.plugins.kotlin.visitors.GlobalVariablesVisitor
import styled.compiler.plugins.kotlin.visitors.TreeVisitor
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Paths

lateinit var fragment: IrModuleFragment
lateinit var context: IrPluginContext

private val logBuilder = StringBuilder()
private val cssBuilder = StringBuilder()

enum class Mode {
    FULL,
    STYLESHEET_STATIC
}

val mode = Mode.STYLESHEET_STATIC

fun String.writeLog() {
    logBuilder.appendLine(this)
}

private fun File.saveVariables(values: Map<String, String>) {
    values.forEach { (name, value) ->
        appendText("$name:$value${System.lineSeparator()}")
    }
}

private fun String.toLogFile() = Paths.get(this, "dump.xlog").toFile()
private fun String.toCssFile() = Paths.get(this, "index.css").toFile()

private fun MutableList<File>.addCssFile(filename: String) = try {
    add(File(filename))
    "Added subproject css $filename".writeLog()
} catch (e: FileNotFoundException) {
    filename.writeLog()
    e.stackTraceToString().writeLog()
}

fun File.create() = apply { parentFile.mkdirs(); createNewFile() }

class CssIrGenerationExtension(resourcesPath: String, varPath: String, private val subprojectVarPaths: List<String>) :
    IrGenerationExtension {
    private val logFile by lazy { resourcesPath.toLogFile().create() }
    private val cssFile by lazy { resourcesPath.toCssFile().create() }
    private val varFile by lazy { File(varPath).create() }
    private val files = mutableListOf<File>()

    private fun loadSavedSubprojectVariables() {
        subprojectVarPaths.forEach {
            try {
//                "Loading saved variables: $it".writeLog()
                File(it).reader().useLines { lines ->
                    lines.forEachIndexed { i, line ->
                        if (i == 0) {
                            files.addCssFile(line)
                        } else {
                            val (name, value) = line.split(":")
                            GlobalVariablesVisitor.varValues[name] = value
                        }
                    }
                }
            } catch (e: Exception) {
                "Failed to load variables file: $it".writeLog()
                e.stackTraceToString().writeLog()
            }
        }
    }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        loadSavedSubprojectVariables()
        fragment = moduleFragment
        context = pluginContext
        // traverse through all the code and
        // collect variables
        fragment.acceptChildrenVoid(GlobalVariablesVisitor())
        // then transform and collect css code
        val treeVisitor = TreeVisitor()
        fragment.acceptChildren(treeVisitor, cssBuilder)
        (files + cssFile).forEach(treeVisitor.sourceFile::importStaticCss)

        // Css variables
        val entries = GlobalVariablesVisitor.cssVarValues.entries
        val cssRootBuilder = StringBuilder().apply {
            if (entries.isNotEmpty()) {
                appendLine(":root {")
                entries.forEach { (name, value) -> appendLine("--$name: $value;") }
                appendLine("}")
            }
        }

//        if (cssBuilder.isEmpty() && cssRootBuilder.isEmpty()) return

        varFile.writeText(cssFile.absolutePath + "\n")
        varFile.saveVariables(GlobalVariablesVisitor.varValues)
        cssFile.writeText(cssRootBuilder.toString())
        cssFile.appendText(cssBuilder.toString())
        logFile.writeText(logBuilder.toString())
    }
}
