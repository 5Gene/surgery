package osp.surgery.plugin.plan

import osp.surgery.api.ClassBytesSurgery
import osp.surgery.api.FilterAction
import osp.surgery.helper.*
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

class ProjectSurgeryImpl : ProjectSurgery {
    val classSurgeries = mutableListOf<ClassBytesSurgery>()
    private val grandFinales: MutableList<GrandFinale<ClassBytesSurgery>> = mutableListOf<GrandFinale<ClassBytesSurgery>>()

    init {
        //利用SPI 全称为 (Service Provider Interface) 查找IWizard的实现类
//        val classBytesSurgeries = ServiceLoader.load(ClassBytesSurgery::class.java)
        val classBytesSurgeries = listOf(ClassTreeSurgery(), ClassVisitorSurgery())
        classBytesSurgeries.iterator().forEach {
            " # ${this.javaClass.simpleName} ==== ProjectSurgery ==== ${it.javaClass.name}".sout()
            classSurgeries.add(it)
        }
    }

    override fun surgeryPrepare() {
        " # ${this.javaClass.simpleName} ==== surgeryPrepare ==== ".sout()
        grandFinales.clear()
        classSurgeries.forEach {
            it.surgeryPrepare()
        }
    }

    override fun surgeryCheckJar(jarFile: File): Boolean {
        //处理jar的时候
        // 对于jar里面的class
        // 可能有部分classMore要处理部分不处理部分以后处理  而且只有在遍历的时候才知道
        // 所以当classMore内部有要现在处理和以后处理的情况的时候 就遍历让现在处理的去处理
        if (classSurgeries.isEmpty()) {
            " # ${this.javaClass.simpleName} ==== surgeryCheckJar classSurgeries is empty: ${jarFile.name} ==== ".sout()
            return false
        }
        if (jarFile.skipJar()) {
            " # ${this.javaClass.simpleName} ==== surgeryCheckJar skip jar: ${jarFile.name} ==== ".sout()
            return false
        }
        val grouped = classSurgeries.groupBy {
            it.filterByJar(jarFile)
        }
        val nowLastGroup = grouped[FilterAction.transformLast].orEmpty()
        val nowGroup =
            grouped[FilterAction.transformNow].orEmpty() + nowLastGroup
        val lastGroup = grouped[FilterAction.transformLast] ?: emptyList<ClassBytesSurgery>()

        if (nowGroup.isNotEmpty() || lastGroup.isNotEmpty()) {
            " # ${this.javaClass.simpleName} ==== surgeryCheckJar > ${jarFile.name} ==== ".sout()
            return true
        } else {
            " # ${this.javaClass.simpleName} ==== surgeryCheckJar no transform > ${jarFile.name} ==== ".sout()
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
            " # ${this.javaClass.simpleName} ==== surgeryOnClass classSurgeries is empty: $fileName ==== ".sout()
            return SurgeryMeds.Stream(compileClassName, inputJarStream)
        }
        if (fileName.skipByFileName()) {
            " # ${this.javaClass.simpleName} ==== surgeryOnClass > skip > class: $fileName".sout()
            return SurgeryMeds.Stream(compileClassName, inputJarStream)
        }
        //如果都不处理就直接复制文件就行了
        val grouped = classSurgeries.groupBy {
            it.filterByClassName(fileName, compileClassName)
        }
        val lastGroup = grouped[FilterAction.transformLast].orEmpty()
        val nowGroup = grouped[FilterAction.transformNow].orEmpty()
        if (lastGroup.isNotEmpty()) {
            " # ${this.javaClass.simpleName} ==== surgeryOnClass > grand finale > class: $fileName".sout()
            //只要有最后执行的就不执行 最后处理
            grandFinales.add(GrandFinale(fileName, compileClassName, inputJarStream.readBytes(), lastGroup + nowGroup))
            return null
        } else if (nowGroup.isNotEmpty()) {
            //如果现在要处理的不为空, 就现在处理
            " # ${this.javaClass.simpleName} ==== surgeryOnClass > transform now > class: $fileName".sout()
            val bytes = inputJarStream.readBytes()
            return SurgeryMeds.Byte(compileClassName, nowGroup.fold(bytes) { acc, more ->
                more.surgery(fileName, acc)
            })
        }
        " # ${this.javaClass.simpleName} ==== surgeryOnClass no transform > class: $fileName".sout()
        //没有未来处理的也没有现在要处理的
        return SurgeryMeds.Stream(compileClassName, inputJarStream)
    }

    override fun surgeryOver(): List<Pair<String, ByteArray>>? {
        if (grandFinales.isEmpty()) {
            classSurgeries.forEach {
                it.surgeryOver()
            }
            " # ${this.javaClass.simpleName} ==== surgeryOver ==== ".sout()
            return null
        }
        val jarBytes = mutableListOf<Pair<String, ByteArray>>()
        grandFinales.forEach {
            " # ${this.javaClass.simpleName} ==== surgeryOver surgery:${it.compileClassName} ==== ".sout()
            jarBytes.add(it.compileClassName to it.doctors.fold(it.classByteArray) { acc, more ->
                more.surgery(it.fileName, acc)
            })
        }
        " # ${this.javaClass.simpleName} ==== surgeryOver grandFinales:${grandFinales.size}==== ".sout()
        classSurgeries.forEach {
            it.surgeryOver()
        }
        " # ${this.javaClass.simpleName} ==== surgeryOver ==== ".sout()
        return jarBytes
    }
}
