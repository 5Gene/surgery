package sparkj.surgery.plan

import org.objectweb.asm.ClassVisitor
import sparkj.surgery.more.FilterAction
import org.objectweb.asm.tree.ClassNode
import sparkj.surgery.more.safeAs
import sparkj.surgery.more.sout
import java.io.File

/**
 * @author yun.
 * @date 2022/4/2
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */

interface ClassDoctor {
    fun sout(msg: String) {
        " # ${this.javaClass.simpleName} >> $msg".sout()
    }

    fun surgeryPrepare()
    fun filterByJar(jar: File): FilterAction
    fun filterByClassName(file: File, className: () -> String): FilterAction
    fun surgeryOver()
}

abstract class ClassTreeDoctor : ClassDoctor {
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
    val className: String = this.javaClass.name

    final override fun equals(other: Any?): Boolean {
        return className == other.safeAs<ClassVisitorDoctor>()?.className
    }

    final override fun hashCode(): Int {
        return className.hashCode()
    }

    abstract fun surgery(visitor: ClassVisitor): ClassVisitor
}