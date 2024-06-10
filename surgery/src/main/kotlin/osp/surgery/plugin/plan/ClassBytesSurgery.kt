package osp.surgery.plugin.plan

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import osp.surgery.api.*
import osp.surgery.helper.*
import java.io.File
import java.lang.reflect.Modifier
import java.util.*
import kotlin.concurrent.getOrSet

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
    private val chiefDoctors = ThreadLocal<MutableMap<String, List<DOCTOR>>>()

    //最后处理的文件线程可能会变，临时保存一份，后续取来用
    @Volatile
    private var lastDoctor: List<DOCTOR>? = null

    /**
     * 可能是 ClassTreeDoctor或者ClassTreeDoctor
     */
    private val doctors by lazy {
        loadDoctors().values
    }

    abstract fun loadDoctors(): MutableMap<String, DOCTOR>

    override fun surgeryPrepare() {
        chiefDoctors.set(mutableMapOf())
        doctors.forEach {
            it.surgeryPrepare()
        }
    }

    override fun filterByJar(jar: File): FilterAction {
        if (doctors.isEmpty()) {
            return FilterAction.noTransform
        }
        val grouped = doctors.groupBy {
            it.filterByJar(jar)
        }
        if (!grouped[FilterAction.transformLast].isNullOrEmpty()) {
            return FilterAction.transformLast
        } else if (grouped[FilterAction.transformNow].isNullOrEmpty()) {
            return FilterAction.noTransform
        }
        return FilterAction.transformNow
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
        val grouped = doctors.groupBy {
            it.filterByClassName(fileName, compileClassName)
        }
        val nowGroup = grouped[FilterAction.transformNow].orEmpty()
        val lastGroup = grouped[FilterAction.transformLast].orEmpty()
        val operatingSurgeons = nowGroup + lastGroup
        if (operatingSurgeons.isNotEmpty()) {
            chiefDoctors.getOrSet {
                mutableMapOf()
            }[fileName] = operatingSurgeons
            if (lastGroup.isNotEmpty()) {
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
        (chiefDoctors.get()?.remove(fileName) ?: lastDoctor)?.apply {
            return doSurgery(this, classFileByte)
        }
        return classFileByte
    }

    abstract fun doSurgery(doctors: List<DOCTOR>, classFileByte: ByteArray): ByteArray

    override fun surgeryOver() {

        "👇👇👇👇👇 $this surgeryOver 👇👇👇👇👇".sout()
        chiefDoctors.get()?.clear()
        doctors.forEach {
            it.surgeryOver()
        }
        "👆👆👆👆👆 $this surgeryOver 👆👆👆👆👆".sout()
    }
}

class ClassTreeSurgery : ClassByteSurgeryImpl<ClassTreeDoctor>() {

    override fun loadDoctors(): MutableMap<String, ClassTreeDoctor> {
        //利用SPI 全称为 (Service Provider Interface) 查找 实现类
        val classTreeDoctors = ServiceLoader.load(ClassTreeDoctor::class.java)
        if (!classTreeDoctors.iterator().hasNext()) {
            return mutableMapOf()
        }
        "👇👇👇👇👇 $tag : loadDoctors 👇👇👇👇👇".sout()
        val supers = mutableListOf<String>()
        return classTreeDoctors.iterator().asSequence().onEach {
            supers.add(it.javaClass.superclass.name)
        }.filter {
            !supers.contains(it.javaClass.name)
        }.map {
            " # $tag === ClassTreeSurgery ==== ${it.javaClass.name}".sout()
            it.className to it
        }.toMap().toMutableMap().also {
            "👆👆👆👆👆 $tag : loadDoctors 👆👆👆👆👆".sout()
        }
    }

    override fun doSurgery(doctors: List<ClassTreeDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isEmpty()) {
            return classFileByte
        }
//        ClassWriter.COMPUTE_MAXS
//        这种方式会自动计算上述 操作数栈和局部变量表的大小 但需要手动触发
//        通过调用org.objectweb.asm.commons.LocalVariablesSorter#visitMaxs
//        触发 参数可以随便写
//        ClassWriter.COMPUTE_FRAMES
//        不仅会计算上述 操作数栈和局部变量表的大小 还会自动计算StackMapFrames
        try {
//            https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-main:build-system/gradle-core/src/main/java/com/android/build/gradle/internal/instrumentation/FixFramesClassWriter.kt
//            FixFramesClassWriter(
//                classReader,
//                getClassWriterFlags(containsJsrOrRetInstruction),
//                classesHierarchyResolver,
//                issueHandler
//            )
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
                        "$tag >>> error >>> ${classNode.name} > ${e.message}".sout()
                        classNode
                    }
                }.accept(writer)
            }.toByteArray()
        } catch (e: Exception) {
            "$tag >>> error >>> [byte to asm] > ${e.message}".sout()
            return classFileByte
        }
    }
}

class ClassVisitorSurgery : ClassByteSurgeryImpl<ClassVisitorDoctor>() {
    override fun loadDoctors(): MutableMap<String, ClassVisitorDoctor> {
        val classVisitorDoctors = ServiceLoader.load(ClassVisitorDoctor::class.java)
        if (!classVisitorDoctors.iterator().hasNext()) {
            return mutableMapOf()
        }
        "👇👇👇👇👇 $tag : loadDoctors 👇👇👇👇👇".sout()
        //利用SPI 全称为 (Service Provider Interface) 查找 实现类
        val supers = mutableListOf<String>()
        return classVisitorDoctors.iterator().asSequence().onEach {
            supers.add(it.javaClass.superclass.name)
        }.filter {
            !supers.contains(it.javaClass.name)
        }.map {
            " # $tag === ClassVisitorSurgery ==== ${it.javaClass.simpleName}".sout()
            it.className to it
        }.toMap().toMutableMap().also {
            "👆👆👆👆👆 $tag : loadDoctors 👆👆👆👆👆".sout()
        }
    }

    override fun doSurgery(doctors: List<ClassVisitorDoctor>, classFileByte: ByteArray): ByteArray {
        if (doctors.isEmpty()) {
            return classFileByte
        }
//        ClassWriter.COMPUTE_MAXS
//        这种方式会自动计算上述 操作数栈和局部变量表的大小 但需要手动触发
//        通过调用org.objectweb.asm.commons.LocalVariablesSorter#visitMaxs
//        触发 参数可以随便写
//        ClassWriter.COMPUTE_FRAMES
//        不仅会计算上述 操作数栈和局部变量表的大小 还会自动计算StackMapFrames
        //https://www.jianshu.com/p/abd1b1b8d3f3
        //https://www.kingkk.com/2020/08/ASM%E5%8E%86%E9%99%A9%E8%AE%B0/
        //com.android.build.gradle.internal.instrumentation.FixFramesClassWriter
        try {
//            val fixFramesClassWriter = "com.android.build.gradle.internal.instrumentation.FixFramesClassWriter"
//            val loadClass = this.javaClass.classLoader.loadClass(fixFramesClassWriter)
            return ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS).also {
                ClassReader(classFileByte).accept(doctors.fold(it as ClassVisitor) { acc, doctor ->
                    try {
                        doctor.surgery(acc)
                    } catch (e: Exception) {
                        "$tag >>> error >>> [surgery] > ${e.message}".sout()
                        acc
                    }
                    //EXPAND_FRAMES 说明在读取 class 的时候同时展开栈映射帧(StackMap Frame)
                    //ClassReader.SKIP_DEBUG：跳过调试信息，提高处理速度，减小字节码大小。
                    //ClassReader.EXPAND_FRAMES：展开栈帧，简化字节码操作，特别是需要修改栈帧的操作。
                }, ClassReader.SKIP_DEBUG or ClassReader.EXPAND_FRAMES)
            }.toByteArray()
        } catch (e: Exception) {
            "$tag >>> error >>> [byte to asm] > ${e.message}".sout()
            return classFileByte
        }
    }
}

