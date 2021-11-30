import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.io.File


class CssGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project): Unit = with(target) {
        extensions.create("template", CssGradleExtension::class.java)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "styled.compiler.plugins.kotlin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "CssGradlePlugin",
        artifactId = "CssGradlePlugin",
        version = "0.1"
    )

    val Project.resourceTempFile: String
        get() {
            return resources.text.fromString("").asFile().absolutePath
        }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val sourceSet = kotlinCompilation.allKotlinSourceSets.firstOrNull {
            it.name == SourceSet.MAIN_SOURCE_SET_NAME
        }

        val resourcesDir = (sourceSet?.resources ?: kotlinCompilation.defaultSourceSet.resources).firstOrNull()
            ?.run { if (isFile) parentFile else this }
            ?: kotlinCompilation.defaultSourceSet.kotlin.srcDirs.first()
                .run { File(parent, "resources").apply { mkdirs() } }
        return project.provider {
            val subprojects = project.subprojects
                .filter { it.plugins.hasPlugin(this.javaClass) }
                .joinToString { it.path }
            val subprojectTempFiles = project.subprojects.joinToString { it.resourceTempFile }
            println(subprojectTempFiles)
            listOf(
                SubpluginOption(key = "css_file", value = resourcesDir.absolutePath),
                SubpluginOption(key = "subprojects", value = subprojects),
            )
        }
    }
}