package osp.surgery.helper

import org.apache.commons.io.FileUtils
import osp.surgery.helper.*
import java.io.File
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * @author yun.
 * @date 2022/4/30
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */

fun File.filterJar():Boolean{
    return name.startsWith("jetified-")
            ||name.startsWith("core-")
            ||name.startsWith("drawerlayout-")
            ||name.startsWith("vectordrawable-")
            ||name.startsWith("dynamicanimation-")
            ||name.startsWith("localbroadcastmanager-")
            ||name.startsWith("navigation-")
            ||name.startsWith("viewpager-")
            ||name.startsWith("coordinatorlayout-")
            ||name.startsWith("legacy-")
            ||name.startsWith("loader-")
            ||name.startsWith("customview-")
            ||name.startsWith("recyclerview-")
            ||name.startsWith("recyclerview-")
            ||name.startsWith("swiperefreshlayout-")
            ||name.startsWith("transition-")
            ||name.startsWith("cardview-")
            ||name.startsWith("slidingpanelayout-")
            ||name.startsWith("versionedparcelable-")
            ||name.startsWith("constraintlayout-")
            ||name.startsWith("material-")
            ||name.startsWith("appcompat-")
            ||name.startsWith("annotation-")
            ||name.startsWith("lifecycle-")
            ||name.startsWith("print-")
            ||name.startsWith("collection-")
            ||name.startsWith("cursoradapter-")
            ||name.startsWith("media-")
            ||name.startsWith("asynclayoutinflater-")
            ||name.startsWith("fragment-")
            ||name.startsWith("interpolator-")
}

/**
 * review之前过滤
 * 在遍历的时候过滤jar后续不处理
 */
fun File.skipJar(): Boolean {
    //0.jar里都是R.class, R$xxx.class, 0.jar只在app里先用variants.instrumentation.transformClassesWith执行transform才出现
    //源码依赖的模块,都是class.jar
    return this.name.equals("R.jar") || this.name == "0.jar"
}

fun File.packageName(srcDirectory: File): String {
    val replace = this.path.replace(srcDirectory.path, "")
    return replace.substring(1).toPackageName()
}

fun File.isModuleJar(): Boolean {
    return name.isModuleJar()
}

/**
 * review之前过滤
 * 在遍历的时候过滤class后续不处理
 */
fun File.skipFile(): Boolean {
    return name.skipByFileName()
}

fun File.className(parent: File?): String {
    return parent!!.toURI().relativize(toURI()).normalize().path.className()
}

fun File.isJar(): Boolean {
    return name.isJar()
}

fun File.isClass(): Boolean {
    return name.isClass()
}

fun File.touch(): File {
    FileUtils.touch(this)
    return this
}

inline fun File.repair(repair: (ByteArray) -> ByteArray) {
    "👇👇👇👇👇 repair File 👇👇👇👇👇".sout()
    val destFile = File("$absolutePath.temp")
    review(destFile, repair)
    delete()
    FileUtils.moveFile(destFile, this)
    "👆👆👆👆👆 repair File 👆👆👆👆👆".sout()
}

//处理class文件 从srcFile复制到destFile ，review这个过程，中间对byteArray做一次处理
inline fun File.review(destFile: File, wizard: (ByteArray) -> ByteArray) {
    destFile.touch().outputStream().use { outputStream ->
        this.inputStream().use {
            if (this.name.skipByFileName()) {
                " File.review > skip class ${this.name}".log()
                outputStream.write(it.readBytes())
            } else {
                outputStream.write(it.review(wizard))
            }
        }
    }
}

inline fun File.repairJar(repair: (srcJarEntry: JarEntry, bytes: ByteArray) -> ByteArray) {
    //jarfile需要close
    "👇👇👇👇👇 repair Jar 👇👇👇👇👇".sout()
    val destFile = File("$absolutePath.temp")
    JarFile(this).review(destFile, repair)
    delete()
    FileUtils.moveFile(destFile, this)
    "👆👆👆👆👆 repair Jar 👆👆👆👆👆".sout()
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
                        " JarFile.review > skip class ${jarEntry.name}".log()
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