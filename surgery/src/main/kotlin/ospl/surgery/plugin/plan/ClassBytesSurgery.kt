package sparkj.surgery.plan

import org.jetbrains.kotlin.com.google.gson.Gson
import org.jetbrains.kotlin.com.google.gson.JsonParser
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import com.google.auto.service.AutoService
import ospl.surgery.api.*
import sparkj.surgery.or.OperatingRoom
import java.io.File
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import ospl.surgery.helper.*
import ospl.surgery.plugin.Dean
import ospl.surgery.plugin.JSP
import sparkj.surgery.more.ExtendClassWriter

/**
 * @author yun.
 * @date 2022/4/8
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
data class LastFile<DOCTOR>(
    val dest: File,
    val doctors: MutableMap<String, MutableSet<DOCTOR>>,
    val jar: Boolean = false
)

interface ClassBytesSurgery {
    fun surgeryPrepare()
    fun filterByJar(jar: File): FilterAction
    fun filterByClassName(src: File, dest: File, isJar: Boolean, fileName: String, status: Status, className: () -> String): FilterAction
    fun surgery(classFileByte: ByteArray): ByteArray
    fun surgeryOver()
}

abstract class ClassByteSurgeryImpl<DOCTOR : ClassDoctor> : ClassBytesSurgery {
    private val lastProcessedFiles = CopyOnWriteArrayList<LastFile<DOCTOR>>()
    private val localLastFile = ThreadLocal<LastFile<DOCTOR>>()
    private val chiefDoctors = ThreadLocal<List<DOCTOR>>()
    private val currentFile = ThreadLocal<File>()
    private val or = OperatingRoom()
    private val gson = Gson()
    private val sp by lazy {
        JSP("${this.javaClass.simpleName}_class_surgery")
    }

    private val doctors by lazy {
        val doctorsMap = loadDoctors()
        if (Dean.context.transformInvocation?.isIncremental != true) {
            doctorsMap.values
        } else {
            restoreCache(doctorsMap)
        }
    }

    private fun restoreCache(doctorsMap: MutableMap<String, DOCTOR>): MutableCollection<DOCTOR> {
        val cache = sp.read()
        if (cache.isNotEmpty()) {
            val array = JsonParser.parseString(cache).asJsonArray
            lastProcessedFiles.addAll(array.map {
                val jsonobj = it.asJsonObject
                val path = jsonobj.get("dest").asJsonObject.get("path").asString
                val doctorsObj = jsonobj.get("doctors").asJsonObject
                val map = doctorsObj.keySet().associateWith { file ->
                    doctorsObj.get(file).asJsonArray.asSequence().map { doc ->
                        val className = doc.asJsonObject.get("className").asString
                        val classIns = Class.forName(className)
                        doctorsMap[className] = gson.fromJson(gson.toJson(doc), classIns) as DOCTOR
                        doctorsMap[className]!!
                    }.toMutableSet()
                }.toMutableMap()
                LastFile(File(path), map, jar = path.isJar())
            }.toList())
            " # ${this.javaClass.simpleName} >>> =============== cache ================== <<< ".sout()
            " # ${this.javaClass.simpleName} >>> ${gson.toJson(lastProcessedFiles)} <<< ".sout()
            " # ${this.javaClass.simpleName} >>> =============== cache ================== <<< ".sout()
        }
        return doctorsMap.values
    }

    abstract fun loadDoctors(): MutableMap<String, DOCTOR>

    override fun surgeryPrepare() {
        localLastFile.set(null)
        chiefDoctors.set(null)
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
                println(lastGroup)
                println(lastGroup.size.toString())
                collectLastWorker(dest, fileName, lastGroup)
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
                println(lastWorkers)
                collectLastWorker(dest, fileName, lastWorkers)
                return FilterAction.noTransform
            }
            if (nowGroup.isNullOrEmpty()) {
                return FilterAction.noTransform
            }
            chiefDoctors.set(nowGroup)
        }
        return FilterAction.transformNow
    }

    @Synchronized
    protected fun collectLastWorker(
        dest: File,
        fileName: String,
        lastGroup: List<DOCTOR>
    ) {
        val lastFile = lastProcessedFiles.find {
            it.dest.path == dest.path
        } ?: LastFile(dest, mutableMapOf(fileName to mutableSetOf<DOCTOR>()), jar = dest.isJar()).apply {
            lastProcessedFiles.add(this)
        }
        if (lastFile.doctors[fileName] == null) {
            lastFile.doctors[fileName] = lastGroup.toMutableSet()
        } else {
            lastFile.doctors[fileName]!!.addAll(lastGroup)
        }
        " # ${this.javaClass.simpleName} >>> fond file to last : ${dest.name} >> doctors : ${lastFile.doctors[fileName]}".sout()
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
            val distincted = lastProcessedFiles.distinctBy {
                it.dest.path
            }
            distincted.forEach { lastFile ->
                or.submit {
                    when {
                        lastFile.jar -> repairJar(lastFile)
                        else -> repairFile(lastFile)
                    }
                }
            }
            or.await()
            sp.save(gson.toJson(distincted))
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
            chiefDoctors.set(lastFile.doctors[jarEntry.name]?.toList())
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

@AutoService(ClassBytesSurgery::class)
class ClassTreeSurgery : ClassByteSurgeryImpl<ClassTreeDoctor>() {

    override fun loadDoctors(): MutableMap<String, ClassTreeDoctor> {
        //利用SPI 全称为 (Service Provider Interface) 查找 实现类
        return ServiceLoader.load(ClassTreeDoctor::class.java).iterator().asSequence().map {
            " # ${this.javaClass.simpleName} === ClassTreeSurgery ==== ${it.javaClass.name}".sout()
            it.className to it
        }.toMap().toMutableMap()
    }

    override fun doSurgery(doctors: List<ClassTreeDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isNullOrEmpty()) {
            return classFileByte
        }
//        ClassWriter.COMPUTE_MAXS
//        这种方式会自动计算上述 操作数栈和局部变量表的大小 但需要手动触发
//        通过调用org.objectweb.asm.commons.LocalVariablesSorter#visitMaxs
//        触发 参数可以随便写
//        ClassWriter.COMPUTE_FRAMES
//        不仅会计算上述 操作数栈和局部变量表的大小 还会自动计算StackMapFrames
        return ExtendClassWriter(ClassWriter.COMPUTE_FRAMES).also { writer ->
            doctors.fold(ClassNode().also { originNode ->
                ClassReader(classFileByte).accept(originNode, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            }) { classNode, doctor ->
                try {
                    doctor.surgery(classNode)
                } catch (e: Exception) {
                    println("${classNode.name} > ${e.message}")
                    classNode
                }
            }.accept(writer)
        }.toByteArray()
    }
}

@AutoService(ClassBytesSurgery::class)
class ClassVisitorSurgery : ClassByteSurgeryImpl<ClassVisitorDoctor>() {
    override fun loadDoctors(): MutableMap<String, ClassVisitorDoctor> {
        //利用SPI 全称为 (Service Provider Interface) 查找 实现类
        return ServiceLoader.load(ClassVisitorDoctor::class.java).iterator().asSequence().map {
            " # ${this.javaClass.simpleName} === ClassVisitorSurgery ==== ${it.javaClass.name}".sout()
            it.className to it
        }.toMap().toMutableMap()
    }

    override fun doSurgery(doctors: List<ClassVisitorDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isNullOrEmpty()) {
            return classFileByte
        }
//        ClassWriter.COMPUTE_MAXS
//        这种方式会自动计算上述 操作数栈和局部变量表的大小 但需要手动触发
//        通过调用org.objectweb.asm.commons.LocalVariablesSorter#visitMaxs
//        触发 参数可以随便写
//        ClassWriter.COMPUTE_FRAMES
//        不仅会计算上述 操作数栈和局部变量表的大小 还会自动计算StackMapFrames
        //https://www.jianshu.com/p/abd1b1b8d3f3
        //https://www.kingkk.com/2020/08/ASM%E5%8E%86%E9%99%A9%E8%AE%B0/
        return ExtendClassWriter(ClassWriter.COMPUTE_FRAMES).also {
            ClassReader(classFileByte).accept(doctors.fold(it as ClassVisitor) { acc, doctor ->
                try {
                    doctor.surgery(acc)
                } catch (e: Exception) {
                    e.message?.sout()
                    acc
                }
                //EXPAND_FRAMES 说明在读取 class 的时候同时展开栈映射帧(StackMap Frame)
            }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
        }.toByteArray()
    }
}

