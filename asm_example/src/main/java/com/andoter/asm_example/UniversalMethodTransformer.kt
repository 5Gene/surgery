package com.andoter.asm_example

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AdviceAdapter

class UniversalMethodTransformer(
    api: Int,
    methodVisitor: MethodVisitor,
    access: Int,
    name: String,
    descriptor: String
) : AdviceAdapter(api, methodVisitor, access, name, descriptor) {

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String,
        descriptor: String,
        isInterface: Boolean
    ) {
        if (opcode == Opcodes.INVOKEVIRTUAL || opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEINTERFACE) {
            // 获取方法参数的类型信息
            val argTypes = Type.getArgumentTypes(descriptor)
            val returnType = Type.getReturnType(descriptor)

            // 创建新的方法描述符，增加对象的类型作为第一个参数
            val newArgTypes = arrayOf(Type.getObjectType(owner)) + argTypes
            val newDescriptor = Type.getMethodDescriptor(returnType, *newArgTypes)

            // 将对象引用和参数按顺序推入操作数栈
            for (i in argTypes.indices.reversed()) {
                mv.visitVarInsn(ALOAD, i + 1) // 加载参数
            }
            mv.visitVarInsn(ALOAD, 0) // 加载对象引用

            // 调用新的静态方法
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "S",
                name,
                newDescriptor,
                false
            )
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }
    }
}

class UniversalClassTransformer(api: Int, classVisitor: ClassVisitor) : ClassVisitor(api, classVisitor) {
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        return UniversalMethodTransformer(api, mv, access, name, descriptor)
    }
}

fun transformClass(inputClass: ByteArray): ByteArray {
    val classReader = ClassReader(inputClass)
    val classWriter = ClassWriter(classReader, 0)
    val classVisitor = UniversalClassTransformer(Opcodes.ASM9, classWriter)
    classReader.accept(classVisitor, 0)
    return classWriter.toByteArray()
}

fun main() {
    // 加载要转换的类
//    val inputClass = java.getResourceAsStream("/Main.class").readBytes()
//    val transformedClass = transformClass(inputClass)
//
//    // 保存转换后的类到文件
//    val outputFile = java.io.File("MainTransformed.class")
//    outputFile.writeBytes(transformedClass)
}