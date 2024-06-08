package com.andoter.asm_example.viewlog

import org.objectweb.asm.ClassWriter

class LogToView {
    fun log(message: String) {
        println(message)
    }

    fun logI(message: String, int: Int): Int {
        println(message)
        return 9
    }
}

fun main() {
    ClassWriter(ClassWriter.COMPUTE_MAXS)
}