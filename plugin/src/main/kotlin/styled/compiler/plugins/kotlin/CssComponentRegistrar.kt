package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

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
            val resourcesPath = configuration[CssCommandLineProcessor.ARG_RESOURCES] ?: return
            val tempPath = configuration[CssCommandLineProcessor.ARG_SAVED_VARS] ?: return
            val vars = configuration[CssCommandLineProcessor.ARG_SUBPROJECT_SAVED_VARS]

            val subprojectTempPaths = if (!vars.isNullOrEmpty()) vars.split("_") else listOf()

            val extension = CssIrGenerationExtension(resourcesPath, tempPath, subprojectTempPaths)

            IrGenerationExtension.registerExtension(project, extension)
        }
    }
}
