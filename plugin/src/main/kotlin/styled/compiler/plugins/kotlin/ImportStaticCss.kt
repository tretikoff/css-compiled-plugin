package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import styled.compiler.plugins.kotlin.*
import styled.compiler.plugins.kotlin.visitors.writeLog

/**
 * Visitor traverses through all the code, finds stylesheet and css nodes and applies [StyleSheetVisitor] and [CssTransformer] to them
 */

internal enum class LogLevel {
    ALL,
    ERROR
}

internal inline fun tryLog(operation: String, level: LogLevel = LogLevel.ERROR, block: () -> Unit) {
    try {
        if (level == LogLevel.ALL) "$operation started".writeLog()
        block()
        if (level == LogLevel.ALL) "$operation finished".writeLog()
    } catch (e: Throwable) {
        "Error $operation: ${e.stackTraceToString()}".writeLog()
    }
}
