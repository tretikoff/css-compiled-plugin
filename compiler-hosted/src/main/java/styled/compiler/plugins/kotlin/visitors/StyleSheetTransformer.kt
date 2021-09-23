package styled.compiler.plugins.kotlin.visitors

import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class StyleSheetTransformer(private var className: String) : IrElementTransformerVoid() {
}