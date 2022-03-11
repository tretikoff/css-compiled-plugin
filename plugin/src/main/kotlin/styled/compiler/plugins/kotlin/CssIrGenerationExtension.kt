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

private fun String.resource(filename: String) = Paths.get(this, filename).toFile()

private fun MutableList<File>.addCssFile(filename: String) = try {
    add(File(filename))
    "Added subproject css $filename".writeLog()
} catch (e: FileNotFoundException) {
    filename.writeLog()
    e.stackTraceToString().writeLog()
}

fun File.create() = apply { parentFile.mkdirs(); createNewFile() }

// У каждого плагина появляется список сгенерированных CSS файлов
class CssIrGenerationExtension(private val resourcesPath: String, varPath: String, private val subprojectVarPaths: List<String>) :
    IrGenerationExtension {
    private val logFile by lazy { resourcesPath.resource("dump.xlog").create() }
    private val mainCssFile by lazy { resourcesPath.resource("index.css").create() }
    private val loadedSubprojFiles = mutableListOf<File>()
    private val cssFilenamesCacheFile by lazy { resourcesPath.resource("cssSub.txt").create() }
    private val varFile by lazy { File(varPath).create() }
    private val files = mutableSetOf<File>() // Subproject css files to include

    private fun loadSavedSubprojectVariables() {
        subprojectVarPaths.forEach {
            tryLog("Load variables file") {
                File(it).reader().useLines { lines ->
                    lines.forEachIndexed { i, line ->
                        if (i == 0) {
                            line.split(",").forEach { loadedSubprojFiles.addCssFile(it) }
                        } else {
                            val (name, value) = line.split(":")
                            GlobalVariablesVisitor.varValues[name] = value
                        }
                    }
                }
            }
        }
    }

    private fun saveCssVariables() {
        // TODO write to separate file
        // Css variables
        val entries = GlobalVariablesVisitor.cssVarValues.entries
        StringBuilder().apply {
            if (entries.isNotEmpty()) {
                appendLine(":root {")
                entries.forEach { (name, value) -> appendLine("--$name: $value;") }
                appendLine("}")
                mainCssFile.writeText(toString())
                files.add(mainCssFile)
            }
        }
    }

    private fun saveSubprojCssFiles() {
        // TODO use relative paths everywhere
        cssFilenamesCacheFile.writeText(files.joinToString("\n") { it.absolutePath })
    }

    private fun TreeVisitor.loadSubprojCssFiles() {
        for (filename in cssFilenamesCacheFile.readLines()) {
            files.add(File(filename))
        }
        files.addAll(cssFiles)
    }

    private fun TreeVisitor.importSubprojCssFiles() {
        files.addAll(loadedSubprojFiles)
        files.forEach(mainFile::importStaticCss)
    }

    private fun File.saveCssFilenames() {
        writeText(files.joinToString(",") { it.absolutePath } + "\n")
    }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        loadSavedSubprojectVariables()
        fragment = moduleFragment
        context = pluginContext
        // traverse through all the code and
        // collect variables
        fragment.acceptChildrenVoid(GlobalVariablesVisitor())
        // then transform and collect css code

        // TODO make order-independent
        val treeVisitor = TreeVisitor(resourcesPath)
        fragment.acceptChildren(treeVisitor, cssBuilder)
        saveCssVariables()

        treeVisitor.loadSubprojCssFiles()
        treeVisitor.importSubprojCssFiles()

        varFile.saveCssFilenames()
        varFile.saveVariables(GlobalVariablesVisitor.varValues)
        logFile.writeText(logBuilder.toString())
        saveSubprojCssFiles()
    }
}
