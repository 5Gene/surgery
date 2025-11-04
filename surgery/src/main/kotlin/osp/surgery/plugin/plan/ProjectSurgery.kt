package osp.surgery.plugin.plan

import osp.surgery.api.ClassBytesSurgery
import osp.surgery.api.FilterAction
import osp.surgery.helper.*
import osp.surgery.plugin.plan.filterDoctors
import java.io.File
import java.io.InputStream
import java.util.*

/**
 * @author yun.
 * @date 2022/4/8
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */

sealed class SurgeryMeds(open val compileClassName: String) {
    class Byte(override val compileClassName: String, val value: ByteArray) : SurgeryMeds(compileClassName)
    data class Stream(override val compileClassName: String, val value: InputStream) : SurgeryMeds(compileClassName)
}

//interferes
interface ProjectSurgery {
    fun surgeryPrepare()

    //过滤文件
    // 1 现在处理 --> transform
    // 2 以后处理 --> 收集起来
    fun surgeryOnClass(fileName: String, compileClassName: String, inputJarStream: InputStream): SurgeryMeds?

    //过滤文件
    // 1 是否处理jar
    fun surgeryCheckJar(jarFile: File): Boolean

    fun surgeryOver(): List<Pair<String, ByteArray>>?
}

class ProjectSurgeryImpl(val logger: (String) -> Unit) : ProjectSurgery {
    private val classSurgeries = mutableListOf<ClassBytesSurgery>()
    private val grandFinales: MutableList<GrandFinale<ClassBytesSurgery>> = mutableListOf()
    private val tag = javaClass.simpleName

    init {
        val classBytesSurgeries = listOf(ClassTreeSurgery(), ClassVisitorSurgery())
        classBytesSurgeries.iterator().forEach {
            logger("${Thread.currentThread().id} $tag ==== ProjectSurgery ==== ${it.javaClass.name}")
            classSurgeries.add(it)
        }
    }

    override fun surgeryPrepare() {
        logger("${Thread.currentThread().id} $tag ==== surgeryPrepare ==== ")
        grandFinales.clear()
        classSurgeries.forEach {
            it.surgeryPrepare()
        }
    }

    override fun surgeryCheckJar(jarFile: File): Boolean {
        // 处理jar的时候
        // 对于jar里面的class
        // 可能有部分classMore要处理部分不处理部分以后处理  而且只有在遍历的时候才知道
        // 所以当classMore内部有要现在处理和以后处理的情况的时候 就遍历让现在处理的去处理
        if (classSurgeries.isEmpty()) {
            logger("${Thread.currentThread().id} $tag ==== surgeryCheckJar classSurgeries is empty: ${jarFile.name} ==== ")
            return false
        }
        //0.jar里都是R.class, R$xxx.class
        //源码依赖的模块,都是class.jar
        if (SurgeryConfig.shouldSkipJar(jarFile.name)) {
            logger("${Thread.currentThread().id} $tag ==== surgeryCheckJar skip jar: ${jarFile.name} ==== ")
            return false
        }
        val result = filterDoctors(classSurgeries) {
            it.filterByJar(jarFile)
        }

        if (result.hasTransform) {
            "🔪.$tag ====  surgeryCheckJar > need surgery -> ${jarFile.name} $".sout()
            return true
        } else {
            logger("${Thread.currentThread().id} $tag ==== surgeryCheckJar no transform > ${jarFile.name} ==== ")
            //都不处理就直接复制jar
            return false
        }
    }

    override fun surgeryOnClass(
        fileName: String,
        compileClassName: String,// com/alibaba/android/arouter/routes/ARouter$$Providers$$app.class
        inputJarStream: InputStream
    ): SurgeryMeds? {
        if (classSurgeries.isEmpty()) {
            logger("${Thread.currentThread().id} $tag ==== surgeryOnClass classSurgeries is empty: $fileName ==== ")
            return SurgeryMeds.Stream(compileClassName, inputJarStream)
        }
        if (SurgeryConfig.shouldSkipClass(fileName)) {
            logger("${Thread.currentThread().id} $tag ==== surgeryOnClass > skip > class: $fileName")
            return SurgeryMeds.Stream(compileClassName, inputJarStream)
        }
        //如果都不处理就直接复制文件就行了
        // 优化：只读取一次字节码，避免重复I/O
        val bytes = inputJarStream.readBytes()
        val result = filterDoctors(classSurgeries) {
            it.filterByClassName(fileName, compileClassName)
        }
        if (result.last.isNotEmpty()) {
            "🔪.$tag ==== surgeryOnClass > grand finale > class: $fileName".sout()
            //只要有最后执行的就不执行 最后处理
            grandFinales.add(GrandFinale(fileName, compileClassName, bytes, result.allTransform))
            return null
        } else if (result.now.isNotEmpty()) {
            //如果现在要处理的不为空, 就现在处理
            "🔪.$tag ==== surgeryOnClass > transform now > class: $fileName > doctors size:${result.now.size}".sout()
            val transformedBytes = result.now.fold(bytes) { acc, more ->
                "🔪.$tag === ${more.javaClass.simpleName} -> surgeryOnClass > transform now > class: $fileName".sout()
                more.surgery(fileName, acc)
            }
            return SurgeryMeds.Byte(compileClassName, transformedBytes)
        }
        logger("${Thread.currentThread().id} $tag ==== surgeryOnClass no transform > class: $fileName")
        //没有未来处理的也没有现在要处理的
        // 注意：如果已经读取了bytes，需要创建新的InputStream
        return SurgeryMeds.Byte(compileClassName, bytes)
    }

    override fun surgeryOver(): List<Pair<String, ByteArray>>? {
        if (grandFinales.isEmpty()) {
            classSurgeries.forEach {
                it.surgeryOver()
            }
            logger("${Thread.currentThread().id} $tag ==== surgeryOver ==== ")
            return null
        }
        val jarBytes = mutableListOf<Pair<String, ByteArray>>()
        grandFinales.forEach {
            "$tag -> surgeryOver surgery now:${it.compileClassName} ==== ".sout()
            jarBytes.add(it.compileClassName to it.doctors.fold(it.classByteArray) { acc, more ->
                "🔪.$tag == ${more.javaClass.simpleName} -> surgeryOver surgery now:${it.compileClassName} ==== ".sout()
                more.surgery(it.fileName, acc)
            })
        }
        logger("${Thread.currentThread().id} $tag ==== surgeryOver grandFinales:${grandFinales.size}==== ")
        classSurgeries.forEach {
            it.surgeryOver()
        }
        logger("${Thread.currentThread().id} $tag ==== surgeryOver ==== ")
        return jarBytes
    }
}
