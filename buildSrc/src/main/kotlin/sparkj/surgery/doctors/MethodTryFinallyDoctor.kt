package sparkj.surgery.doctors

import groovyjarjarasm.asm.Opcodes
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter
import sparkj.surgery.JTAG
import sparkj.surgery.doctors.visitor.ClassMethEEVisitor
import sparkj.surgery.more.FilterAction
import sparkj.surgery.more.isJar
import sparkj.surgery.more.isModuleJar
import sparkj.surgery.more.sout
import sparkj.surgery.plan.ClassVisitorDoctor
import java.io.File

/**
 * @author yun.
 * @date 2022/4/2
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */

class MethodTryFinallyDoctor : ClassVisitorDoctor() {

    override fun surgeryPrepare() {
        " # ${this.javaClass.simpleName} === surgeryPrepare ==== ".sout()
    }

    override fun filterByJar(jar: File): FilterAction {
        if (jar.name.contains("arouter-api") || jar.isModuleJar()) {
            return FilterAction.transformNow
        }
        return FilterAction.transformNow
    }

    override fun filterByClassName(file: File, className: () -> String): FilterAction {
        if(file.name.endsWith("Activity.class")) {
            " # ${this.javaClass.simpleName} >> fond Activity [in] ${file.path}".sout()
            return FilterAction.transformNow
        }
        return FilterAction.noTransform
    }

    override fun surgery(visitor: ClassVisitor): ClassVisitor {
        return TryFinallyAdapter(visitor)
    }

    override fun surgeryOver() {
        " # ${this.javaClass.simpleName} === finishOperate ==== ${javaClass.name}".sout()
    }

    fun onTryEnter(className:String,methodName:String,mv: MethodVisitor, adapter: AdviceAdapter) {

    }

    fun onFinallyScope(className:String,methodName:String,mv: MethodVisitor, adapter: AdviceAdapter) {

    }

    inner class TryFinallyAdapter(classVisitor: ClassVisitor) : ClassMethEEVisitor(classVisitor) {
        override fun atMethodEnter(mv: MethodVisitor, adapter: AdviceAdapter) {
            mv.visitLdcInsn("TraceMethodNode > $className->${adapter.name}")
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "android/os/Trace", "beginSection", "(Ljava/lang/String;)V", false)

        }

        override fun atMethodExit(mv: MethodVisitor, adapter: AdviceAdapter, opcode: Int) {
            mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "android/os/Trace", "endSection", "()V", false)
        }
    }

}