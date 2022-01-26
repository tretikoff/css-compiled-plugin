package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

class CssCommandLineProcessor : CommandLineProcessor {
    companion object {
        private const val OPTION_SAVED_VARS = "var_file"
        private const val OPTION_RESOURCES = "resources"
        private const val OPTION_SUBPROJECT_SAVED_VARS = "subprojects"

        val ARG_RESOURCES = CompilerConfigurationKey<String>(OPTION_RESOURCES)
        val ARG_SUBPROJECT_SAVED_VARS = CompilerConfigurationKey<String>(OPTION_SUBPROJECT_SAVED_VARS)
        val ARG_SAVED_VARS = CompilerConfigurationKey<String>(OPTION_SAVED_VARS)
    }

    override val pluginId = "styled.compiler.plugins.kotlin"
    override val pluginOptions = listOf(
        CliOption(
            optionName = OPTION_RESOURCES,
            valueDescription = "",
            description = "Path to output css file",
            required = false
        ),
        CliOption(
            optionName = OPTION_SUBPROJECT_SAVED_VARS,
            valueDescription = "",
            description = "Paths to subproject's variable sources",
            required = false
        ),
        CliOption(
            optionName = OPTION_SAVED_VARS,
            valueDescription = "",
            description = "Path to store variable values",
            required = false
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) = when (option.optionName) {
        OPTION_RESOURCES -> configuration.put(ARG_RESOURCES, value)
        OPTION_SUBPROJECT_SAVED_VARS -> configuration.put(ARG_SUBPROJECT_SAVED_VARS, value)
        OPTION_SAVED_VARS -> configuration.put(ARG_SAVED_VARS, value)
        else -> throw CliOptionProcessingException("Unknown option: ${option.optionName}")
    }
}
