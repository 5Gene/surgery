package sparkj.surgery.doctors

import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import sparkj.surgery.JAPI
import sparkj.surgery.more.*
import sparkj.surgery.plan.ClassTreeDoctor
import java.io.File

class MethodTraceDoctor : ClassTreeDoctor {

    override fun surgeryPrepare() {
        " # ${this.javaClass.simpleName} === surgeryPrepare ==== ".sout()
    }

    override fun filterByJar(jar: File): FilterAction {
        if (jar.name.contains("arouter-api") or jar.isModuleJar()) {
            return FilterAction.transformNow
        }
        return FilterAction.noTransform
    }

    override fun filterByClassName(file: File, className: () -> String): FilterAction {
        if (file.name.endsWith("Activity.class")) {
            return FilterAction.transformNow
        }else if (file.isJar()) {
            return FilterAction.transformNow
        }
        return FilterAction.noTransform
    }

    override fun surgery(classNode: ClassNode): ClassNode {
        classNode.methods.replaceAll {
            if (it.name == "<init>") {
                it
            } else {
                val newMethodNode = it.copy()
                it.accept(TraceAdapter(classNode.name, newMethodNode))
                newMethodNode
            }
        }
        return classNode
    }

    override fun surgeryOver() {
        " # ${this.javaClass.simpleName} === finishOperate ==== ${javaClass.name}".sout()
    }
}

//class TraceMethodNode(val node: MethodNode) :
//    MethodNode(JAPI, node.access, name, descriptor, signature, exceptions) {
//    init {
//        node.accept(this)
//    }
//}

class TraceAdapter(val className: String, mn: MethodNode) : AdviceAdapter(JAPI, mn, mn.access, mn.name, mn.desc) {

    override fun onMethodEnter() {
        mv.visitLdcInsn("TraceMethodNode > $className->$name")
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "android/os/Trace", "beginSection", "(Ljava/lang/String;)V", false)

    }

    override fun onMethodExit(opcode: Int) {
        super.onMethodExit(opcode)
        mv.addLogCode("TraceMethodNode", "TraceAdapter --> onMethodExit")
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "android/os/Trace", "endSection", "()V", false)
    }
}