package osp.surgery.doctor.arouter

import com.google.auto.service.AutoService
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import osp.surgery.api.ClassVisitorDoctor
import osp.surgery.api.FilterAction
import osp.surgery.helper.*
import java.io.File

/**
 * @author yun.
 * @date 2022/4/2
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */
@AutoService(ClassVisitorDoctor::class)
class ARouterVisitorDoctor : ClassVisitorDoctor() {

    private val loadRouterMap = "loadRouterMap"
    private val arouterFilePrefix = "ARouter$$"
    private val logisticsCenterClass = "LogisticsCenter.class"
    private val logisitscCenter = "com/alibaba/android/arouter/core/LogisticsCenter"

    //需要缓存起来 然后读取 过滤重复
    private val routesClassNames = mutableSetOf<String>()

    @Transient
    private val isIncrementalRoutesClassNames = mutableSetOf<String>()

    override fun toString(): String {
        if (routesClassNames.isEmpty()) {
            return super.toString()
        }
        return "${super.toString()}>$routesClassNames"
    }

    override fun surgeryPrepare() {
        " # $tag === surgeryPrepare ==== ".sout()
    }

    override fun filterByJar(jar: File): FilterAction {
        if (jar.name.contains("arouter-api") or jar.isModuleJar()) {
            return FilterAction.transformNow
        }
        return FilterAction.noTransform
    }

    override fun filterByClassName(fileName: String, compileClassName: String): FilterAction {
        if (fileName.startsWith(arouterFilePrefix)) {
            val router = compileClassName.className()
            " # $tag .. keep > $router  from  $fileName".sout()
            if (routesClassNames.add(router)) {
                isIncrementalRoutesClassNames.add(router)
            }
            return FilterAction.noTransform
        } else if (fileName == logisticsCenterClass) {
            return FilterAction.transformLast
        }
        return FilterAction.noTransform
    }

    override fun surgery(visitor: ClassVisitor): ClassVisitor {
        if (isIncrementalRoutesClassNames.size == 0) {
            " # $tag ====== surgery router size: 0 ======".sout()
            return visitor
        }
        " # $tag ====== surgery router size: ${isIncrementalRoutesClassNames.size} ======".sout()
        return object : ClassVisitor(JAPI, visitor) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
                if (name == loadRouterMap) {
                    //把loadRouterMap()方法换成新方法
                    return object : MethodVisitor(JAPI) {
                        override fun visitCode() {
                            super.visitCode()
                            isIncrementalRoutesClassNames.forEach {
                                " # $tag ====== surgery router register: $it ======".sout()
                                visitMethod.visitLdcInsn(it)
                                visitMethod.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    logisitscCenter,
                                    "register",
                                    "(Ljava/lang/String;)V",
                                    false
                                )
                            }
                            visitMethod.visitInsn(Opcodes.RETURN)
                            visitMethod.visitMaxs(1, 1)
                            visitMethod.visitEnd()
                        }
                    }
                }

                return visitMethod
            }
        }
    }

    override fun surgeryOver() {
        " # $tag === surgeryOver ==== ${javaClass.name}".sout()
    }
}