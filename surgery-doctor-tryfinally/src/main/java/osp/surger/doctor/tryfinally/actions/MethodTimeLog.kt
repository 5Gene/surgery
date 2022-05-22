package ospl.surgery.doctors.tryfinally.actions

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import osp.surger.doctor.tryfinally.TryFinally

/**
 * @author yun.
 * @date 2022/4/28
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
class MethodTimeLog : TryFinally {

    val timeLocalIndex: ThreadLocal<Int> = ThreadLocal()

    override fun methodEnter(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        mv.visitMethodInsn(AdviceAdapter.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        val newLocal = adapter.newLocal(Type.LONG_TYPE)
        timeLocalIndex.set(newLocal); //这个是LocalVariablesSorter 提供的功能，可以尽量复用以前的局部变量
        mv.visitVarInsn(AdviceAdapter.LSTORE, newLocal);
    }
    override fun methodExit(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
//        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "kotlin/lang/System", "currentTimeMillis", "()J", false)
//        mv.visitVarInsn(org.objectweb.asm.Opcodes.LLOAD, timeLocalIndex.get())
//        mv.visitInsn(org.objectweb.asm.Opcodes.LSUB)
//        val index: Int = adapter.newLocal(Type.LONG_TYPE)
//        mv.visitVarInsn(org.objectweb.asm.Opcodes.LSTORE, index)
//        mv.visitLdcInsn("$className -> $methodName")
//        mv.visitVarInsn(org.objectweb.asm.Opcodes.LLOAD, index)
//        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "spark/surgery/TimeLog", "mTime", "(Ljava/lang/String;J)V", false)

        mv.visitMethodInsn(AdviceAdapter.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
        val index = timeLocalIndex.get()
        mv.visitVarInsn(AdviceAdapter.LLOAD, index)
        mv.visitInsn(AdviceAdapter.LSUB) //此处的值在栈顶
        mv.visitVarInsn(AdviceAdapter.LSTORE, index) //因为后面要用到这个值所以先将其保存到本地变量表中
        val stringBuilderIndex = adapter.newLocal(org.objectweb.asm.Type.getObjectType("java/lang/StringBuilder"))
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
        mv.visitInsn(Opcodes.DUP)
//        INVOKESPECIAL kotlin/lang/StringBuilder.<init> ()V
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
        mv.visitVarInsn(Opcodes.ASTORE, stringBuilderIndex) //需要将栈顶的 stringbuilder 保存起来否则后面找不到了

        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
        mv.visitLdcInsn("$className -> ${adapter.name} time:")
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
        mv.visitInsn(Opcodes.POP)
        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
        mv.visitVarInsn(Opcodes.LLOAD, index)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
        mv.visitInsn(Opcodes.POP)
        mv.visitLdcInsn(JTAG)
        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "w", "(Ljava/lang/String;Ljava/lang/String;)I", false) //注意： Log.d 方法是有返回值的，需要 pop 出去
        mv.visitInsn(Opcodes.POP) //插入字节码后要保证栈的清洁，不影响原来的逻辑，否则就会产生异常，也会对其他框架处理字节码造成影响
    }
}

const val JTAG = "surgery.tryfinal"