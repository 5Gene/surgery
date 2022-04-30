package ospl.surgery.api

import java.io.File
import java.util.*

/**
 * @author yun.
 * @date 2022/4/8
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
inline fun <reified T> Any?.safeAs(): T? = this as? T

enum class Status {
    /**
     * The file was not changed since the last build.
     */
    NOTCHANGED,

    /**
     * The file was added since the last build.
     */
    ADDED,

    /**
     * The file was modified since the last build.
     */
    CHANGED,

    /**
     * The file was removed since the last build.
     */
    REMOVED
}

interface ClassBytesSurgery {
    fun surgeryPrepare()
    fun filterByJar(jar: File): FilterAction
    fun filterByClassName(src: File, dest: File, isJar: Boolean, fileName: String, status: Status, className: () -> String): FilterAction
    fun surgery(classFileByte: ByteArray): ByteArray
    fun surgeryOver()
}
