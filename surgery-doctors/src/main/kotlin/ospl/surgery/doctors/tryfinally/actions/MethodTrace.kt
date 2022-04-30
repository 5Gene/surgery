package ospl.surgery.doctors.tryfinally.actions

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.AdviceAdapter
import ospl.surgery.doctors.tryfinally.TryFinally

/**
 * @author yun.
 * @date 2022/4/28
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
class MethodTrace : TryFinally {
    override fun methodEnter(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        val tag = "$className -> $methodName".let {
//            println(it)
            it.substring(0.coerceAtLeast(it.length - 126))
        }
        mv.visitLdcInsn(tag)
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/Trace", "beginSection", "(Ljava/lang/String;)V", false)
    }

    override fun methodExit(className: String, methodName: String, mv: MethodVisitor, adapter: AdviceAdapter) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/Trace", "endSection", "()V", false)
    }

}