import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.io.File
import java.nio.file.Paths

class CssGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project): Unit = with(target) {
        val sharedModel = mapOf("path" to ":plugin") // TODO add from maven when published
        dependencies.add("kotlinCompilerPluginClasspath", dependencies.project(sharedModel))
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "styled.compiler.plugins.kotlin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "CssGradlePlugin",
        artifactId = "CssGradlePlugin",
        version = "0.1"
    )

    private val Project.savedVarPath: String
        get() = Paths.get(buildDir.absolutePath, "tmp", "cssVars", "css.tmp").toString()

    private val KotlinCompilation<*>.resourcesPath: String
        get() {
            val sourceSet = allKotlinSourceSets.firstOrNull {
                it.name == SourceSet.MAIN_SOURCE_SET_NAME
            }
            val resourcesDir = (sourceSet?.resources ?: defaultSourceSet.resources).firstOrNull()
                ?.run { if (isFile) parentFile else this }
                ?: defaultSourceSet.kotlin.srcDirs.first()
                    .run { File(parent, "resources").apply { mkdirs() } }
            return resourcesDir.absolutePath
        }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        return project.provider {
            val subprojects = project.subprojects
                .filter { it.plugins.hasPlugin(this.javaClass) }
            val subprojectTempFiles = subprojects.joinToString { it.savedVarPath }
            listOf(
                SubpluginOption(key = "var_file", value = project.savedVarPath),
                SubpluginOption(key = "subprojects", value = subprojectTempFiles),
                SubpluginOption(key = "resources", value = kotlinCompilation.resourcesPath),
            )
        }
    }
}