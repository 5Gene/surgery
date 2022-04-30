package zu.yun.surgery.doctors.tree

import com.google.auto.service.AutoService
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import ospl.sparkj.surgery.api.ClassTreeDoctor
import ospl.sparkj.surgery.api.FilterAction
import ospl.surgery.helper.JAPI
import ospl.surgery.helper.filterJar
import ospl.surgery.helper.isMethodIgnore
import ospl.surgery.helper.sout
import zu.yun.surgery.doctors.tryfinally.MethodProcess
import zu.yun.surgery.doctors.tryfinally.TryFinalMethodAdapter
import zu.yun.surgery.doctors.tryfinally.TryFinally
import zu.yun.surgery.doctors.tryfinally.actions.MethodTimeLog
import zu.yun.surgery.doctors.tryfinally.actions.MethodTrace
import java.io.File

@AutoService(ClassTreeDoctor::class)
open class TryFinallyDoctor : ClassTreeDoctor(), MethodProcess {
    private val enterActions: List<TryFinally> by lazy {
        configMethodActions()
    }

    private val exitActions: List<TryFinally> by lazy {
        enterActions.reversed()
    }

    open fun configMethodActions(): List<TryFinally> = listOf<TryFinally>(MethodTrace(), MethodTimeLog())

    override fun surgeryPrepare() {
        " # ${this.javaClass.simpleName} === surgeryPrepare ==== ".sout()
    }

    override fun filterByJar(jar: File): FilterAction {
        if (jar.filterJar()) {
            return FilterAction.noTransform
        }
        return FilterAction.transformNow
    }

    override fun filterByClassName(file: File, className: () -> String): FilterAction {
        return FilterAction.transformNow
    }

    override fun surgeryOver() {
        " # ${this.javaClass.simpleName} === surgeryOver ==== ".sout()
    }

    override fun surgery(classNode: ClassNode): ClassNode {
        classNode.methods.replaceAll { method ->
            try {
                if (method.isMethodIgnore() || method.instructions == null || !(method.instructions.any { it is MethodInsnNode })) {
                    println(method.name)
                    method
                } else {
                    val newMethod = MethodNode(JAPI, method.access, method.name, method.desc, method.signature, method.exceptions.toTypedArray())
                    val adapter = TryFinalMethodAdapter(this, classNode.name, newMethod, method.access, method.name, method.desc)
                    method.accept(adapter)
                    newMethod
                }
            } catch (e: Exception) {
                println("${method.name} ==> ${e.message}")
                method
            }
        }
        return classNode
    }

    override fun onMethodEnter(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        enterActions.forEach {
            it.methodEnter(className, methodName, mv, adapter)
        }
    }

    override fun onMethodReturn(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        onMethodExit(className, methodName, mv, adapter)
    }

    override fun onMethodError(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        onMethodExit(className, methodName, mv, adapter)
    }

    private fun onMethodExit(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        exitActions.forEach {
            it.methodExit(className, methodName, mv, adapter)
        }
    }
}
