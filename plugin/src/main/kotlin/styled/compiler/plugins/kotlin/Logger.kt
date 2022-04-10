package styled.compiler.plugins.kotlin

import styled.compiler.plugins.kotlin.*
import java.io.File
import java.nio.file.Paths


/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */
class Logger(private val filePrefix: String, private val fileEnding: String) {
    private val logBuilder = StringBuilder()

    private fun flushToFile(filename: String): File? {
        return if (logBuilder.isNotEmpty()) {
            val file = Paths.get(filePrefix, "${filePrefix}$filename$fileEnding").toFile().create()
            file.writeText(toString())
            logBuilder.clear()
            file
        } else null
    }
}
