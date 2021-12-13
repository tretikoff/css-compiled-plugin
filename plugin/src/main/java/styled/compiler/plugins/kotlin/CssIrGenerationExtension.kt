package styled.compiler.plugins.kotlin

import kotlinx.css.hyphenize
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.backend.evaluate.evaluateConstants
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
import styled.compiler.plugins.kotlin.visitors.GlobalVariablesVisitor
import styled.compiler.plugins.kotlin.visitors.TreeVisitor
import java.io.File
import java.nio.file.Paths
import java.util.*

typealias Parameter = Pair<String, Any?>

fun IrCall.getConstValues(): Collection<String?> {
    return this.getArgumentsWithIr()
        .map { it.second }
        .filterIsInstanceMapNotNull<IrConstImpl<*>, String?> { it.value?.toString() }
}

fun String.replacePropertyAccessor(): String {
    return this.replace("<get-", "").replace("<set-", "").replace(">", "")
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun String.normalize(): String {
    return this.replacePropertyAccessor().hyphenize()
}

fun createStyleSheetClassname(name: String, propertyName: String): String {
    return "$name-$propertyName"
}

lateinit var fragment: IrModuleFragment
lateinit var context: IrPluginContext

private val logBuilder = StringBuilder()
private val cssBuilder = StringBuilder()
fun String.writeLog() {
    logBuilder.appendLine(this)
}

private fun File.saveVariables(values: Map<String, String>) {
    writeText("")
    values.forEach { (name, value) ->
        appendText("$name:$value${System.lineSeparator()}")
    }
}

class CssIrGenerationExtension(resourcesPath: String, varPath: String, private val subprojectVarPaths: List<String>) :
    IrGenerationExtension {
    private val logFile = Paths.get(resourcesPath, "dump.log").toFile().apply {
        createNewFile()
    }
    private val cssFile = Paths.get(resourcesPath, "index.css").toFile().apply {
        createNewFile()
    }
    private val varFile = File(varPath).apply { parentFile.mkdirs(); createNewFile() }
    private fun loadSavedSubprojectVariables() {
        subprojectVarPaths.forEach {
            try {
                File(it).reader().useLines { lines ->
                    lines.forEach { line ->
                        val (name, value) = line.split(":")
                        GlobalVariablesVisitor.varValues[name] = value
                    }
                }
            } catch (e: Exception) {
                logBuilder.appendLine(e.stackTrace)
            }
        }
    }

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        loadSavedSubprojectVariables()
        fragment = moduleFragment
        context = pluginContext
        // traverse through all the code and
        // collect variables
        evaluateConstants(fragment)
        fragment.acceptChildrenVoid(GlobalVariablesVisitor())
        // then transform and collect css code
        fragment.acceptChildren(TreeVisitor(), cssBuilder)

        // Css variables
        val cssRootBuilder = StringBuilder().appendLine(":root {")
        GlobalVariablesVisitor.cssVarValues.entries.forEach { (name, value) -> cssRootBuilder.appendLine("--$name: $value;") }
        cssRootBuilder.appendLine("}")


        varFile.saveVariables(GlobalVariablesVisitor.varValues)
        cssFile.writeText(cssRootBuilder.toString())
        cssFile.appendText(cssBuilder.toString())
        logFile.writeText(logBuilder.toString())
    }
}
