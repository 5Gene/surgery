package com.spark.review

import com.android.build.api.transform.TransformInvocation
import org.gradle.api.Project
import java.io.File
import com.spark.*
/**
 * @author yun.
 * @date 2021/9/8
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
class SparkEnvironment() {
    companion object {
        val environment = SparkEnvironment()
    }

    var transformInvocation: TransformInvocation? = null
    var transformRootPath: String? = null
    var project: Project? = null

    val recordDir: File by lazy {
        File(project!!.buildDir, "record").also {
            "create record dir ${it.mkdirs()}".sout()
        }
    }

    fun release() {
        project = null
        transformInvocation = null
    }
}

class JSP(val name: String) {
    val file: File by lazy {
        val dir = SparkEnvironment.environment.recordDir
        val file = File(dir, name)
        if (!file.exists()) {
            file.createNewFile()
        }
        file
    }

    fun get(key: String): String {
        return ""
    }

    fun put(key: String, value: String) {
//        file.writeText(key)
    }

    fun read(): String {
        return file.readText()
    }

    fun save(key: String) {
        file.createNewFile()
        file.writeText(key)
    }
}

