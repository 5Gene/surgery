package sparkj.doctor

import com.google.auto.service.AutoService
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.commons.AdviceAdapter
import osp.surgery.api.ClassVisitorDoctor
import osp.surgery.api.FilterAction
import osp.surgery.helper.*
import java.io.File
import java.util.*

/**
 * @author yun.
 * @date 2022/5/22
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */
@AutoService(ClassVisitorDoctor::class)
class MethodLogDoctor : ClassVisitorDoctor() {
    override fun filterByClassName(file: File, className: () -> String): FilterAction {
        return FilterAction.transformNow
    }

    override fun filterByJar(jar: File): FilterAction {
        return FilterAction.transformNow
    }

    override fun surgery(visitor: ClassVisitor): ClassVisitor {
        return MethodLogClassVisitor(visitor)
    }

    override fun surgeryOver() {
    }

    override fun surgeryPrepare() {
    }
}

class MethodLogClassVisitor(classVisitor: ClassVisitor) : ClassVisitor(JAPI, classVisitor) {

    var className: String? = null

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (access.isMethodIgnore()) {
            return visitMethod
        }
        val methodName = Objects.toString(name, "method_name")
        return object : AdviceAdapter(JAPI, visitMethod, access, name, descriptor) {
            override fun onMethodEnter() {
                mv.addLogCode("jonas", "-- $className > $methodName --")
            }
        }
    }
}