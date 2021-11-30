import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class CssGradleExtension(objects: ObjectFactory) {
    val resourcesDirProperty: Property<String> = objects.property(String::class.java)
    val subprojectsProperty: Property<String> = objects.property(String::class.java)
}