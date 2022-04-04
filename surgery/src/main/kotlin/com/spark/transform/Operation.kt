package com.spark.transform

import com.android.build.api.transform.Status
import com.spark.*
import com.spark.helper.*
import com.spark.work.Schedulers
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.io.File
import com.spark.review.JSP
import com.spark.transform.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.jar.JarFile


/**
 * @author yun.
 * @date 2021/7/22
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */

//interferes
interface FileOperation {
    fun operationStart()

    //过滤文件
    // 1 现在处理 --> transform
    // 2 以后处理 --> 收集起来
    fun operateOnFile(srcFile: File, srcDirectory: File, destDirectory: File, status: Status)

    //过滤文件
    // 1 现在处理 --> 扫描jar-> transform
    // 2 以后处理 --> 扫描jar-> 收集起来
    fun operateOnJar(srcJarFile: File, destJarFile: File, status: Status)

    fun operationFinish()
}

class FileOperationImpl : FileOperation {
    val operations = mutableListOf<ClassOperation>()
    private val scheduler = Schedulers()
    private val sp by lazy {
        JSP("${this.javaClass.simpleName}_last")
    }
    val lastFiles by lazy {
        mutableListOf<String>().also {
            val cache = sp.read()
            " # ${this.javaClass.simpleName} last file cache: $cache".sout()
            if (!cache.isNullOrBlank()) {
                it.addAll(cache.substring(1, cache.length-1).split(",").toList())
            }
            " # ${this.javaClass.simpleName} last file cache: $it".sout()
        }
    }

    init {
        //利用SPI 全称为 (Service Provider Interface) 查找IWizard的实现类
        ServiceLoader.load(ClassOperation::class.java).iterator().forEach {
            " # ${this.javaClass.simpleName} ==== FileOperationImpl ==== ${it.javaClass.name}".sout()
            operations.add(it)
        }
    }

    override fun operationStart() {
        " # ${this.javaClass.simpleName} ==== operationStart ==== ".sout()
        operations.forEach {
            it.operationStart()
        }
    }

    override fun operateOnFile(srcFile: File, srcDirectory: File, destDirectory: File, status: Status) {
        scheduler.submit {
            val destFilePath = srcFile.absolutePath.replace(srcDirectory.absolutePath, destDirectory.absolutePath)
            val destFile = File(destFilePath)
            if (srcFile.name.skipByFileName()) {
                FileUtils.touch(destFile)
                FileUtils.copyFile(srcFile, destFile)
                return@submit
            }
            //如果都不处理就直接复制文件就行了
            val grouped = operations.groupBy {
                it.filterByClassName(srcFile, destFile, false, srcFile.name) {
                    srcFile.className(srcDirectory)
                }
            }
            if (status == Status.NOTCHANGED && !lastFiles.contains(srcFile.path)) {
                return@submit
            }
            val nowGroup = grouped[FilterAction.transformNow]
            if (!nowGroup.isNullOrEmpty()) {
                //只要有现在执行的就执行 以后执行的他内部自己会在以后处理
                srcFile.review(destFile) { bytes ->
                    nowGroup.fold(bytes) { acc, more ->
                        more.operate(acc)
                    }
                }
            } else {
                if (!grouped[FilterAction.transformLast].isNullOrEmpty()) {
                    if (!lastFiles.contains(srcFile.path)) {
                        lastFiles.add(srcFile.path)
                    }
                }
                //如果现在要处理的为空 未来处理的不为空那么未来会处理 但是先要复制源文件到dest
                FileUtils.touch(destFile)
                FileUtils.copyFile(srcFile, destFile)
            }
        }
    }

    override fun operateOnJar(srcJarFile: File, destJarFile: File, status: Status) {
        //处理jar的时候
        // 对于jar里面的class
        // 可能有部分classMore要处理部分不处理部分以后处理  而且只有在遍历的时候才知道
        // 所以当classMore内部有要现在处理和以后处理的情况的时候 就遍历让现在处理的去处理
        scheduler.submit {
            if (srcJarFile.skipJar()) {
                " # ${this.javaClass.simpleName} ==== operateOnJar skip ${srcJarFile.name} ==== ".sout()
                FileUtils.touch(destJarFile)
                FileUtils.copyFile(srcJarFile, destJarFile)
                return@submit
            }
            val grouped = operations.groupBy {
                it.filterByJar(srcJarFile)
            }
            val nowGroup = grouped[FilterAction.transformNow] ?: emptyList<ClassOperation>()
            val lastGroup = grouped[FilterAction.transformLast] ?: emptyList<ClassOperation>()
            if (status == Status.NOTCHANGED && !lastFiles.contains(srcJarFile.path)) {
                //查看 最后处理的文件有没有它 有的话需要transform
                if (!nowGroup.isNullOrEmpty() || !lastGroup.isNullOrEmpty()) {
                    JarFile(srcJarFile).scan { jarEntry ->
                        (nowGroup + lastGroup).groupBy { doctor ->
                            doctor.filterByClassName(srcJarFile, destJarFile, true, jarEntry.name) {
                                jarEntry.name.className()
                            }
                        }
                    }
                }
                return@submit
            }

            if (nowGroup.isNullOrEmpty() && lastGroup.isNullOrEmpty()) {
                //都不处理就直接复制jar
                FileUtils.touch(destJarFile)
                FileUtils.copyFile(srcJarFile, destJarFile)
            } else {
                var lastOperated = false
                JarFile(srcJarFile).review(destJarFile) { jarEntry, bytes ->
                    operations.filter { more ->
                        val action = more.filterByClassName(srcJarFile, destJarFile, true, jarEntry.name) {
                            jarEntry.name.className()
                        }
                        if (action == FilterAction.transformLast || action == FilterAction.transformNowLast) {
                            lastOperated = true
                        }
                        //以后处理的限制也得复制到dest因为以后处理的时候是直接处理dest
                        action >= FilterAction.transformNow
                    }.fold(bytes) { acc, more ->
                        more.operate(acc)
                    }
                }
                if (lastOperated && !lastFiles.contains(srcJarFile.path)) {
                    lastFiles.add(srcJarFile.path)
                }
            }
        }
    }

    override fun operationFinish() {
        scheduler.await()
        operations.forEach {
            it.operationFinish()
        }
        sp.save(lastFiles.toString())
        " # ${this.javaClass.simpleName} ==== operationFinish ==== ".sout()
    }
}

interface ClassOperation {
    fun operationStart()
    fun filterByJar(jar: File): FilterAction
    fun filterByClassName(src: File, dest: File, isJar: Boolean, fileName: String, className: () -> String): FilterAction
    fun operate(classFileByte: ByteArray): ByteArray
    fun operationFinish()
}

abstract class ClassOperationImpl<WORKER : ClassDoctor> : ClassOperation {
    val dealtWithLast = CopyOnWriteArrayList<LastFile<WORKER>>()
    val doctors = mutableListOf<WORKER>()
    val localLastFile = ThreadLocal<LastFile<WORKER>>()
    val nowDoctors = ThreadLocal<List<WORKER>>()
    private val scheduler = Schedulers()

    override fun operationStart() {
        localLastFile.set(null)
        nowDoctors.set(null)
        dealtWithLast.clear()
        doctors.forEach {
            it.startOperate()
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
    override fun filterByClassName(
        src: File,
        dest: File,
        isJar: Boolean,
        fileName: String,
        className: () -> String
    ): FilterAction {
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
                collectLastWorker(dest, fileName, lastGroup)
                " # ${this.javaClass.simpleName} >>> fond jar file to last : ${src.name} >> doctors : $lastGroup".sout()
                result = FilterAction.transformLast
            }
            if (nowGroup.isNullOrEmpty()) {
                //这里没有 现在就要处理的 那么就全部是以后处理不需要转换
                return result
            }
            nowDoctors.set(nowGroup)
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
                dealtWithLast.add(LastFile(dest, mutableMapOf(fileName to lastWorkers), jar = false))
                " # ${this.javaClass.simpleName} >>> fond class file to last : ${src.name} >> doctors : $lastWorkers".sout()
                return FilterAction.noTransform
            }
            if (nowGroup.isNullOrEmpty()) {
                return FilterAction.noTransform
            }
            nowDoctors.set(nowGroup)
        }
        return FilterAction.transformNow
    }

    private fun collectLastWorker(
        dest: File,
        fileName: String,
        lastGroup: List<WORKER>
    ) {
        val temp = localLastFile.get()
        if (temp == null) {
            val lastFile = LastFile<WORKER>(dest, mutableMapOf(), jar = true)
            localLastFile.set(lastFile)
            dealtWithLast.add(lastFile)
        } else if (temp.dest.name == dest.name) {
            val lastFile = LastFile<WORKER>(dest, mutableMapOf(), jar = true)
            localLastFile.set(lastFile)
            dealtWithLast.add(lastFile)
        }
        val lastFile = localLastFile.get()
        lastFile.doctors[fileName] = lastGroup
    }

    override fun operate(classFileByte: ByteArray): ByteArray {
        nowDoctors.get()?.apply {
            return doOperate(this, classFileByte)
        }
        return classFileByte
    }

    abstract fun doOperate(workers: List<WORKER>, classFileByte: ByteArray): ByteArray

    override fun operationFinish() {
        if (dealtWithLast.isNotEmpty()) {
            dealtWithLast.forEach { lastFile ->
                scheduler.submit {
                    when {
                        lastFile.jar -> repairJar(lastFile)
                        else -> repairFile(lastFile)
                    }
                }
            }
            scheduler.await()
        }
        doctors.forEach {
            it.finishOperate()
        }
    }

    private fun repairJar(lastFile: LastFile<WORKER>) {
        " # ${this.javaClass.simpleName} >>>>>>>>>>>>>>>>>>>>>>>>>> last repairJar file <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<".sout()
        " # ${this.javaClass.simpleName} >>> fire: ${lastFile.dest}".sout()
        " # ${this.javaClass.simpleName} >>> doctors: ${lastFile.doctors}".sout()
        " # ${this.javaClass.simpleName} >>>>>>>>>>>>>>>>>>>>>>>>>> last repairJar file <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<".sout()
        lastFile.dest.repairJar { jarEntry, bytes ->
            nowDoctors.set(lastFile.doctors[jarEntry.name])
            operate(bytes)
        }
    }

    private fun repairFile(lastFile: LastFile<WORKER>) {
        " # ${this.javaClass.simpleName} >>> last repairFile file <<< ==================================================".sout()
        " # ${this.javaClass.simpleName} >>> fire: ${lastFile.dest}".sout()
        " # ${this.javaClass.simpleName} >>> doctors: ${lastFile.doctors}".sout()
        " # ${this.javaClass.simpleName} >>> last repairFile file <<< ==================================================".sout()
        nowDoctors.set(lastFile.doctors.values.flatten().toList())
        lastFile.dest.repair { bytes ->
            operate(bytes)
        }
    }
}

class ClassTreeOperation : ClassOperationImpl<ClassTreeDoctor>() {
    init {
        //利用SPI 全称为 (Service Provider Interface) 查找IWizard的实现类
        ServiceLoader.load(ClassTreeDoctor::class.java).iterator().forEach {
            " # ${this.javaClass.simpleName} === ClassTreeOperation ==== ${it.javaClass.name}".sout()
            doctors.add(it)
        }
    }

    override fun doOperate(workers: List<ClassTreeDoctor>, classFileByte: ByteArray): ByteArray {
        return ClassWriter(ClassWriter.COMPUTE_MAXS).also { writer ->
            workers.fold(ClassNode().also { originNode ->
                ClassReader(classFileByte).accept(originNode, 0)
            }) { classNode, worker ->
                worker.operate(classNode)
            }.accept(writer)
        }.toByteArray()
    }
}

