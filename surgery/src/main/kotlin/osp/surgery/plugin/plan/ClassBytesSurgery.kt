package osp.surgery.plugin.plan

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.com.google.gson.Gson
import org.jetbrains.kotlin.com.google.gson.JsonParser
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import osp.surgery.api.*
import osp.surgery.helper.*
import osp.surgery.plugin.Dean
import osp.surgery.plugin.JSP
import osp.surgery.plugin.or.OperatingRoom
import osp.surgery.plugin.wings.ExtendClassWriter
import java.io.File
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * @author yun.
 * @date 2022/4/8
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */
data class LastFile<DOCTOR>(
    val destPath: String,
    val doctors: MutableMap<String, MutableSet<DOCTOR>>,
    val jar: Boolean = false
)

interface ClassBytesSurgery {
    fun surgeryPrepare()
    fun filterByJar(jar: File): FilterAction
    fun filterByClassName(
        src: File,
        dest: File,
        isJar: Boolean,
        fileName: String,
        status: Status,
        className: () -> String
    ): FilterAction

    fun surgery(classFileByte: ByteArray): ByteArray
    fun surgeryOver()
}

abstract class ClassByteSurgeryImpl<DOCTOR : ClassDoctor> : ClassBytesSurgery {
    val tag = this.javaClass.simpleName
    private val finalProcessedFiles = CopyOnWriteArrayList<LastFile<DOCTOR>>()
    private val localLastFile = ThreadLocal<LastFile<DOCTOR>>()
    private val chiefDoctors = ThreadLocal<List<DOCTOR>>()
    private val currentFile = ThreadLocal<File>()
    private val or = OperatingRoom()
    private val gson = Gson()
    private val sp by lazy {
        JSP("${tag}_class_surgery")
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
            finalProcessedFiles.addAll(array.map {
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
                LastFile(path, map, jar = path.isJar())
            }.toList())
            " # $tag >>> =============== cache ================== <<< ".sout()
            " # $tag >>> ${gson.toJson(finalProcessedFiles)} <<< ".sout()
            " # $tag >>> =============== cache ================== <<< ".sout()
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
        if (doctors.isEmpty()) {
            return FilterAction.noTransform
        }
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
    override fun filterByClassName(
        src: File,
        dest: File,
        isJar: Boolean,
        fileName: String,
        status: Status,
        className: () -> String
    ): FilterAction {
        if (doctors.isEmpty()) {
            return FilterAction.noTransform
        }
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
                "$tag > lastGroup : ${lastGroup.size} > $lastGroup".sout()
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
                "$tag > lastWorkers > $lastWorkers".sout()
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
        val lastFile = finalProcessedFiles.find {
            it.destPath == dest.path
        } ?: LastFile(dest.path, mutableMapOf(fileName to mutableSetOf<DOCTOR>()), jar = dest.isJar()).apply {
            finalProcessedFiles.add(this)
        }
        if (lastFile.doctors[fileName] == null) {
            lastFile.doctors[fileName] = lastGroup.toMutableSet()
        } else {
            lastFile.doctors[fileName]!!.addAll(lastGroup)
        }
        " # $tag >>> fond file to last : ${dest.name} >> doctors : ${lastFile.doctors[fileName]}".sout()
    }

    override fun surgery(classFileByte: ByteArray): ByteArray {
        chiefDoctors.get()?.apply {
            return doSurgery(this, classFileByte)
        }
        return classFileByte
    }

    abstract fun doSurgery(doctors: List<DOCTOR>, classFileByte: ByteArray): ByteArray

    override fun surgeryOver() {
        "\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47 $this surgeryOver \uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47".sout()
        if (finalProcessedFiles.isNotEmpty()) {
            val distincted = finalProcessedFiles.distinctBy {
                it.destPath
            }
            distincted.forEach { lastFile ->
                "finalProcessedFile >> $lastFile".sout()
                or.submit {
                    when {
                        lastFile.jar -> repairJar(lastFile)
                        else -> repairFile(lastFile)
                    }
                }
            }
            or.await()
            try {
                sp.save(gson.toJson(distincted))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        doctors.forEach {
            it.surgeryOver()
        }
        "\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46 $this surgeryOver \uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46".sout()
    }

    private fun repairJar(lastFile: LastFile<DOCTOR>) {
        " # $tag \uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47 final repairJar file \uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47".sout()
        " # $tag >>> fire: ${lastFile.destPath}".sout()
        " # $tag >>> doctors: ${lastFile.doctors}".sout()
        File(lastFile.destPath).repairJar { jarEntry, bytes ->
            lastFile.doctors[jarEntry.name]?.toList()?.let {
                chiefDoctors.set(it)
                surgery(bytes)
            } ?: run {
                chiefDoctors.remove()
                bytes
            }
        }
        " # $tag \uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46 final repairJar file \uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46".sout()
    }

    private fun repairFile(lastFile: LastFile<DOCTOR>) {
        " # $tag \uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47 final repairFile file \uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47".sout()
        " # $tag >>> fire: ${lastFile.destPath}".sout()
        " # $tag >>> doctors: ${lastFile.doctors}".sout()
        chiefDoctors.set(lastFile.doctors.values.flatten().toList())
        File(lastFile.destPath).repair { bytes ->
            surgery(bytes)
        }
        " # $tag \uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46 final repairFile file \uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46".sout()
    }
}

@AutoService(ClassBytesSurgery::class)
class ClassTreeSurgery : ClassByteSurgeryImpl<ClassTreeDoctor>() {

    override fun loadDoctors(): MutableMap<String, ClassTreeDoctor> {
        "\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47 $tag : loadDoctors \uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47".sout()
        //利用SPI 全称为 (Service Provider Interface) 查找 实现类
        val supers = mutableListOf<String>()
        return ServiceLoader.load(ClassTreeDoctor::class.java).iterator().asSequence()
            .onEach {
                supers.add(it.javaClass.superclass.name)
            }.filter {
                !supers.contains(it.javaClass.name)
            }.map {
                " # $tag === ClassTreeSurgery ==== ${it.javaClass.name}".sout()
                it.className to it
            }.toMap().toMutableMap().also {
                "\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46 $tag : loadDoctors \uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46".sout()
            }
    }

    override fun doSurgery(doctors: List<ClassTreeDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isEmpty()) {
            return classFileByte
        }
//        ClassWriter.COMPUTE_MAXS
//        这种方式会自动计算上述 操作数栈和局部变量表的大小 但需要手动触发
//        通过调用org.objectweb.asm.commons.LocalVariablesSorter#visitMaxs
//        触发 参数可以随便写
//        ClassWriter.COMPUTE_FRAMES
//        不仅会计算上述 操作数栈和局部变量表的大小 还会自动计算StackMapFrames
        try {
            return ExtendClassWriter(ClassWriter.COMPUTE_FRAMES).also { writer ->
                doctors.fold(ClassNode().also { originNode ->
                    ClassReader(classFileByte).accept(
                        originNode,
                        ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
                    )
                }) { classNode, doctor ->
                    try {
                        if (Modifier.isInterface(classNode.access)) {
                            "$tag > skip class [is interface] ${classNode.name}".sout()
                            classNode
                        } else {
                            doctor.surgery(classNode)
                        }
                    } catch (e: Exception) {
                        "$tag >>> error >>> ${classNode.name} > ${e.message}".sout()
                        classNode
                    }
                }.accept(writer)
            }.toByteArray()
        } catch (e: Exception) {
            "$tag >>> error >>> [byte to asm] > ${e.message}".sout()
            return classFileByte
        }
    }
}

@AutoService(ClassBytesSurgery::class)
class ClassVisitorSurgery : ClassByteSurgeryImpl<ClassVisitorDoctor>() {
    override fun loadDoctors(): MutableMap<String, ClassVisitorDoctor> {
        val classVisitorDoctors = ServiceLoader.load(ClassVisitorDoctor::class.java)
        if (!classVisitorDoctors.iterator().hasNext()) {
            return mutableMapOf<String, ClassVisitorDoctor>()
        }
        "\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47 $tag : loadDoctors \uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47\uD83D\uDC47".sout()
        //利用SPI 全称为 (Service Provider Interface) 查找 实现类
        val supers = mutableListOf<String>()
        return classVisitorDoctors.iterator().asSequence()
            .onEach {
                supers.add(it.javaClass.superclass.name)
            }.filter {
                !supers.contains(it.javaClass.name)
            }.map {
                " # $tag === ClassVisitorSurgery ==== ${it.javaClass.superclass.simpleName}".sout()
                it.className to it
            }.toMap().toMutableMap().also {
                "\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46 $tag : loadDoctors \uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46\uD83D\uDC46".sout()
            }
    }

    override fun doSurgery(doctors: List<ClassVisitorDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isEmpty()) {
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
        try {
            return ExtendClassWriter(ClassWriter.COMPUTE_FRAMES).also {
                ClassReader(classFileByte).accept(doctors.fold(it as ClassVisitor) { acc, doctor ->
                    try {
                        doctor.surgery(acc)
                    } catch (e: Exception) {
                        "$tag >>> error >>> [surgery] > ${e.message}".sout()
                        acc
                    }
                    //EXPAND_FRAMES 说明在读取 class 的时候同时展开栈映射帧(StackMap Frame)
                }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            }.toByteArray()
        } catch (e: Exception) {
            "$tag >>> error >>> [byte to asm] > ${e.message}".sout()
            return classFileByte
        }
    }
}

