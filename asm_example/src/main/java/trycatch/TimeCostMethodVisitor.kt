package trycatch

import helper.isFinish
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes


class TimeCostMethodVisitor(
    apiVersion: Int,
    val classMethodName: String,
    methodVisitor: MethodVisitor,
) : MethodVisitor(apiVersion, methodVisitor) {

    val start_time_long_store = 2
    var increasedStack = 0
    var increasedLocals = 0
    override fun visitCode() {
        super.visitCode()
//        val start = System.currentTimeMillis()
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
        mv.visitVarInsn(Opcodes.LSTORE, 1)//当前时间放入本地变量表
        increasedLocals += 2
        increasedStack += 1
    }

    var varIndexLast = 0

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
        if (varIndex > 0) {
            varIndexLast = varIndex + start_time_long_store
            super.visitVarInsn(opcode, varIndexLast)
        } else {
            super.visitVarInsn(opcode, varIndex)
        }
    }

    override fun visitInsn(opcode: Int) {
        if (opcode.isFinish()) {
            //方法结束前 计算时间并输出

            //让我们回顾一下 INVOKESTATIC 指令的作用：
            //从操作数栈弹出方法的参数。 参数的数量和类型由方法签名决定。
            //调用指定的静态方法。
            //将方法的返回值（如果有）压入操作数栈。
            //关键点： INVOKESTATIC 指令本身不会向栈中压入任何值，它只是调用一个方法， 方法的返回值才会影响栈的深度。

            //INVOKESTATIC 指令会调用静态方法，方法的参数从操作数栈中弹出， 返回值（ 如果有） 压入操作数栈。
            //在这个例子中，currentTimeMillis() 方法没有参数，返回值为 long 类型。
            //因此，maxStack 的净变化为 +2（没有弹出值，压入一个 long 类型值）。
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
            increasedStack += 2
            //作用： 将局部变量表索引为 1 的 long 类型值加载到操作数栈顶。
            //对 maxStack 的影响： LLOAD 指令会将两个值（long 类型占两个槽位）压入栈，因此 maxStack 增加 2。
            //对 maxLocals 的影响： LLOAD 指令不会影响 maxLocals。
            mv.visitVarInsn(Opcodes.LLOAD, 1)
            increasedLocals += 2
            //对操作数栈的影响：
            //弹出栈顶的两个 long 值（被减数和减数），每个 long 值占两个槽位，共弹出 4 个槽位。
            //将计算结果（一个 long 值）压入栈顶，占 2 个槽位。
            //因此，maxStack 的净变化为 -2 （-4 + 2 = -2）
            mv.visitInsn(Opcodes.LSUB)
            increasedStack -= 2
            //作用： 将操作数栈顶的 long 类型值弹出，并将其存储到局部变量表索引为 2 的位置。
            //对 maxStack 的影响： LSTORE 指令会弹出栈顶的两个值（long 类型占两个槽位），因此 maxStack 减少 2。
            //对 maxLocals 的影响： 如果局部变量表索引 2 之前没有被使用过，那么 maxLocals 需要增加 2（long 类型占两个槽位）。如果索引 2 已经被使用，则 maxLocals 不变。
            val costIndex = varIndexLast + 2
            mv.visitVarInsn(Opcodes.LSTORE, costIndex)
            increasedLocals -= 2
//            mv.visitVarInsn(Opcodes.LLOAD, varIndexLast)
//            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
//            mv.visitInsn(Opcodes.SWAP)
//            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false)
            //作用： 创建一个 StringBuilder 对象，并将对象的引用压入操作数栈。
            //对 maxStack 的影响： NEW 指令会将一个引用压入栈，因此 maxStack 增加 1。
            //对 maxLocals 的影响： NEW 指令不会影响 maxLocals。
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
            increasedStack++
            //作用： 复制操作数栈顶的值（即 StringBuilder 对象的引用），并将其压入栈顶。
            //对 maxStack 的影响： DUP 指令会复制栈顶的值，因此 maxStack 增加 1。
            //对 maxLocals 的影响： DUP 指令不会影响 maxLocals。
            mv.visitInsn(Opcodes.DUP)
            increasedStack++
            //作用： 调用 StringBuilder 对象的构造函数 <init>()，初始化对象。
            //对 maxStack 的影响： INVOKESPECIAL 指令会弹出栈顶的一个值（StringBuilder 对象的引用），因此 maxStack 减少 1。
            //对 maxLocals 的影响： INVOKESPECIAL 指令不会影响 maxLocals。
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
            increasedStack -= 1

            val stringBuilderIndex = costIndex + 2
            mv.visitVarInsn(Opcodes.ASTORE, stringBuilderIndex) //需要将栈顶的 stringbuilder 保存起来否则后面找不到了
            mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)

            mv.visitLdcInsn("$classMethodName -> time:")
            increasedStack++
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
            increasedStack--
            mv.visitInsn(Opcodes.POP)
            increasedStack--
            mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
            increasedStack++
            mv.visitVarInsn(Opcodes.LLOAD, costIndex)
            increasedStack += 2
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
            increasedStack -= 2
            mv.visitInsn(Opcodes.POP)
            increasedStack--
            mv.visitLdcInsn("JTAG")
            increasedStack++
            mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
            increasedStack++
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
            increasedStack--
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "w", "(Ljava/lang/String;Ljava/lang/String;)I", false) //注意： Log.d 方法是有返回值的，需要 pop 出去
            mv.visitInsn(Opcodes.POP) //插入字节码后要保证栈的清洁，不影响原来的逻辑，否则就会产生异常，也会对其他框架处理字节码造成影响
            increasedStack--
        }
        super.visitInsn(opcode)
    }

//    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
//        println("$increasedStack--$increasedLocals")
//        super.visitMaxs(maxStack + increasedStack, maxLocals + increasedLocals)
//    }

}
