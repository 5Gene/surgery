package osp.surgery.doctors.tryfinally

import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import osp.surgery.helper.JAPI
import osp.surgery.helper.isReturn

open class TryFinalMethodAdapter(
    val process: MethodProcess,
    val className: String,
    methodVisitor: MethodVisitor?,
    access: Int,
    name: String?,
    descriptor: String?,
    val hasTryfinalCode: Boolean = false//有try final的代码补充插入try-final会出问题
) :
    AdviceAdapter(JAPI, methodVisitor, access, name, descriptor) {

    private val beforeOriginalCode: Label = Label()
    private val afterOriginalCode: Label = Label()
    val methodName: String by lazy {
        name ?: "method_name"
    }

    override fun visitCode() {
        process.onMethodEnter(className, methodName, mv, this)
        if (hasTryfinalCode) {
            super.visitCode()
        }
        mv.visitTryCatchBlock(
            beforeOriginalCode,
            afterOriginalCode,
            afterOriginalCode,
            null
        )
        mv.visitLabel(beforeOriginalCode)
        super.visitCode()
    }

    override fun visitInsn(opcode: Int) {
        if (opcode.isReturn()) {
            process.onMethodReturn(className, methodName, mv, this)
        }
        super.visitInsn(opcode)

    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        if (hasTryfinalCode) {
            super.visitMaxs(maxStack, maxLocals)
        }
        mv.visitLabel(afterOriginalCode)
        process.onMethodError(className, methodName, mv, this)
        mv.visitInsn(Opcodes.ATHROW)
        super.visitMaxs(maxStack, maxLocals)
    }
}

interface MethodProcess {
    fun onMethodEnter(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter)

    fun onMethodReturn(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter)

    fun onMethodError(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter)
}
