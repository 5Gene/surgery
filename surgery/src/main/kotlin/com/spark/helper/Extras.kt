package com.spark.helper

import com.spark.review.SparkEnvironment
import com.spark.skipByFileName
import com.spark.sout
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * @author yun.
 * @date 2022/3/31
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */

enum class FilterAction {
    noTransform, transformLast, transformNow, transformNowLast
}

data class LastFile<WORKER>(
    val dest: File,
    val doctors: MutableMap<String, List<WORKER>>,
    val jar: Boolean = false
)

fun String.log() {
    SparkEnvironment.environment.project?.logger?.info("✨ $this ")
}

fun File.className(parent: File?): String {
    return parent!!.toURI().relativize(toURI()).normalize().path.className()
}
fun File.isJar(): Boolean {
    return name.endsWith(".jar")
}

fun String.className(): String {
    return substringBeforeLast(".").replace("/", ".")
}

fun File.touch(): File {
    FileUtils.touch(this)
    return this
}


inline fun File.repair(repair: (ByteArray) -> ByteArray) {
    val destFile = File("$absolutePath.temp")
    review(destFile,repair)
    delete()
    FileUtils.moveFile(destFile, this)
}

//处理class文件 从srcFile复制到destFile ，review这个过程，中间对byteArray做一次处理
inline fun File.review(destFile: File, wizard: (ByteArray) -> ByteArray) {
    destFile.touch().outputStream().use { outputStream ->
        this.inputStream().use {
            if (this.name.skipByFileName()) {
                outputStream.write(it.readBytes())
            } else {
                outputStream.write(it.review(wizard))
            }
        }
    }
}

inline fun File.repairJar(repair: (srcJarEntry: JarEntry, bytes: ByteArray) -> ByteArray) {
    //jarfile需要close
    val destFile = File("$absolutePath.temp")
    JarFile(this).review(destFile,repair)
    delete()
    FileUtils.moveFile(destFile, this)
}

/**
 * 处理jar 从srcFile复制到destFile ，review这个过程，中间 俺需要修改jar 对byteArray做一次处理
 * 1, 遍历jar中的jarEntry
 * 2, putNextEntry一个空的zipEntry对应jarEntry 到jaroutputstrean
 * 3，往jarOutputStream写入jarEntry的byre数据
 * 4，关闭closeEntry
 */
inline fun JarFile.review(destFile: File, jarWizard: (srcJarEntry: JarEntry, bytes: ByteArray) -> ByteArray) {
    //jarfile需要close
    this.use { jarFile ->
        JarOutputStream(destFile.touch().outputStream()).use { jarOutputStream: JarOutputStream ->
            jarFile.entries().asIterator().forEach { jarEntry ->
                //先添加entry
                jarOutputStream.putNextEntry(ZipEntry(jarEntry.name))
                //读取jarEntry中的流 进行处理
                jarFile.getInputStream(jarEntry).use { inputStream ->
                    if (jarEntry.name.skipByFileName()) {
                        jarOutputStream.write(inputStream.readBytes())
                    } else {
                        "jarEntry.reivew > $jarEntry ".log()
                        //写入到 目标jar流
                        val newBytes = jarWizard(jarEntry, inputStream.readBytes())
                        jarOutputStream.write(newBytes)
                    }
                    //finish 一个jarEntry的写入
                    jarOutputStream.closeEntry()
                }
            }
        }
    }
}

inline fun JarFile.scan(jarWizard: (srcJarEntry: JarEntry) -> Unit) {
    //jarfile需要close
    this.use { jarFile ->
        jarFile.entries().asIterator().forEach { jarEntry ->
            "jarEntry.scan > $jarEntry ".sout()
            jarWizard(jarEntry)
        }
    }
}

/**
 * review InputStream 转换成byteArray 同时利用asm对byteArray做一些处理
 */
inline fun InputStream.review(wizard: (ByteArray) -> ByteArray = { it -> it }): ByteArray {
    return wizard(readBytes())
}
