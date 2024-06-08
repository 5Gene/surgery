package transform

import com.andoter.asm_example.part2.ClassPrintVisitor
import com.andoter.asm_example.part3.MyLoader
import com.andoter.asm_example.utils.ClassOutputUtil
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode


class T {
    fun ttt2(i: Int, s: String): Int {
        return 0
    }

    fun ttt(i: Double) {}
    fun tttStr(i: Double): String = "wwwww"
    fun tttAae(vararg i: Double?): String? = "wwwwwww"

    fun retDou(i: Int, d: Double): Array<String>? = null

    fun retDous(i: Int, d: Double, s: String): Double = 0.1

    fun methoObj(aa: Int, ll: List<String>?): Double {
        return 0.1
    }
}

object Change {
    fun retlist(string: String?): List<String>? {
        return emptyList<String>()
    }

    fun retlist(int: Int?): Map<String, String>? {
        return emptyMap()
    }

    fun retInt(int: Int?): Double {
        return 0.9
    }

    @JvmStatic
    fun retIt(int: Int?): Long {
        return 0L
    }

    fun returnV(int: Double?, D: Int) {
    }

    fun returnArr(int: Array<String>) {
    }

    fun varar(vararg int: String?) {
    }


    @JvmStatic
    fun use(beUse: BeUse, msg: String) {
        println("msg from Change= [${msg}]")
    }

    @JvmStatic
    fun use(msg: String) {
        println("msg from Change= [${msg}]")
    }

    @JvmStatic
    fun one(msg: String) {
        println("msg from Change= [${msg}]")
    }

    init {
        println("")
    }
}

open class PP(string: String, intt: Int) {
    init {
        println("9000")
        println("9000")
        println("9000")
        println("9000")
    }
}
class Child(string: String) : PP(string, 9) {}
open class BeUse {
    fun use(msg: String) {
        println(msg)
        println("---------- from BeUse ")
    }
}

object Origin : PP(",", 0) {

    init {
        println("9000")
        println("9000")
        println("9000")
        println("9000")
    }

    val beUse = BeUse()

    fun testDo(beUse: BeUse) {
        beUse.use("xx")
        val a = "xxxxx"
        Change.use(beUse, a)
    }

    fun testDoField(msg: String) {
        beUse.use(msg)
    }

    fun testNewB(msg: String) {
        val beUse = BeUse()
        beUse.use("msg")
        Change.one("")
    }

    fun retlist(string: String?): List<String>? {
        return emptyList<String>()
//        return HashMap()
    }
}


class ChangeVisitor(classWriter: ClassWriter) : ClassVisitor(Opcodes.ASM9, classWriter) {

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        println(name)
        val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (name == "<init>") {
            return object : MethodVisitor(Opcodes.ASM9, visitMethod) {
                override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
                    if (name != "<init>") {
                        visitEnd()
                        return
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                }

                override fun visitInsn(opcode: Int) {
                    super.visitInsn(opcode)
                }
            }

        }
        if (name == "retlist") {
            println("xxx $descriptor")
            return object : MethodVisitor(Opcodes.ASM9) {
                override fun visitCode() {
                    visitMethod.visitMethodInsn(Opcodes.INVOKESTATIC, "kotlin/collections/MapsKt", "emptyMap", "()Ljava/util/Map;", false)
                    visitMethod.visitInsn(Opcodes.ARETURN)
                    //必须设置Stack 否则会报：java.lang.VerifyError: Operand stack overflow
                    visitMethod.visitMaxs(1, 1)
                }
            }
        }
        if (name == "testDo" || name == "testDoField" || name == "testNewB") {
            return object : MethodVisitor(Opcodes.ASM9, visitMethod) {
                override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
                    println("opcode = [${opcode}], owner = [${owner}], name = [${name}], descriptor = [${descriptor}], isInterface = [${isInterface}]")
                    if (name == "use") {
                        super.visitMethodInsn(
                            Opcodes.INVOKESTATIC,
                            "transform/Change",
                            "use",
                            "(Ltransform/BeUse;Ljava/lang/String;)V",
                            false
                        )
                    } else {
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                    }
                }
            }
        }
        return visitMethod
    }
}

fun main() {
    val classReader = ClassReader("transform/Origin")
    val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
    val classVisitor = ChangeVisitor(classWriter)
    classReader.accept(classVisitor, ClassReader.SKIP_DEBUG)

    println("===== 处理后的信息  ======")
    val printVisitor = ClassPrintVisitor(Opcodes.ASM9)
    val printReader = ClassReader(classWriter.toByteArray())
    printReader.accept(printVisitor, ClassReader.SKIP_DEBUG)

    //输出文件查看
    ClassOutputUtil.byte2File("asm_example/files/Change.class", classWriter.toByteArray())

    val defineClass = MyLoader().defineClass("transform.Origin", classWriter.toByteArray())!!
    val origin = defineClass.newInstance()
    println("--=====================================")
    (defineClass.getMethod("testNewB", String::class.java).invoke(origin, "11"))
    (defineClass.getMethod("testDoField", String::class.java).invoke(origin, "11"))

    println("(Ljava/lang/String;)V".substring(1))

    val classReader2 = ClassReader("T")
    val classNode: ClassNode = ClassNode()
//    classReader2.accept(classNode)

    for (methodNode in classNode.methods) {
        System.out.println("方法名：" + methodNode.name)
        System.out.println("maxLocals：" + methodNode.maxLocals)
        System.out.println("maxStack：" + methodNode.maxStack)
        println("----------------------")
    }
}