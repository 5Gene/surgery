package com.spark

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Action
import java.io.File

/**
 * @author yun.
 * @date 2021/9/3
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
class JConstants {
    companion object {
        const val moduleClassName = "classes.jar"

        fun sout(msg : Any) {
            println(" $ Jspark > $msg")
        }
    }
}

inline fun String.isClass(): Boolean {
    return this.endsWith(".class")
}

inline fun String.isBuildConfigClass(): Boolean {
    return this.equals("BuildConfig.class")
}

inline fun String.isBindingClass(): Boolean {
    return this.endsWith("Binding.class")
}

inline fun String.isArouterClass(): Boolean {
    return this.startsWith("ARouter$$")
}


inline fun String.isRClass(): Boolean {
    return this.startsWith("R") || this.contains("R$")
}

inline fun File.packageName(srcDirectory: File): String {
    val replace = this.path.replace(srcDirectory.path, "")
    return replace.substring(1).toPackageName()
}

inline fun String.toPackageName(): String {
    return substring(0, this.indexOf(".class")).replace("/|\\\\", ".")
}

fun Any.sout() {
    println(" $ Jspark > $this")
}

/**
 * review之前过滤
 * 在遍历的时候过滤jar后续不处理
 */
inline fun File.skipJar(): Boolean {
    return this.name.equals("R.jar")
}

/**
 * review之前过滤
 * 在遍历的时候过滤class后续不处理
 */
inline fun File.skipFile(): Boolean {
    return !this.name.isClass()
}

/**
 * review的时候过滤
 */
inline fun String.skipByFileName(): Boolean {
    return !endsWith(".class")||isBindingClass()||isBuildConfigClass()
}


fun org.gradle.api.Project.spark(configure: Action<com.spark.extension.Spark>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("spark", configure)





