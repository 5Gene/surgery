package trycatch

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type


class TryCatchDemo {
    fun errorVoidTryCatch(input: String) {
        try {
            println(input)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun errorVoid(input: String) {
        println(input)
    }

    fun errorDouble(input: String): Double {
        println(input)
        return .1
    }

    fun errorDoubleTryCatch(input: String): Double {
        try {
            println(input)
            if (input.length > 1) {
                throw RuntimeException(input)
            }
            return .1
        } catch (e: Throwable) {
            e.printStackTrace()
            return 0.0
        }
    }
}

class TryCatchClassVisitor(visitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, visitor) {
    override fun visitMethod(access: Int, name: String?, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (name == "errorVoid") {
            return MyTryCatchMethodVisitor(Opcodes.ASM9, visitMethod, Type.getReturnType(descriptor))
//            return LoggingMethodVisitor(Opcodes.ASM9, visitMethod)
        }
        return visitMethod
    }
}