package osp.surgery.plugin

import com.android.build.api.transform.TransformInvocation
import org.gradle.api.Project
import osp.surgery.helper.sout
import java.io.File
/**
 * @author yun.
 * @date 2021/9/8
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */


const val JTAG = "surgery"

class Dean {
    companion object {
        val context = Dean()
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
        val dir = Dean.context.recordDir
        val file = File(dir, name)
        if (!file.exists()) {
            " JSP create file ${file.absolutePath} ${file.createNewFile()}".sout()
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
        try {
            return file.readText()
        } catch (e: Exception) {
           return ""
        }
    }

    fun save(key: String) {
        try {
            file.writeText(key)
        } catch (e: Exception) {
        }
    }
}

