package com.spark.transform.doctors

import com.spark.JConstants
import com.spark.helper.FilterAction
import com.spark.sout
import com.spark.transform.ClassTreeDoctor
import org.objectweb.asm.tree.ClassNode
import java.io.File
import com.spark.helper.*
import groovyjarjarasm.asm.Opcodes
import org.objectweb.asm.tree.*
import org.objectweb.asm.tree.InsnList

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
class ArouteDoctor : ClassTreeDoctor {

    private val routesClassNames = mutableListOf<String>()

    override fun operate(classNode: ClassNode): ClassNode {
        " # ${this.javaClass.simpleName} ====== operate router size: ${routesClassNames.size} ======".sout()
        classNode.methods.find {
            it.name.equals(loadRouterMap)
        }?.instructions?.let { insn ->
            insn.findAll(Opcodes.RETURN,Opcodes.ATHROW).forEach { ret ->
                insn.insertBefore(ret, LdcInsnNode("SparkOperation"))
                insn.insertBefore(ret, LdcInsnNode("$logisitscCenter --> visitInsn"))
                insn.insertBefore(ret, MethodInsnNode(Opcodes.INVOKESTATIC, "android/util/Log", "i", "(Ljava/lang/String;Ljava/lang/String;)I", false))
                routesClassNames.onEach {
                    "# ${this.javaClass.simpleName} > Transforming $it".sout()
                    insn.insertBefore(ret, LdcInsnNode(it))
                    insn.insertBefore(ret, MethodInsnNode(Opcodes.INVOKESTATIC, logisitscCenter, "register", "(Ljava/lang/String;)V", false))
                }
            }
        }
        return classNode
    }

    override fun startOperate() {
        
        " # ${this.javaClass.simpleName} === startOperate ==== ${javaClass.name}".sout()
    }

    override fun filterByJar(jar: File): FilterAction {
        if (jar.name.contains("arouter-api") or jar.name.equals(JConstants.moduleClassName)) {
            return FilterAction.transformNow
        }
        return FilterAction.noTransform
    }

    override fun filterByClassName(file: File, className: () -> String): FilterAction {
        if (file.isJar()) {
            val name = className()
            if (name.contains(arouterFilePrefix)) {
                val router = className()
                " # ${this.javaClass.simpleName} .. keep $router  from  ${file.name}".sout()
                routesClassNames.add(router)
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
                routesClassNames.add(router)
                return FilterAction.noTransform
            }
        }
        return FilterAction.noTransform
    }

    override fun finishOperate() {
        " # ${this.javaClass.simpleName} === finishOperate ==== ${javaClass.name}".sout()
    }
}

fun InsnList.findAll(vararg opcodes: Int) = this.filter { it.opcode in opcodes }