package framework.x

import kotlinx.css.*
import runtime.html.CommonTheme

/**
 * Theme for platform-level components.
 * For application-specific components please use AppTheme.
 */
interface XTheme {
    companion object {
        val theme = DefaultTheme
        val dark = DarkTheme()

        private fun CssBuilder.setDefault() {
            setCustomProperty("skeleton-background-color", theme.skeletonBackgroundColorRaw)
        }

        private fun CssBuilder.setDark(suffix: String = "-dark") {
            setCustomProperty("skeleton-background-color$suffix", dark.skeletonBackgroundColorRaw)
        }
    }

    val skeletonBackgroundColor: Color
    val skeletonBackgroundColorRaw: Color

    open class DefaultThemeBase : XTheme {
        override val skeletonBackgroundColor = "skeleton-background-color".toColorProperty()
        override val skeletonBackgroundColorRaw = CommonTheme.textColor.withAlpha(0.05)
    }

    object DefaultTheme : DefaultThemeBase()

    open class DarkTheme : DefaultThemeBase() {
        override val skeletonBackgroundColor = "skeleton-background-color-dark".toColorProperty()
        override val skeletonBackgroundColorRaw = whiteAlpha(0.05)
    }
}

// `styles` are applied when the dark theme is enabled
// Only works for setting CSS custom properties, not for regular component CSS!
//fun CssBuilder.applyDarkThemeProperties(styles: RuleSet) {
//    media("(prefers-color-scheme: dark)") {
//        ":root[data-dark-theme='AUTO']" {
//            styles()
//        }
//    }
//
//    ":root[data-dark-theme='ENABLED']" {
//        styles()
//    }
//}

fun String.toColorProperty(): Color {
    return Color(this.toCustomProperty())
}

fun Color.fromColorProperty(): String {
    return this.value.removePrefix("var(--").removeSuffix(")")
}
