package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import styled.compiler.plugins.kotlin.writeLog

private val variables = mutableMapOf<IrDeclaration, Any?>()
private val unvisitedVariables = mutableMapOf<IrDeclaration, IrElement?>()

abstract class AbstractTreeVisitor<T> : IrElementVisitor<Unit, T> {
    fun getVariable(decl: IrDeclaration): Any? {
        return variables[decl]
    }

    private fun saveVariable(declaration: IrDeclaration, initializer: IrElement?) {
        val value = try {
            initializer?.extractValues()?.firstOrNull()
        } catch (e: Exception) {
            e.stackTraceToString().writeLog();
            null
        }
        "saving variable ${declaration.nameForIrSerialization} $declaration $value".writeLog()
        if (value != null) {
            variables[declaration] = value
        } else {
            unvisitedVariables[declaration] = value
        }
    }

    override fun visitVariable(declaration: IrVariable, data: T) {
        super.visitVariable(declaration, data)
        saveVariable(declaration, declaration)
    }

    override fun visitField(declaration: IrField, data: T) {
        super.visitField(declaration, data)
        saveVariable(declaration, declaration.initializer)
    }


//    override fun visitSetField(expression: IrSetField, data: T) {
//        super.visitSetField(expression, data)
//        saveVariable(expression.symbol.owner, expression)
//    }

    override fun visitSetValue(expression: IrSetValue, data: T) {
        super.visitSetValue(expression, data)
        saveVariable(expression.symbol.owner, expression)
    }
}