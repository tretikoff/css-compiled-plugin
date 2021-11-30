package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import repro.deepcopy.generation.CssIrGenerationExtension
import repro.deepcopy.generation.writeDump

class CssCommandLineProcessor : CommandLineProcessor {
    companion object {
        private const val OPTION_RESOURCES = "css_file"
        private const val OPTION_SUBPROJECTS = "subprojects"

        val ARG_RESOURCES = CompilerConfigurationKey<String>(OPTION_RESOURCES)
        val ARG_SUBPROJECTS = CompilerConfigurationKey<String>(OPTION_SUBPROJECTS)
    }

    override val pluginId = "styled.compiler.plugins.kotlin"
    override val pluginOptions = listOf(
        CliOption(
            optionName = OPTION_RESOURCES,
            valueDescription = "",
            description = "path to output css file",
            required = false
        ),
        CliOption(
            optionName = OPTION_SUBPROJECTS,
            valueDescription = "",
            description = "subprojects in which css variables can be found",
            required = false
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option.optionName) {
        OPTION_RESOURCES -> configuration.put(ARG_RESOURCES, value)
        OPTION_SUBPROJECTS -> configuration.put(ARG_SUBPROJECTS, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }.also {
        "<<<<<$value".writeDump()
    }
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
                CssIrGenerationExtension(configuration[CssCommandLineProcessor.ARG_RESOURCES] ?: "")
            )
        }
    }
}
