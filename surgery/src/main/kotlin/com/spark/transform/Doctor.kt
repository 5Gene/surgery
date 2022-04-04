package com.spark.transform

import com.spark.helper.FilterAction
import org.objectweb.asm.tree.ClassNode
import java.io.File

/**
 * @author yun.
 * @date 2022/4/2
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */

interface ClassDoctor {
    fun startOperate()
    fun filterByJar(jar: File): FilterAction
    fun filterByClassName(file: File, className: () -> String): FilterAction
    fun finishOperate()
}

interface ClassTreeDoctor : ClassDoctor {
    fun operate(classNode: ClassNode): ClassNode
}

interface ClassVisitorDoctor : ClassDoctor {
    fun operate(classFileByte: ByteArray): ByteArray
}