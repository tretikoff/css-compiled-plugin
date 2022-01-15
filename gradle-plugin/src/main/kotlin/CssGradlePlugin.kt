import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.common
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.js
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.io.File
import java.nio.file.Paths

class CssGradlePlugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project): Unit = with(target) {
        try {
            File(target.savedVarPath).writeText("")
            dependencies.add(
                "kotlinCompilerPluginClasspath",
                project(":plugin") // TODO add from maven when published
            )
        } catch (ignored: Throwable) {
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>) = listOf(js, common).contains(kotlinCompilation.platformType)
    override fun getCompilerPluginId(): String = "styled.compiler.plugins.kotlin"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "CssGradlePlugin",
        artifactId = "CssGradlePlugin",
        version = "0.1"
    )

    private val Project.savedVarPath: String
        get() = Paths.get(buildDir.absolutePath, "css.tmp").toString()

    private val KotlinCompilation<*>.resourcesPath: String
        get() {
            val sourceSet = allKotlinSourceSets.firstOrNull { it.name == SourceSet.MAIN_SOURCE_SET_NAME }
            val resourcesDir = (sourceSet?.resources ?: defaultSourceSet.resources).srcDirs.firstOrNull()
                ?: defaultSourceSet.kotlin.srcDirs.first().run { File(parent, "resources") }
            val staticDir = File(resourcesDir, "static")
            return staticDir.absolutePath
        }

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        if (!isApplicable(kotlinCompilation)) return project.provider { listOf() }
        return project.provider {
            val deps =
                project.configurations.flatMap { it.dependencies.withType(ProjectDependency::class.java) } // TODO maybe get only implementation/compileOnly deps
            val depProjects = deps.map { it.dependencyProject }.filter { it.plugins.hasPlugin(this.javaClass) }
            val tempFiles = depProjects.joinToString("_") { it.savedVarPath }
            val varFile = project.savedVarPath
            listOf(
                SubpluginOption(key = "var_file", value = varFile),
                SubpluginOption(key = "subprojects", value = tempFiles),
                SubpluginOption(key = "resources", value = kotlinCompilation.resourcesPath),
            )
        }
    }
}
