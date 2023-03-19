package osp.surgery.api

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.tree.ClassNode
import java.io.File

/**
 * @author yun.
 * @date 2022/4/30
 */

interface ClassDoctor {

    fun sout(msg: String) {
        println(" # ${this.javaClass.simpleName} >> $msg")
    }

    fun surgeryPrepare()
    fun filterByJar(jar: File): FilterAction
    fun filterByClassName(file: File, className: () -> String): FilterAction
    fun surgeryOver()
}

abstract class ClassTreeDoctor : ClassDoctor {
    val tag = this.javaClass.simpleName
    val className: String = this.javaClass.name

    final override fun equals(other: Any?): Boolean {
        return className == other.safeAs<ClassTreeDoctor>()?.className
    }

    final override fun hashCode(): Int {
        return className.hashCode()
    }

    abstract fun surgery(classNode: ClassNode): ClassNode
}

abstract class ClassVisitorDoctor : ClassDoctor {
    val tag = this.javaClass.simpleName
    val className: String = this.javaClass.name

    final override fun equals(other: Any?): Boolean {
        return className == other.safeAs<ClassVisitorDoctor>()?.className
    }

    final override fun hashCode(): Int {
        return className.hashCode()
    }

    abstract fun surgery(visitor: ClassVisitor): ClassVisitor
}
