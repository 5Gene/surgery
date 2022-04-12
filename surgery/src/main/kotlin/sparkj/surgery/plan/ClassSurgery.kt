package sparkj.surgery.plan

import com.android.build.api.transform.Status
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import sparkj.surgery.more.*
import sparkj.surgery.or.OperatingRoom
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author yun.
 * @date 2022/4/8
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */


interface ClassSurgery {
    fun surgeryPrepare()
    fun filterByJar(jar: File): FilterAction
    fun filterByClassName(src: File, dest: File, isJar: Boolean, fileName: String, status: Status, className: () -> String): FilterAction
    fun surgery(classFileByte: ByteArray): ByteArray
    fun surgeryOver()
}

abstract class ClassSurgeryImpl<DOCTOR : ClassDoctor> : ClassSurgery {
    private val lastProcessedFiles = CopyOnWriteArrayList<LastFile<DOCTOR>>()
    val doctors = mutableListOf<DOCTOR>()
    private val localLastFile = ThreadLocal<LastFile<DOCTOR>>()
    private val chiefDoctors = ThreadLocal<List<DOCTOR>>()
    private val or = OperatingRoom()

    override fun surgeryPrepare() {
        localLastFile.set(null)
        chiefDoctors.set(null)
        lastProcessedFiles.clear()
        doctors.forEach {
            it.surgeryPrepare()
        }
    }

    override fun filterByJar(jar: File): FilterAction {
        val grouped = doctors.groupBy {
            it.filterByJar(jar)
        }
        if (!grouped[FilterAction.transformLast].isNullOrEmpty()) {
            return FilterAction.transformLast
        } else if (grouped[FilterAction.transformNow].isNullOrEmpty()) {
            return FilterAction.noTransform
        }
        return FilterAction.transformNow
    }

    /**
     * 一个线程处理一个jar 这个方式是jar遍历jarEntry的时候执行的
     */
    override fun filterByClassName(src: File, dest: File, isJar: Boolean, fileName: String, status: Status, className: () -> String): FilterAction {
        var result = FilterAction.noTransform
//        if (status != Status.ADDED) {
//            return result
//        }
        if (isJar) {
            val grouped = doctors.groupBy {
                it.filterByClassName(src, className)
            }
            val nowGroup = grouped[FilterAction.transformNow]
            val lastGroup = grouped[FilterAction.transformLast]
            if (nowGroup.isNullOrEmpty() && lastGroup.isNullOrEmpty()) {
                return FilterAction.noTransform
            }
            if (!lastGroup.isNullOrEmpty()) {
                collectLastWorker(dest, fileName, lastGroup)
                " # ${this.javaClass.simpleName} >>> fond jar file to last : ${src.name} >> doctors : $lastGroup".sout()
                result = FilterAction.transformLast
            }
            if (nowGroup.isNullOrEmpty()) {
                //这里没有 现在就要处理的 那么就全部是以后处理不需要转换
                return result
            }
            chiefDoctors.set(nowGroup)
            if (result == FilterAction.transformLast) {
                return FilterAction.transformNowLast
            }
        } else {
            val grouped = doctors.groupBy {
                it.filterByClassName(src, className)
            }
            val nowGroup = grouped[FilterAction.transformNow]
            val lastGroup = grouped[FilterAction.transformLast]
            if (!lastGroup.isNullOrEmpty()) {
                //只要有未来要处理的就不执行 以后在执行  以后执行的时候要把现在执行的也执行
                val lastWorkers = if (nowGroup.isNullOrEmpty()) lastGroup else nowGroup + lastGroup
                lastProcessedFiles.add(LastFile(dest, mutableMapOf(fileName to lastWorkers), jar = false))
                " # ${this.javaClass.simpleName} >>> fond class file to last : ${src.name} >> doctors : $lastWorkers".sout()
                return FilterAction.noTransform
            }
            if (nowGroup.isNullOrEmpty()) {
                return FilterAction.noTransform
            }
            chiefDoctors.set(nowGroup)
        }
        return FilterAction.transformNow
    }

    private fun collectLastWorker(
        dest: File,
        fileName: String,
        lastGroup: List<DOCTOR>
    ) {
        val temp = localLastFile.get()
        if (temp == null) {
            val lastFile = LastFile<DOCTOR>(dest, mutableMapOf(), jar = true)
            localLastFile.set(lastFile)
            lastProcessedFiles.add(lastFile)
        } else if (temp.dest.name == dest.name) {
            val lastFile = LastFile<DOCTOR>(dest, mutableMapOf(), jar = true)
            localLastFile.set(lastFile)
            lastProcessedFiles.add(lastFile)
        }
        val lastFile = localLastFile.get()
        lastFile.doctors[fileName] = lastGroup
    }

    override fun surgery(classFileByte: ByteArray): ByteArray {
        chiefDoctors.get()?.apply {
            return doSurgery(this, classFileByte)
        }
        return classFileByte
    }

    abstract fun doSurgery(doctors: List<DOCTOR>, classFileByte: ByteArray): ByteArray

    override fun surgeryOver() {
        if (lastProcessedFiles.isNotEmpty()) {
            lastProcessedFiles.forEach { lastFile ->
                or.submit {
                    when {
                        lastFile.jar -> repairJar(lastFile)
                        else -> repairFile(lastFile)
                    }
                }
            }
            or.await()
        }
        doctors.forEach {
            it.surgeryOver()
        }
    }

    private fun repairJar(lastFile: LastFile<DOCTOR>) {
        " # ${this.javaClass.simpleName} >>>>>>>>>>>>>>>>>>>>>>>>>> last repairJar file <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<".sout()
        " # ${this.javaClass.simpleName} >>> fire: ${lastFile.dest}".sout()
        " # ${this.javaClass.simpleName} >>> doctors: ${lastFile.doctors}".sout()
        " # ${this.javaClass.simpleName} >>>>>>>>>>>>>>>>>>>>>>>>>> last repairJar file <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<".sout()
        lastFile.dest.repairJar { jarEntry, bytes ->
            chiefDoctors.set(lastFile.doctors[jarEntry.name])
            surgery(bytes)
        }
    }

    private fun repairFile(lastFile: LastFile<DOCTOR>) {
        " # ${this.javaClass.simpleName} >>> last repairFile file <<< ==================================================".sout()
        " # ${this.javaClass.simpleName} >>> fire: ${lastFile.dest}".sout()
        " # ${this.javaClass.simpleName} >>> doctors: ${lastFile.doctors}".sout()
        " # ${this.javaClass.simpleName} >>> last repairFile file <<< ==================================================".sout()
        chiefDoctors.set(lastFile.doctors.values.flatten().toList())
        lastFile.dest.repair { bytes ->
            surgery(bytes)
        }
    }
}

class ClassTreeSurgery : ClassSurgeryImpl<ClassTreeDoctor>() {
    init {
        //利用SPI 全称为 (Service Provider Interface) 查找IWizard的实现类
        ServiceLoader.load(ClassTreeDoctor::class.java).iterator().forEach {
            " # ${this.javaClass.simpleName} === ClassTreeSurgery ==== ${it.javaClass.name}".sout()
            doctors.add(it)
        }
    }

    override fun doSurgery(doctors: List<ClassTreeDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isNullOrEmpty()) {
            return classFileByte
        }
        return ClassWriter(ClassWriter.COMPUTE_MAXS).also { writer ->
            doctors.fold(ClassNode().also { originNode ->
                ClassReader(classFileByte).accept(originNode, ClassReader.EXPAND_FRAMES)
            }) { classNode, worker ->
                try {
                    worker.surgery(classNode)
                } catch (e: Exception) {
                    e.printStackTrace()
                    classNode
                }
            }.accept(writer)
        }.toByteArray()
    }
}

class ClassVisitorSurgery : ClassSurgeryImpl<ClassVisitorDoctor>() {
    init {
        //利用SPI 全称为 (Service Provider Interface) 查找IWizard的实现类
        ServiceLoader.load(ClassVisitorDoctor::class.java).iterator().forEach {
            " # ${this.javaClass.simpleName} === ClassVisitorSurgery ==== ${it.javaClass.name}".sout()
            doctors.add(it)
        }
    }

    override fun doSurgery(doctors: List<ClassVisitorDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isNullOrEmpty()) {
            return classFileByte
        }
        //COMPUTE_MAXS 说明使用ASM自动计算本地变量表最大值和操作数栈的最大值
        return ClassWriter(ClassWriter.COMPUTE_MAXS).also {
            ClassReader(classFileByte).accept(doctors.fold(it as ClassVisitor) { acc, doctor ->
                try {
                    doctor.surgery(acc)
                } catch (e: Exception) {
                    e.printStackTrace()
                    acc
                }
                //EXPAND_FRAMES 说明在读取 class 的时候同时展开栈映射帧(StackMap Frame)
            }, ClassReader.EXPAND_FRAMES)
        }.toByteArray()
    }
}

