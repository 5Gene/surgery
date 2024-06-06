package osp.surgery.doctors.tree

import com.google.auto.service.AutoService
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import osp.surgery.api.ClassTreeDoctor
import osp.surgery.api.FilterAction
import osp.surgery.doctors.tryfinally.MethodProcess
import osp.surgery.doctors.tryfinally.TryFinalMethodAdapter
import osp.surgery.doctors.tryfinally.TryFinally
import osp.surgery.doctors.tryfinally.actions.MethodTimeLog
import osp.surgery.doctors.tryfinally.actions.MethodTrace
import osp.surgery.helper.JAPI
import osp.surgery.helper.filterJar
import osp.surgery.helper.isMethodIgnore
import osp.surgery.helper.sout
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
        " # $tag === surgeryPrepare ==== ".sout()
    }

    override fun filterByJar(jar: File): FilterAction {
        if (jar.filterJar()) {
            val name = jar.name
            "$tag >> ignore $name".sout()
            return FilterAction.noTransform
        }
        return FilterAction.transformNow
    }

    override fun filterByClassName(fileName: String, compileClassName: String): FilterAction {
        return FilterAction.transformNow
    }

    override fun surgeryOver() {
        " # $tag === surgeryOver ==== ".sout()
    }

    override fun surgery(classNode: ClassNode): ClassNode {
        classNode.methods.replaceAll { method ->
            try {
                if (method.isMethodIgnore() || method.instructions == null || !(method.instructions.any { it is MethodInsnNode })) {
                    "$tag > skip method ${method.name}".sout()
                    method
                } else {
                    val newMethod = MethodNode(
                        JAPI,
                        method.access,
                        method.name,
                        method.desc,
                        method.signature,
                        method.exceptions.toTypedArray()
                    )
                    val adapter = TryFinalMethodAdapter(
                        this,
                        classNode.name,
                        newMethod,
                        method.access,
                        method.name,
                        method.desc,
                        (method.tryCatchBlocks?.size ?: 0) > 0
                    )
                    method.accept(adapter)
                    newMethod
                }
            } catch (e: Exception) {
                "$tag > transform method error ${method.name} ==> ${e.message}".sout()
                method
            }
        }
        return classNode
    }

    override fun onMethodEnter(
        className: String,
        methodName: String,
        mv: MethodVisitor,
        adapter: AdviceAdapter
    ) {
        enterActions.forEach {
            it.methodEnter(className, methodName, mv, adapter)
        }
    }

    override fun onMethodReturn(
        className: String,
        methodName: String,
        mv: MethodVisitor,
        adapter: AdviceAdapter
    ) {
        onMethodExit(className, methodName, mv, adapter)
    }

    override fun onMethodError(
        className: String,
        methodName: String,
        mv: MethodVisitor,
        adapter: AdviceAdapter
    ) {
        onMethodExit(className, methodName, mv, adapter)
    }

    private fun onMethodExit(
        className: String,
        methodName: String,
        mv: MethodVisitor,
        adapter: AdviceAdapter
    ) {
        exitActions.forEach {
            it.methodExit(className, methodName, mv, adapter)
        }
    }
}
