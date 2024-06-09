package trycatch

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import kotlin.random.Random

object Trace {

    @JvmStatic
    fun beginTrace(msg: String) {
        println(msg)
    }

    @JvmStatic
    fun endTrace() {

    }
}

class TryCatchDemo {

    fun trr() {
//        Trace.beginTrace("aa")
//        Trace.endTrace()
        try {
//            Trace.beginTrace("aa")
//            Trace.endTrace()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun errorVoidTryCatch(input: String): String {
        try {
            if (Random.nextBoolean()) {
                throw RuntimeException("throw error")
            }
            println(input)
            return "aa"
        } catch (e: Throwable) {
            e.printStackTrace()
            return "throwable"
        }
    }

    fun errorVoid(input: String) {
        val start = System.currentTimeMillis()
        val a = 90
        val d = a / 5
        println(input)
        val coust = System.currentTimeMillis() - start
        println("error viod cost:$coust")
    }

    fun funInt(input: String): Int = 0

    fun errorDouble(input: String): Double {
        funInt(input)
        val a = input
        println(input)
        println(input)
        println(input)
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
    var className = ""
    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name
    }

    override fun visitMethod(access: Int, name: String?, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (name == "errorDoubleTryCatch") {
//            return MyTryCatchMethodVisitor(Opcodes.ASM9, visitMethod, Type.getReturnType(descriptor))
//            return LoggingMethodVisitor(Opcodes.ASM9, visitMethod)
            return TimeCostMethodVisitor2(Opcodes.ASM9, "$className#$name", visitMethod, access, name, descriptor)
//        } else {
//            return visitMethod
        }
        return MyTryCatchMethodVisitor(Opcodes.ASM9, visitMethod, descriptor)
//        if (name == "errorDouble") {
//            return MyTryCatchMethodVisitor(Opcodes.ASM9, visitMethod, Type.getReturnType(descriptor))
////            return LoggingMethodVisitor(Opcodes.ASM9, visitMethod)
//        }
//        if (name == "errorDoubleTryCatch") {
////            return MyTryCatchMethodVisitor(Opcodes.ASM9, visitMethod, Type.getReturnType(descriptor))
//            return LoggingMethodVisitor(Opcodes.ASM9, visitMethod)
//        }
//        return visitMethod
    }
}