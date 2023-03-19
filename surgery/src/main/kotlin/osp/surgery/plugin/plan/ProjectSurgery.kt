package osp.surgery.plugin.plan

import org.apache.commons.io.FileUtils
import osp.surgery.api.FilterAction
import osp.surgery.helper.*
import osp.surgery.plugin.or.OperatingRoom
import java.io.File
import java.util.*
import java.util.jar.JarFile

/**
 * @author yun.
 * @date 2022/4/8
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */


//interferes
interface ProjectSurgery {
    fun surgeryPrepare()

    //过滤文件
    // 1 现在处理 --> transform
    // 2 以后处理 --> 收集起来
    fun surgeryOnFile(srcFile: File, srcDirectory: File, destDirectory: File, status: osp.surgery.api.Status)

    //过滤文件
    // 1 现在处理 --> 扫描jar-> transform
    // 2 以后处理 --> 扫描jar-> 收集起来
    fun surgeryOnJar(srcJarFile: File, destJarFile: File, status: osp.surgery.api.Status)

    fun surgeryOver()
}

class ProjectSurgeryImpl : ProjectSurgery {
    val classSurgeries = mutableListOf<ClassBytesSurgery>()
    private val scheduler = OperatingRoom()

    init {
        //利用SPI 全称为 (Service Provider Interface) 查找IWizard的实现类
        ServiceLoader.load(ClassBytesSurgery::class.java).iterator().forEach {
            " # ${this.javaClass.simpleName} ==== ProjectSurgery ==== ${it.javaClass.name}".sout()
            classSurgeries.add(it)
        }
    }

    override fun surgeryPrepare() {
        " # ${this.javaClass.simpleName} ==== surgeryPrepare ==== ".sout()
        classSurgeries.forEach {
            it.surgeryPrepare()
        }
    }

    override fun surgeryOnFile(
        srcFile: File,
        srcDirectory: File,
        destDirectory: File,
        status: osp.surgery.api.Status
    ) {
        scheduler.submit {
            if (status == osp.surgery.api.Status.NOTCHANGED) {
                " # ${this.javaClass.simpleName} ==== surgeryOnFile > NOTCHANGED class: ${srcFile.name}".sout()
                // 文件没变化
                // 如果是最后处理的文件 需要当做有变化处理，因为最后处理的文件都是遍历过其他文件之后对最后处理的文件进行修改
                return@submit
            }
            val destFilePath =
                srcFile.absolutePath.replace(srcDirectory.absolutePath, destDirectory.absolutePath)
            val destFile = File(destFilePath)
            if (srcFile.name.skipByFileName()) {
                " # ${this.javaClass.simpleName} ==== surgeryOnFile > skip class: ${srcFile.name}".sout()
                FileUtils.touch(destFile)
                FileUtils.copyFile(srcFile, destFile)
                return@submit
            }
            //如果都不处理就直接复制文件就行了
            val grouped = classSurgeries.groupBy {
                it.filterByClassName(srcFile, destFile, false, srcFile.name, status) {
                    srcFile.className(srcDirectory)
                }
            }
            val nowLastGroup = grouped[FilterAction.transformNowLast] ?: emptyList<ClassBytesSurgery>()
            val nowGroup =
                grouped[FilterAction.transformNow] ?: (emptyList<ClassBytesSurgery>() + nowLastGroup)
            if (nowGroup.isNotEmpty()) {
                //只要有现在执行的就执行 以后执行的他内部自己会在以后处理
                srcFile.review(destFile) { bytes ->
                    nowGroup.fold(bytes) { acc, more ->
                        more.surgery(acc)
                    }
                }
            } else {
                //如果现在要处理的为空 未来处理的不为空那么未来会处理 但是先要复制源文件到dest
                FileUtils.touch(destFile)
                FileUtils.copyFile(srcFile, destFile)
            }
        }
    }

    override fun surgeryOnJar(srcJarFile: File, destJarFile: File, status: osp.surgery.api.Status) {
        //处理jar的时候
        // 对于jar里面的class
        // 可能有部分classMore要处理部分不处理部分以后处理  而且只有在遍历的时候才知道
        // 所以当classMore内部有要现在处理和以后处理的情况的时候 就遍历让现在处理的去处理
        scheduler.submit {
            if (status == osp.surgery.api.Status.NOTCHANGED) {
                " # ${this.javaClass.simpleName} ==== surgeryOnJar > NOTCHANGED jar: ${srcJarFile.name}".sout()
                return@submit
            }

            if (srcJarFile.skipJar()) {
                " # ${this.javaClass.simpleName} ==== surgeryOnJar skip jar: ${srcJarFile.name} ==== ".sout()
                FileUtils.touch(destJarFile)
                FileUtils.copyFile(srcJarFile, destJarFile)
                return@submit
            }
            val grouped = classSurgeries.groupBy {
                it.filterByJar(srcJarFile)
            }
            val nowLastGroup = grouped[FilterAction.transformNowLast] ?: emptyList<ClassBytesSurgery>()
            val nowGroup =
                grouped[FilterAction.transformNow] ?: (emptyList<ClassBytesSurgery>() + nowLastGroup)
            val lastGroup = grouped[FilterAction.transformLast] ?: emptyList<ClassBytesSurgery>()

            if (nowGroup.isEmpty() && lastGroup.isEmpty()) {
                //都不处理就直接复制jar
                FileUtils.touch(destJarFile)
                FileUtils.copyFile(srcJarFile, destJarFile)
            } else {
                JarFile(srcJarFile).review(destJarFile) { jarEntry, bytes ->
                    classSurgeries.filter { more ->
                        val action =
                            more.filterByClassName(srcJarFile, destJarFile, true, jarEntry.name, status) {
                                jarEntry.name.className()
                            }
                        //以后处理的限制也得复制到dest因为以后处理的时候是直接处理dest
                        action >= FilterAction.transformNow
                    }.fold(bytes) { acc, more ->
                        more.surgery(acc)
                    }
                }
            }
        }
    }

    override fun surgeryOver() {
        scheduler.await()
        classSurgeries.forEach {
            it.surgeryOver()
        }
        " # ${this.javaClass.simpleName} ==== surgeryOver ==== ".sout()
    }
}
