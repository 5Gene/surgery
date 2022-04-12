package sparkj.surgery.plan

import org.objectweb.asm.ClassVisitor
import sparkj.surgery.more.FilterAction
import org.objectweb.asm.tree.ClassNode
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

interface ClassTreeDoctor : ClassDoctor {
    fun surgery(classNode: ClassNode): ClassNode
}

interface ClassVisitorDoctor : ClassDoctor {
    fun surgery(visitor: ClassVisitor): ClassVisitor
}