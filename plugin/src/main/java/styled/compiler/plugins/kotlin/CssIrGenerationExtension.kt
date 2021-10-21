package repro.deepcopy.generation

import kotlinx.css.hyphenize
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
import styled.compiler.plugins.kotlin.visitors.GlobalVariablesVisitor
import styled.compiler.plugins.kotlin.visitors.TreeVisitor
import java.io.File

typealias Parameter = Pair<String, Any?>

fun IrCall.getConstValues(): Collection<String?> {
    return this.getArgumentsWithIr()
        .map { it.second }
        .filterIsInstanceMapNotNull<IrConstImpl<*>, String?> { it.value?.toString() }
}

fun String.replacePropertyAccessor(): String {
    return this.replace("<get-", "").replace("<set-", "").replace(">", "")
}

fun String.normalize(): String {
    return this.replacePropertyAccessor().hyphenize()
}

fun createStyleSheetClassname(name: String, propertyName: String): String {
    return "$name-$propertyName"
}

lateinit var fragment: IrModuleFragment
lateinit var context: IrPluginContext

private val dumpBuilder = StringBuilder()
fun String.writeDump() {
    dumpBuilder.appendLine(this)
}

class CssIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val builder = StringBuilder()
        fragment = moduleFragment
        context = pluginContext
        // traverse through all the code
        fragment.acceptChildren(GlobalVariablesVisitor(), builder)
        GlobalVariablesVisitor.varValues.entries.forEach { (name, value) ->
            ">>>$name  <<>>  $value<<<<".writeDump()
        }
        fragment.acceptChildren(TreeVisitor(), builder)

        // Css vars collecting
        // TODO not dump everything into the root
        val cssVarBuilder = StringBuilder(":root {\n")
        GlobalVariablesVisitor.cssVarValues.entries.forEach { (name, value) -> cssVarBuilder.appendLine("--$name: $value;") }
        cssVarBuilder.appendLine("}")


        cssVarBuilder.appendLine(builder).dumpToFile("index.css")
        dumpBuilder.dumpToFile("dump.log")
    }

    private fun StringBuilder.dumpToFile(filename: String) {
//        val path = fragment.files.first().path.replaceAfterLast(File.separator, filename)
        val path = "/Users/Konstantin.Tretiakov/plugin/$filename"
        val file = File(path)
        file.createNewFile()
        file.writer().use { writer ->
            writer.write(toString())
        }
    }
}
