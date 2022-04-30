package ospl.surgery.doctors.tree

import com.google.auto.service.AutoService
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import java.io.File
import org.objectweb.asm.tree.*
import ospl.surgery.api.ClassTreeDoctor
import ospl.surgery.api.FilterAction
import ospl.surgery.helper.*

/**
 * @author yun.
 * @date 2022/4/2
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */

const val loadRouterMap = "loadRouterMap"
const val arouterFilePrefix = "ARouter$$"
const val logisticsCenterClass = "LogisticsCenter"
const val logisitscCenter = "com/alibaba/android/arouter/core/LogisticsCenter"
const val JTAG = "surgery"

//@AutoService(ClassTreeDoctor::class)
class ArouteDoctor : ClassTreeDoctor() {
    //需要缓存起来 然后读取 过滤重复
    private val routesClassNames = mutableSetOf<String>()
    @Transient
    private val isIncrementalRoutesClassNames = mutableSetOf<String>()

    override fun toString(): String {
        if(routesClassNames.isEmpty()){
            return super.toString()
        }
        return "${super.toString()}>$routesClassNames"
    }

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
        if (file.isJar()) {
            val name = className()
            if (name.contains(arouterFilePrefix)) {
                val router = className()
                println(routesClassNames)
                " # ${this.javaClass.simpleName} .. keep $router  from  ${file.name}".sout()
                if (routesClassNames.add(router)) {
                    isIncrementalRoutesClassNames.add(router)
                }
                return FilterAction.noTransform
            } else if (name.endsWith(logisticsCenterClass)) {
                " # ${this.javaClass.simpleName} >> fond LogisticsCenter [in] ${file.path}".sout()
                return FilterAction.transformLast
            }
        } else {
            val name = file.name
            if (name.contains(arouterFilePrefix)) {
                val router = className()
                " # ${this.javaClass.simpleName} .. keep $router  from  ${file.name}".sout()
                if (routesClassNames.add(router)) {
                    isIncrementalRoutesClassNames.add(router)
                }
                return FilterAction.noTransform
            }
        }
        return FilterAction.noTransform
    }

    override fun surgery(classNode: ClassNode): ClassNode {
        if (isIncrementalRoutesClassNames.size == 0) {
            " # ${this.javaClass.simpleName} ====== surgery router size: 0 ======".sout()
            return classNode
        }
        " # ${this.javaClass.simpleName} ====== surgery router size: ${isIncrementalRoutesClassNames.size} ======".sout()
        classNode.methods.find {
            it.name.equals(loadRouterMap)
        }?.instructions?.let { insn ->
            insn.findAll(Opcodes.RETURN,Opcodes.ATHROW).forEach { ret ->
                insn.insertLogCodeBefore(ret, JTAG,"$logisitscCenter --> visitInsn")
//                insn.insertBefore(ret, LdcInsnNode("Surgery"))
//                insn.insertBefore(ret, LdcInsnNode("$logisitscCenter --> visitInsn"))
//                insn.insertBefore(ret, MethodInsnNode(Opcodes.INVOKESTATIC, "android/util/Log", "i", "(Ljava/lang/String;Ljava/lang/String;)I", false))
                isIncrementalRoutesClassNames.onEach {
                    "# ${this.javaClass.simpleName} > Transforming $it".sout()
                    insn.insertBefore(ret, LdcInsnNode(it))
                    insn.insertBefore(ret, MethodInsnNode(Opcodes.INVOKESTATIC, logisitscCenter, "register", "(Ljava/lang/String;)V", false))
                }
            }
        }
        return classNode
    }

    override fun surgeryOver() {
        " # ${this.javaClass.simpleName} === surgeryOver ==== ${javaClass.name}".sout()
    }
}