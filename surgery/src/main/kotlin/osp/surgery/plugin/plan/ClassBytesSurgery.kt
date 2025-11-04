package osp.surgery.plugin.plan

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import osp.surgery.api.*
import osp.surgery.helper.*
import osp.surgery.helper.filterDuplicates
import java.io.File
import java.lang.reflect.Modifier
import java.util.*

/**
 * @author yun.
 * @date 2022/4/8
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */
class GrandFinale<DOCTOR>(
    val fileName: String,
    val compileClassName: String,
    val classByteArray: ByteArray,
    val doctors: List<DOCTOR>,
)

abstract class ClassByteSurgeryImpl<DOCTOR : ClassDoctor> : ClassBytesSurgery {
    val tag = this.javaClass.simpleName
    
    // 使用ThreadLocal.withInitial确保线程安全，并在surgeryOver时清理防止内存泄漏
    private val chiefDoctors = ThreadLocal.withInitial { mutableMapOf<String, List<DOCTOR>>() }

    //最后处理的文件线程可能会变，临时保存一份，后续取来用
    @Volatile
    private var lastDoctor: List<DOCTOR>? = null

    /**
     * 可能是 ClassTreeDoctor或者ClassVisitorDoctor
     */
    private val doctors by lazy {
        loadDoctors().values
    }

    abstract fun loadDoctors(): MutableMap<String, DOCTOR>

    override fun surgeryPrepare() {
        chiefDoctors.get().clear()
        doctors.forEach {
            it.surgeryPrepare()
        }
    }

    override fun filterByJar(jar: File): FilterAction {
        if (doctors.isEmpty()) {
            return FilterAction.noTransform
        }
        val result = filterDoctors(doctors) { it.filterByJar(jar) }
        return when {
            result.last.isNotEmpty() -> FilterAction.transformLast
            result.now.isNotEmpty() -> FilterAction.transformNow
            else -> FilterAction.noTransform
        }
    }

    /**
     * 一个线程处理一个jar 这个方式是jar遍历jarEntry的时候执行的
     */
    override fun filterByClassName(
        fileName: String,
        compileClassName: String,
    ): FilterAction {
        if (doctors.isEmpty()) {
            return FilterAction.noTransform
        }
        val result = filterDoctors(doctors) { 
            it.filterByClassName(fileName, compileClassName) 
        }
        val operatingSurgeons = result.allTransform
        if (operatingSurgeons.isNotEmpty()) {
            chiefDoctors.get()[fileName] = operatingSurgeons
            if (result.last.isNotEmpty()) {
                lastDoctor = operatingSurgeons
                //只要有最后处理的,就放最后处理,此次不处理
                return FilterAction.transformLast
            }
            return FilterAction.transformNow
        }
        return FilterAction.noTransform
    }

    override fun surgery(fileName: String, classFileByte: ByteArray): ByteArray {
        //如果是最后处理的话可能线程会变
        //如果不是最后处理，那么过滤完要处理的时候同一个线程立刻会处理执行surgery,也就是说非最后处理的filterByClassName和surgery方法在同一线程执行
        (chiefDoctors.get().remove(fileName) ?: lastDoctor)?.apply {
            return doSurgery(this, classFileByte)
        }
        return classFileByte
    }

    abstract fun doSurgery(doctors: List<DOCTOR>, classFileByte: ByteArray): ByteArray

    override fun surgeryOver() {
        if (doctors.isEmpty()) {
            return
        }
        try {
            "👇👇👇👇👇 $this surgeryOver 👇👇👇👇👇".sout()
            chiefDoctors.get().clear()
            doctors.forEach {
                it.surgeryOver()
            }
            "👆👆👆👆👆 $this surgeryOver 👆👆👆👆👆".sout()
        } finally {
            // 清理ThreadLocal，防止内存泄漏
            chiefDoctors.remove()
        }
    }
}

class ClassTreeSurgery : ClassByteSurgeryImpl<ClassTreeDoctor>() {

    override fun loadDoctors(): MutableMap<String, ClassTreeDoctor> {
        "👇👇👇👇👇 $tag : loadDoctors 👇👇👇👇👇".sout()
        val doctors = DoctorRegistry.loadDoctors(ClassTreeDoctor::class.java)
        if (doctors.isEmpty()) {
            "👆👆👆👆👆 $tag : loadDoctors (empty) 👆👆👆👆👆".sout()
            return mutableMapOf()
        }
        return doctors
            .asSequence()
            .filterDuplicates()
            .map {
                " # $tag === ClassTreeSurgery ==== ${it.javaClass.name}".sout()
                it.className to it
            }
            .toMap()
            .toMutableMap()
            .also {
                "👆👆👆👆👆 $tag : loadDoctors 👆👆👆👆👆".sout()
            }
    }

    override fun doSurgery(doctors: List<ClassTreeDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isEmpty()) {
            return classFileByte
        }
        // ClassWriter.COMPUTE_FRAMES: 自动计算操作数栈、局部变量表大小和StackMapFrames
        // ClassWriter.COMPUTE_MAXS: 自动计算操作数栈和局部变量表大小
        try {
            return ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS).also { writer ->
                doctors.fold(ClassNode().also { originNode ->
                    ClassReader(classFileByte).accept(
                        originNode, ClassReader.SKIP_DEBUG or ClassReader.EXPAND_FRAMES
                    )
                }) { classNode, doctor ->
                    try {
                        if (Modifier.isInterface(classNode.access)) {
                            "$tag > skip class [is interface] ${classNode.name}".sout()
                            classNode
                        } else {
                            doctor.surgery(classNode)
                        }
                    } catch (e: Exception) {
                        val exception = SurgeryException.DoctorExecutionException(
                            doctor.javaClass.simpleName,
                            classNode.name,
                            e
                        )
                        "$tag >>> error >>> ${exception.message}".sout()
                        classNode
                    }
                }.accept(writer)
            }.toByteArray()
        } catch (e: Exception) {
            val exception = SurgeryException.BytecodeReadException("unknown", e)
            "$tag >>> error >>> ${exception.message}".sout()
            return classFileByte
        }
    }
}

class ClassVisitorSurgery : ClassByteSurgeryImpl<ClassVisitorDoctor>() {
    override fun loadDoctors(): MutableMap<String, ClassVisitorDoctor> {
        "👇👇👇👇👇 $tag : loadDoctors 👇👇👇👇👇".sout()
        val doctors = DoctorRegistry.loadDoctors(ClassVisitorDoctor::class.java)
        if (doctors.isEmpty()) {
            "👆👆👆👆👆 $tag : loadDoctors (empty) 👆👆👆👆👆".sout()
            return mutableMapOf()
        }
        return doctors
            .asSequence()
            .filterDuplicates()
            .map {
                " # $tag === ClassVisitorSurgery ==== ${it.javaClass.simpleName}".sout()
                it.className to it
            }
            .toMap()
            .toMutableMap()
            .also {
                "👆👆👆👆👆 $tag : loadDoctors 👆👆👆👆👆".sout()
            }
    }

    override fun doSurgery(doctors: List<ClassVisitorDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isEmpty()) {
            return classFileByte
        }
        // ClassWriter.COMPUTE_FRAMES: 自动计算操作数栈、局部变量表大小和StackMapFrames
        // ClassWriter.COMPUTE_MAXS: 自动计算操作数栈和局部变量表大小
        try {
            return ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS).also {
                ClassReader(classFileByte).accept(doctors.fold(it as ClassVisitor) { acc, doctor ->
                    try {
                        doctor.surgery(acc)
                    } catch (e: Exception) {
                        val exception = SurgeryException.DoctorExecutionException(
                            doctor.javaClass.simpleName,
                            "unknown",
                            e
                        )
                        "$tag >>> error >>> ${exception.message}".sout()
                        acc
                    }
                    // ClassReader.SKIP_DEBUG: 跳过调试信息，提高处理速度，减小字节码大小
                    // ClassReader.EXPAND_FRAMES: 展开栈帧，简化字节码操作，特别是需要修改栈帧的操作
                }, ClassReader.SKIP_DEBUG or ClassReader.EXPAND_FRAMES)
            }.toByteArray()
        } catch (e: Exception) {
            val exception = SurgeryException.BytecodeReadException("unknown", e)
            "$tag >>> error >>> ${exception.message}".sout()
            return classFileByte
        }
    }
}

