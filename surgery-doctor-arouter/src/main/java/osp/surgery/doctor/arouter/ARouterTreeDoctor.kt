package osp.surgery.doctor.arouter

import com.google.auto.service.AutoService
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import osp.surgery.api.ClassTreeDoctor
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


@AutoService(ClassTreeDoctor::class)
class ARouterTreeDoctor : ClassTreeDoctor() {
    private val JTAG = "surgery"
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
            " # $tag .. keep $router  from  $fileName".sout()
            if (routesClassNames.add(router)) {
                isIncrementalRoutesClassNames.add(router)
            }
            return FilterAction.noTransform
        } else if (fileName == logisticsCenterClass) {
            return FilterAction.transformLast
        }
        return FilterAction.noTransform
    }

    override fun surgery(classNode: ClassNode): ClassNode {
        if (isIncrementalRoutesClassNames.size == 0) {
            " # $tag ====== surgery router size: 0 ======".sout()
            return classNode
        }
        " # $tag ====== surgery router size: ${isIncrementalRoutesClassNames.size} ======".sout()
        classNode.methods.find {
            it.name.equals(loadRouterMap)
        }?.instructions?.let { insn ->
            insn.findAll(Opcodes.RETURN, Opcodes.ATHROW).forEach { ret ->
                insn.insertLogCodeBefore(ret, "e", JTAG, "$logisitscCenter --> visitInsn")
//                insn.insertBefore(ret, LdcInsnNode("Surgery"))
//                insn.insertBefore(ret, LdcInsnNode("$logisitscCenter --> visitInsn"))
//                insn.insertBefore(ret, MethodInsnNode(Opcodes.INVOKESTATIC, "android/util/Log", "i", "(Ljava/lang/String;Ljava/lang/String;)I", false))
                isIncrementalRoutesClassNames.onEach {
                    "# $tag > Transforming $it".sout()
                    insn.insertBefore(ret, LdcInsnNode(it))
                    insn.insertBefore(
                        ret,
                        MethodInsnNode(Opcodes.INVOKESTATIC, logisitscCenter, "register", "(Ljava/lang/String;)V", false)
                    )
                }
            }
        }
        return classNode
    }

    override fun surgeryOver() {
        " # $tag === surgeryOver ==== ${javaClass.name}".sout()
    }
}