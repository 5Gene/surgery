package trycatch

import helper.isReturn
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type


class MyTryCatchMethodVisitor(
    apiVersion: Int,
    methodVisitor: MethodVisitor,
    private val returnType: Type
) : MethodVisitor(apiVersion, methodVisitor) {

    val label0 = Label()
    val label1 = Label()
    val label2 = Label()
    val label3 = Label()
    val label5 = Label()
    val label6 = Label()
    override fun visitCode() {
        super.visitCode()
        //TRYCATCHBLOCK L0 L1 L2 java/lang/Exception

        // 开始 try 块
        mv.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable")
        mv.visitLabel(label3)
    }

    override fun visitInsn(opcode: Int) {
        if (opcode.isReturn()) {
            mv.visitLabel(label1)
            mv.visitJumpInsn(Opcodes.GOTO, label5)
            mv.visitLabel(label2)
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, arrayOf<Any>("java/lang/Throwable"))
            mv.visitVarInsn(Opcodes.ASTORE, 2)
            mv.visitLabel(label6)
            mv.visitVarInsn(Opcodes.ALOAD, 2)
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false)
            mv.visitLabel(label5)
            mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null)
        }
        super.visitInsn(opcode)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {

//        // 根据返回类型返回默认值
//        when (returnType.sort) {
//            Type.VOID -> {
//                // void 返回类型，不需要返回值
//            }
//
//            Type.BOOLEAN -> {
//                mv.visitInsn(Opcodes.ICONST_0) // boolean 返回 false
//            }
//
//            Type.CHAR -> {
//                mv.visitInsn(Opcodes.ICONST_0) // char 返回 '\u0000'
//            }
//
//            Type.BYTE, Type.SHORT, Type.INT -> {
//                mv.visitInsn(Opcodes.ICONST_0) // int, byte, short 返回 0
//            }
//
//            Type.FLOAT -> {
//                mv.visitInsn(Opcodes.FCONST_0) // float 返回 0.0f
//            }
//
//            Type.LONG -> {
//                mv.visitInsn(Opcodes.LCONST_0) // long 返回 0L
//            }
//
//            Type.DOUBLE -> {
//                mv.visitInsn(Opcodes.DCONST_0) // double 返回 0.0
//            }
//
//            Type.ARRAY, Type.OBJECT -> {
//                mv.visitInsn(Opcodes.ACONST_NULL) // 对象和数组返回 null
//            }
//
//            else -> {
//                throw IllegalArgumentException("Unsupported return type: ${returnType.sort}")
//            }
//        }

        val label7 = Label()
        mv.visitLabel(label7)
        mv.visitLocalVariable("e", "Ljava/lang/Throwable;", null, label6, label5, 2)
        mv.visitLocalVariable("this", "Ltrycatch/TryCatchDemo;", null, label3, label7, 0)
        mv.visitLocalVariable("input", "Ljava/lang/String;", null, label3, label7, 1)

        // 更新 maxStack 和 maxLocals
        super.visitMaxs(maxStack + 2, maxLocals + 1) // +2 for stack size (aload and invokevirtual), +1 for exception local var
        //maxLocals：
        // - 原始方法的本地变量数。
        // - 加上一个新的本地变量用于存储异常对象。
        //  在这个例子中，假设 maxLocals 原来为 N，新的 maxLocals 将是 N + 1。

        //maxStack：
        // - 处理异常时，栈中需要额外的空间来存储异常对象（aload）和调用 printStackTrace 方法（invokevirtual）。
        // - aload 指令需要 1 个位置，invokevirtual 需要额外 1 个位置。
        // - 因此，额外增加 2 个位置。
        //  在这个例子中，假设 maxStack 原来为 M，新的 maxStack 将是 M + 2。
    }
}
