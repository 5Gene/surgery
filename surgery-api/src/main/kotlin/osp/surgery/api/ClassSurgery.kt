package osp.surgery.api

import java.io.File
import java.util.*

/**
 * @author yun.
 * @date 2022/4/8
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */
inline fun <reified T> Any?.safeAs(): T? = this as? T

interface ClassBytesSurgery {
    fun surgeryPrepare()
    fun filterByJar(jar: File): FilterAction
    fun filterByClassName(
        fileName: String,
        compileClassName: String,
    ): FilterAction

    fun surgery(fileName: String, classFileByte: ByteArray): ByteArray
    fun surgeryOver()
}
