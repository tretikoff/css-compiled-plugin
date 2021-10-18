package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import repro.deepcopy.generation.CssIrGenerationExtension

class CssCommandLineProcessor : CommandLineProcessor {
    override val pluginId = "styled.compiler.plugins.kotlin"
    override val pluginOptions = listOf<AbstractCliOption>()

//    override fun processOption(
//        option: AbstractCliOption,
//        value: String,
//        configuration: CompilerConfiguration
//    ) = when (option) {
//        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
//    }
}

class CssComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration
    ) {
        registerProjectExtensions(
            project as Project,
            configuration
        )
    }

    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun registerProjectExtensions(
            project: Project,
            configuration: CompilerConfiguration
        ) {
            IrGenerationExtension.registerExtension(
                project,
                CssIrGenerationExtension()
            )
        }
    }
}
