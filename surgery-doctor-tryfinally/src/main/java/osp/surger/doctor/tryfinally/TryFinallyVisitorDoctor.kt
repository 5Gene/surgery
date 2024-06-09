package osp.surger.doctor.tryfinally

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import osp.surgery.api.ClassVisitorDoctor
import osp.surgery.api.FilterAction
import osp.surgery.helper.*
import java.io.File
import java.lang.reflect.Modifier
import java.util.*

/**
 * @author yun.
 * @date 2022/4/2
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */

open class TryFinallyVisitorDoctor : ClassVisitorDoctor() {

    override fun surgeryPrepare() {
        " # $tag === surgeryPrepare ==== ".sout()
    }

    override fun filterByJar(jar: File): FilterAction {
        val name = jar.name
        if (name.contains("TraceCompat") || name.contains("Trace")) {
            "$tag >> ignore $name".sout()
            return FilterAction.noTransform
        }
        return FilterAction.transformNow
    }

    override fun filterByClassName(fileName: String, compileClassName: String): FilterAction {
        return FilterAction.transformNow
    }

    override fun surgery(visitor: ClassVisitor): ClassVisitor {
        return TryFinallyAdapter(visitor)
    }

    override fun surgeryOver() {
        " # $tag === surgery over ==== ".sout()
    }

    open fun onMethodEnter(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        val tag = "$className -> $methodName".let {
            print(it)
            it.substring(0.coerceAtLeast(it.length - 126))
        }
        mv.visitLdcInsn(tag)
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "android/os/Trace", "beginSection", "(Ljava/lang/String;)V", false)
    }

    open fun onMethodReturn(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        mv.addLogCode(className, "normal finish -> $methodName")
        onMethodExit(className, methodName, mv, adapter)
    }

    open fun onMethodError(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        mv.addLogCode(className, "finally on error -> $methodName")
        onMethodExit(className, methodName, mv, adapter)
    }

    open fun onMethodExit(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "android/os/Trace", "endSection", "()V", false)
    }

    inner class TryFinallyAdapter(classVisitor: ClassVisitor) : ClassVisitor(JAPI, classVisitor) {

        var className = ""
        var isInterface = false

        override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
            super.visit(version, access, name, signature, superName, interfaces)
            className = name ?: "className"
            isInterface = Modifier.isInterface(access)
        }

        override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
            if (isInterface || access.isMethodIgnore()) {
                return visitMethod
            }
            val methodName = Objects.toString(name, "method_name")
            return object : AdviceAdapter(JAPI, visitMethod, access, name, descriptor) {
                private val beforeOriginalCode: Label = Label()
                private val afterOriginalCode: Label = Label()
                override fun visitCode() {
                    onMethodEnter(className, methodName, mv, this)
                    mv.visitTryCatchBlock(
                        beforeOriginalCode,
                        afterOriginalCode,
                        afterOriginalCodv.visitLabel(beforeOriginalCode)
                    super.visitCode()
                }

                override fun visitInsn(opcode: Int) {
                    if (opcode.isReturn()) {
                        onMethodReturn(className, methodName, mv, this)
                    }
                    super.visitInsn(opcode)

                }

                override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                    mv.visitLabel(afterOriginalCode)
                    onMethodError(className, methodName, mv, this)
                    mv.visitInsn(Opcodes.ATHROW)
                    super.visitMaxs(maxStack, maxLocals)
                }
            }
        }
    }
}

interface TryFinally{
    fun methodEnter(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter)
    fun methodExit(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter)
}