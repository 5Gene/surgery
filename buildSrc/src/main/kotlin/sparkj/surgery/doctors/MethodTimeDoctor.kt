package sparkj.surgery.doctors

import groovyjarjarasm.asm.Opcodes
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.tree.*
import sparkj.surgery.JAPI
import sparkj.surgery.JTAG
import sparkj.surgery.doctors.visitor.ClassMethEEVisitor
import sparkj.surgery.more.*
import sparkj.surgery.plan.ClassVisitorDoctor
import java.io.File

/**
 * @author yun.
 * @date 2022/4/2
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */

class MethodTimeDoctor : ClassVisitorDoctor() {

    override fun surgeryPrepare() {
        " # ${this.javaClass.simpleName} === surgeryPrepare ==== ".sout()
    }

    override fun filterByJar(jar: File): FilterAction {
        if (jar.name.contains("arouter-api") || jar.isModuleJar()) {
            return FilterAction.transformNow
        }
        return FilterAction.noTransform
    }

    override fun filterByClassName(file: File, className: () -> String): FilterAction {
        if (file.isJar() && className().endsWith(logisticsCenterClass)) {
            " # ${this.javaClass.simpleName} >> fond LogisticsCenter [in] ${file.path}".sout()
            return FilterAction.transformNow
        } else if (file.name.endsWith("Activity.class")) {
            " # ${this.javaClass.simpleName} >> fond Activity [in] ${file.path}".sout()
            return FilterAction.transformNow
        }
        return FilterAction.noTransform
    }

    override fun surgery(visitor: ClassVisitor): ClassVisitor {
        return ClassTraceAdapter(visitor)
    }

    override fun surgeryOver() {
        " # ${this.javaClass.simpleName} === finishOperate ==== ${javaClass.name}".sout()
    }
}

class ClassTraceAdapter(classVisitor: ClassVisitor) : ClassMethEEVisitor(classVisitor) {
    var timeLocalIndex = 0
    override fun atMethodEnter(mv: MethodVisitor, adapter: AdviceAdapter) {
        mv.visitMethodInsn(AdviceAdapter.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        timeLocalIndex = adapter.newLocal(Type.LONG_TYPE); //这个是LocalVariablesSorter 提供的功能，可以尽量复用以前的局部变量
        mv.visitVarInsn(AdviceAdapter.LSTORE, timeLocalIndex);
    }

    override fun atMethodExit(mv: MethodVisitor, adapter: AdviceAdapter, opcode: Int) {
        mv.visitMethodInsn(AdviceAdapter.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
        mv.visitVarInsn(AdviceAdapter.LLOAD, timeLocalIndex)
        mv.visitInsn(AdviceAdapter.LSUB) //此处的值在栈顶

        mv.visitVarInsn(AdviceAdapter.LSTORE, timeLocalIndex) //因为后面要用到这个值所以先将其保存到本地变量表中


        val stringBuilderIndex = adapter.newLocal(org.objectweb.asm.Type.getObjectType("java/lang/StringBuilder"))
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
        mv.visitInsn(Opcodes.DUP)
//        INVOKESPECIAL java/lang/StringBuilder.<init> ()V
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
        mv.visitVarInsn(Opcodes.ASTORE, stringBuilderIndex) //需要将栈顶的 stringbuilder 保存起来否则后面找不到了

        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
        mv.visitLdcInsn("$className.${adapter.name} time:")
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
        mv.visitInsn(Opcodes.POP)
        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
        mv.visitVarInsn(Opcodes.LLOAD, timeLocalIndex)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
        mv.visitInsn(Opcodes.POP)
        mv.visitLdcInsn(JTAG)
        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false) //注意： Log.d 方法是有返回值的，需要 pop 出去
        mv.visitInsn(Opcodes.POP) //插入字节码后要保证栈的清洁，不影响原来的逻辑，否则就会产生异常，也会对其他框架处理字节码造成影响
    }
}

class TraceMethodAdapter(val className: String, methodVisitor: MethodVisitor?, access: Int, name: String?, descriptor: String?) : AdviceAdapter(
    JAPI, methodVisitor, access, name,
    descriptor
) {
    var timeLocalIndex = 0
    override fun onMethodEnter() {
        super.onMethodEnter()
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        timeLocalIndex = newLocal(Type.LONG_TYPE); //这个是LocalVariablesSorter 提供的功能，可以尽量复用以前的局部变量
        mv.visitVarInsn(LSTORE, timeLocalIndex);
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

    override fun onMethodExit(opcode: Int) {
        super.onMethodExit(opcode)
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false)
        mv.visitVarInsn(LLOAD, timeLocalIndex)
        mv.visitInsn(LSUB) //此处的值在栈顶

        mv.visitVarInsn(LSTORE, timeLocalIndex) //因为后面要用到这个值所以先将其保存到本地变量表中


        val stringBuilderIndex = newLocal(org.objectweb.asm.Type.getObjectType("java/lang/StringBuilder"))
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder")
        mv.visitInsn(Opcodes.DUP)
//        INVOKESPECIAL java/lang/StringBuilder.<init> ()V
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false)
        mv.visitVarInsn(Opcodes.ASTORE, stringBuilderIndex) //需要将栈顶的 stringbuilder 保存起来否则后面找不到了

        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
        mv.visitLdcInsn("$className.$name time:")
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false)
        mv.visitInsn(Opcodes.POP)
        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
        mv.visitVarInsn(Opcodes.LLOAD, timeLocalIndex)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false)
        mv.visitInsn(Opcodes.POP)
        mv.visitLdcInsn(JTAG)
        mv.visitVarInsn(Opcodes.ALOAD, stringBuilderIndex)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "d", "(Ljava/lang/String;Ljava/lang/String;)I", false) //注意： Log.d 方法是有返回值的，需要 pop 出去
        mv.visitInsn(Opcodes.POP) //插入字节码后要保证栈的清洁，不影响原来的逻辑，否则就会产生异常，也会对其他框架处理字节码造成影响
    }
}