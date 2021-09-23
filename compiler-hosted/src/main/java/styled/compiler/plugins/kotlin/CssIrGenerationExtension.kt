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

fun String.normalize(): String {
    return this.replace("<get-", "").replace("<set-", "").replace(">", "").hyphenize()
}

val builder = StringBuilder()
val classnameBuilder = mutableListOf<String>()

// list of stylesheet classname/property pair to css
val dynamicStyleSheets = mutableMapOf<String, String>()

// list of stylesheet classname/property pair to classname
val staticStyleSheets = mutableMapOf<String, String>()

class CssIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // traverse through all of the code
        moduleFragment.acceptChildren(TreeVisitor(), null)
        this.javaClass.getResource("/")?.path?.let {
            // TODO remove hardcode
            val file = File("/Users/Konstantin.Tretiakov/plugin/index.css")
            file.createNewFile()
            file.writeText(it)
            file.writeText(builder.toString())
        }
    }
}
