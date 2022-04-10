package styled.compiler.plugins.kotlin

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildProperty
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import styled.compiler.plugins.kotlin.*
import styled.compiler.plugins.kotlin.visitors.writeLog
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

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
