package repro.deepcopy.generation

import kotlinx.css.hyphenize
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.util.collectionUtils.filterIsInstanceMapNotNull
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

class CssIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val builder = StringBuilder()
        fragment = moduleFragment
        context = pluginContext
        // traverse through all the code
        fragment.acceptChildren(TreeVisitor(), builder)

//        val path = fragment.files.first().path.replaceAfterLast(File.separator, "index.css")
        val path = "/Users/Konstantin.Tretiakov/plugin/index.css"
        val file = File(path)
        file.createNewFile()
        file.writer().use { writer ->
            writer.write(builder.toString())
        }
    }
}
