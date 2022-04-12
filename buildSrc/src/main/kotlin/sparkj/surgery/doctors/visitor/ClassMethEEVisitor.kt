package sparkj.surgery.doctors.visitor

import groovyjarjarasm.asm.Opcodes
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import sparkj.surgery.JAPI

/**
 * @author yun.
 * @date 2022/4/9
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
abstract class ClassMethEEVisitor(classVisitor: ClassVisitor) : ClassVisitor(JAPI, classVisitor) {
    var className = ""

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name ?: "no_name"
    }

    override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
        return TraceMethodAdapter(className, visitMethod, access, name, descriptor)
    }

    abstract fun atMethodEnter(mv: MethodVisitor, adapter: AdviceAdapter)
    abstract fun atMethodExit(mv: MethodVisitor, adapter: AdviceAdapter, opcode: Int)

    inner class TraceMethodAdapter(val className: String, methodVisitor: MethodVisitor?, access: Int, name: String?, descriptor: String?) :
        AdviceAdapter(JAPI, methodVisitor, access, name, descriptor) {

        private val labelTry = Label()
        private val labelReturn = Label()
        private val labelFinally = Label()
        private var varIndex = 0

        override fun onMethodEnter() {
            mv.visitTryCatchBlock(labelTry, labelReturn, labelFinally, null)
            mv.visitLabel(labelTry)
            atMethodEnter(mv, this)
            //try开始
        }

        override fun visitTypeInsn(opcode: Int, type: String?) {
            super.visitTypeInsn(opcode, type)
            //创建类型
        }

        override fun visitMethodInsn(opcodeAndSource: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
            super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface)
            //调用方法
            if (opcodeAndSource >= Opcodes.INVOKEVIRTUAL && opcodeAndSource <= INVOKEDYNAMIC) {
                println("${this.className} = $opcodeAndSource $owner $name $descriptor")
            }
        }

        override fun visitVarInsn(opcode: Int, varIndex: Int) {
            this.varIndex = varIndex
            super.visitVarInsn(opcode, varIndex)
        }

        override fun visitInsn(opcode: Int) {
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                mv.visitLabel(labelReturn)
                atMethodExit(mv, this, opcode)
            }
            super.visitInsn(opcode)

        }

        override fun onMethodExit(opcode: Int) {

        }

        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            mv.visitLabel(labelFinally)
            mv.visitFrame(Opcodes.F_SAME1,0,null,1, arrayOf("java/lang/Exception"))
            mv.visitVarInsn(Opcodes.ASTORE,0)
            atMethodExit(mv, this, 0)
            mv.visitVarInsn(Opcodes.ALOAD,0)
            mv.visitInsn(Opcodes.ATHROW)
            super.visitMaxs(maxStack, maxLocals)
        }

        override fun visitFrame(type: Int, numLocal: Int, local: Array<out Any>?, numStack: Int, stack: Array<out Any>?) {
            super.visitFrame(type, numLocal, local, numStack, stack)
        }

        fun visitFrame2(type: Int, numLocal: Int, local: Array<out Any>?, numStack: Int, stack: Array<out Any>?) {
            mv.visitFrame(type, numLocal, local, numStack, stack)
        }
    }
}