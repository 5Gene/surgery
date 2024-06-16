package osp.surgery.helper

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.T_BYTE
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import java.lang.reflect.Modifier
import kotlin.reflect.KClass

/**
 * @author yun.
 * @date 2022/4/30
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */

const val JAPI = Opcodes.ASM9


/**
 * ## 如果添加日志的话
 * - MAXSTACK 要➕maxStack
 * - MAXLOCALS 要➕maxLocals
 *
 * @return (maxStack, maxLocals)
 */
fun MethodVisitor.insertDefaultReturn(access: Int, methodDesc: String): Pair<Int, Int> {
    val isStaticMethod: Boolean = (access and Opcodes.ACC_STATIC) != 0
    //静态方法不需要加载this
    var maxLocals: Int = if (isStaticMethod) 0 else 1
    val arguments = Type.getArgumentTypes(methodDesc)
    arguments.forEach {
        if (it == Type.DOUBLE_TYPE || it == Type.LONG_TYPE) {
            //long 或 double 类型，它们占用两个索引。
            maxLocals += 2
        } else {
            maxLocals += 1
        }
    }

    //方法的操作数栈 (maxStack)
    //操作数栈用于执行字节码指令时的中间结果。计算 maxStack 的关键是跟踪每条字节码指令对栈的影响（入栈和出栈操作），
    // 并找出操作数栈在方法执行过程中达到的最大深度。
    //计算 maxStack:
    //  - 分析方法体的字节码，跟踪每条指令对栈的影响（入栈和出栈操作）。
    //  - 找出操作数栈在执行过程中达到的最大深度。
    //常量加载指令（如 iconst_0、ldc）会将常量压入栈中，增加栈深度。
    //返回指令（如 ireturn、dreturn）会弹出栈顶元素，并在返回后栈深度变为 0。
    //方法调用前需要确保栈有足够的空间来存储参数和返回值。
    val maxStack: Int
    when (Type.getReturnType(methodDesc).sort) {
        Type.VOID -> {
            // 如果返回类型是 void，插入 RETURN 指令
            visitInsn(Opcodes.RETURN)
            maxStack = 0
        }

        Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> {
            // 对于 boolean、char、byte、short 和 int 类型，插入 ICONST_0 和 IRETURN 指令
            visitInsn(Opcodes.ICONST_0)
            visitInsn(Opcodes.IRETURN)
            maxStack = 1
        }

        Type.FLOAT -> {
            // 对于 float 类型，插入 FCONST_0 和 FRETURN 指令
            visitInsn(Opcodes.FCONST_0)
            visitInsn(Opcodes.FRETURN)
            maxStack = 1
        }

        Type.LONG -> {
            // 对于 long 类型，插入 LCONST_0 和 LRETURN 指令
            visitInsn(Opcodes.LCONST_0)
            visitInsn(Opcodes.LRETURN)
            maxStack = 2
        }

        Type.DOUBLE -> {
            // 对于 double 类型，插入 DCONST_0 和 DRETURN 指令
            visitInsn(Opcodes.DCONST_0)
            visitInsn(Opcodes.DRETURN)
            maxStack = 2
        }

        Type.ARRAY, Type.OBJECT -> {
            //Ljava/lang/String;返回值为String
            if (methodDesc.endsWith("lang/String;")) {
                // 加载空字符串常量到操作数栈
                visitLdcInsn("def from knife plugin")
            } else if (methodDesc.endsWith("java/util/List;")) {
                //返回空list列表
                visitMethodInsn(Opcodes.INVOKESTATIC, "kotlin/collections/CollectionsKt", "emptyList", "()Ljava/util/List;", false)
            } else if (methodDesc.endsWith("java/util/Map;")) {
                //返回空map集合
                visitMethodInsn(Opcodes.INVOKESTATIC, "kotlin/collections/MapsKt", "emptyMap", "()Ljava/util/Map;", false)
            } else {
                // 对于数组和对象类型，插入 ACONST_NULL 和 ARETURN 指令
                visitInsn(Opcodes.ACONST_NULL)
            }
            visitInsn(Opcodes.ARETURN)
            maxStack = 1
        }

        else -> throw IllegalArgumentException("不支持的返回类型:$methodDesc")
    }

    // 计算并设置最大堆栈大小和局部变量表的大小
    // 因为方法中可能有返回值的指令，所以需要合理设置堆栈和局部变量的大小
//    visitMaxs(maxStack, maxLocals)
//    要在原基础上➕
    return maxStack to maxLocals
}

/**
 * ## 如果添加日志的话
 * - MAXSTACK 要➕maxStack:2
 * - MAXLOCALS 要➕maxLocals:0
 *
 * @return (maxStack, maxLocals)
 */
fun logCode(mv: MethodVisitor, tag: String, msg: String): Pair<Int, Int> {
    return logCode(mv, "i", tag, msg)
}

/**
 * ## 如果添加日志的话
 * - MAXSTACK 要➕maxStack:2
 * - MAXLOCALS 要➕maxLocals:0
 *
 * @return (maxStack, maxLocals)
 */
fun logCode(mv: MethodVisitor, level: String, tag: String, msg: String): Pair<Int, Int> {
    //加载字符串
    mv.visitLdcInsn(tag)
    mv.visitLdcInsn(msg)
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", level, "(Ljava/lang/String;Ljava/lang/String;)I", false)
    mv.visitInsn(Opcodes.POP) //log.i有返回值 需要扔掉
    return 2 to 0
}

/**
 * ## 如果添加日志的话
 * - MAXSTACK 要➕maxStack:2
 * - MAXLOCALS 要➕maxLocals:0
 *
 * @return (maxStack, maxLocals)
 */
fun MethodVisitor.addLogCode(tag: String, msg: String): Pair<Int, Int> {
    return addLogCode("i", tag, msg) //log.i有返回值 需要扔掉
}


/**
 * ## 如果添加日志的话
 * - MAXSTACK 要➕maxStack:2
 * - MAXLOCALS 要➕maxLocals:0
 *
 * @return (maxStack, maxLocals)
 */
fun MethodVisitor.addLogCode(level: String, tag: String, msg: String): Pair<Int, Int> {
    visitLdcInsn(tag) //LDC tag 将字符串压入栈，栈深度变为 1。
    visitLdcInsn(msg) ////LDC msg 将另一个字符串压入栈，栈深度变为 2。
    //INVOKESTATIC 调用方法，消耗栈上的两个字符串，并将返回值压入栈， 栈深度变为 1。
    visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", level, "(Ljava/lang/String;Ljava/lang/String;)I", false)
    //POP 弹出栈顶的整数，栈深度变为 0。
    visitInsn(Opcodes.POP) //log.i有返回值 需要扔掉
    //因此，这段字节码的最大栈深度为 2

    //这段字节码 不需要使用任何局部变量，因此 MAXLOCALS 可以设置为 0。
    //原因：
    //  - 没有局部变量声明： 字节码中没有出现任何 STORE 指令（如 ASTORE、ISTORE 等），表示没有将值存储到局部变量表。
    //  - 参数直接传递给方法： 两个 LDC 指令将字符串常量直接加载到操作数栈，然后作为参数传递给 INVOKESTATIC 调用的方法。
    //因此，这段字节码不需要使用局部变量表来存储任何数据， MAXLOCALS 可以设置为 0

    //在实际的 Java 类文件中，MAXLOCALS 通常不会设置为 0，因为即使方法不使用局部变量，编译器也可能会为方法分配一个或多个局部变量槽， 用于存储 this 引用或其他隐式参数。
    //但是，从这段字节码片段来看，它本身不需要使用任何局部变量， 因此 MAXLOCALS 可以设置为 0。
    return 2 to 0
}


fun MethodNode.copy(): MethodNode {
    return MethodNode(JAPI, access, name, desc, signature, exceptions.toTypedArray())
}

fun InsnList.insertLogCodeBefore(ret: AbstractInsnNode, tag: String, msg: String) {
    insertLogCodeBefore(ret, "i", tag, msg)
}

fun InsnList.insertLogCodeBefore(ret: AbstractInsnNode, level: String, tag: String, msg: String) {
    insertBefore(ret, LdcInsnNode(tag))
    insertBefore(ret, LdcInsnNode(msg))
    insertBefore(
        ret,
        MethodInsnNode(Opcodes.INVOKESTATIC, "android/util/Log", level, "(Ljava/lang/String;Ljava/lang/String;)I", false)
    )
    insertBefore(ret, InsnNode(Opcodes.POP)) //log.i有返回值 扔掉
}

fun InsnList.findAll(vararg opcodes: Int) = this.filter { it.opcode in opcodes }


//setOnClickListener {
//
//}
//name = [onViewCreated$lambda-0], descriptor = [(Lop/po/apptest/FirstFragment;Landroid/view/View;)V], signature = [null], exceptions = [null]
fun isLambdaClick(methodName: String, descriptor: String, className: String) =
    methodName.contains("\$lambda-") && descriptor.endsWith("$className;Landroid/view/View;)V")

//
//============================================================================================
//setOnClickListener { view ->
//
//}
//name = [onCreate$lambda-0], descriptor = [(Landroid/view/View;)V], signature = [null], exceptions = [null]
fun isLambdaWithParamClick(methodName: String, descriptor: String, className: String) = methodName.contains("\$lambda-") &&
        descriptor == "(Landroid/view/View;)V"

//
//==============================================================================================
//setOnClickListener { object :View.OnClickListener{
//    override fun onClick(v: View?) {
//
//    }
//}
//name = [onViewCreated$lambda-0], descriptor = [(Lop/po/apptest/FirstFragment;Landroid/view/View;)V], signature = [null], exceptions = [null]
////innerClass
//    name = [onClick], descriptor = [(Landroid/view/View;)V], signature = [null], exceptions = [null]
//
//    ===================================================================================================
////setOnClickListener(this)
//    [onClick], descriptor = [(Landroid/view/View;)V], signature = [null], exceptions = [null]
fun isNormalClick(methodName: String, descriptor: String) = "onClick" == methodName &&
        descriptor == "(Landroid/view/View;)V"

fun MethodNode.isMethodIgnore(): Boolean {
    return isEmptyBody() || access.isMethodIgnore()
}

fun MethodNode.isEmptyBody(): Boolean {
    return instructions == null || instructions.size() == 0
}

fun Int.isReturn(): Boolean {
    return (this <= Opcodes.RETURN && this >= Opcodes.IRETURN)
}

fun Int.isMethodExit(): Boolean {
    return isReturn() || this == Opcodes.ATHROW
}

fun Int.isMethodInvoke(): Boolean {
    return (this <= Opcodes.INVOKEDYNAMIC && this >= Opcodes.INVOKEVIRTUAL)
}

fun Int.isMethodIgnore(): Boolean {
    return Modifier.isAbstract(this) || Modifier.isNative(this) || Modifier.isInterface(this)
}

fun publicClass(
    apiVersion: Int,
    name: InternalName,
    superName: InternalName? = null,
    interfaces: List<InternalName>? = null,
    classBody: ClassWriter.() -> Unit = {}
) = beginPublicClass(apiVersion, name, superName, interfaces).run {
    classBody()
    endClass()
}

fun beginPublicClass(
    apiVersion: Int,
    name: InternalName,
    superName: InternalName? = null,
    interfaces: List<InternalName>? = null
) = beginClass(apiVersion, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, name, superName, interfaces)

fun beginClass(
    apiVersion: Int,
    modifiers: Int,
    name: InternalName,
    superName: InternalName? = null,
    interfaces: List<InternalName>? = null
): ClassWriter = ClassWriter(ClassWriter.COMPUTE_MAXS + ClassWriter.COMPUTE_FRAMES).apply {
    visit(
        apiVersion,
        modifiers,
        name.value,
        null,
        (superName ?: InternalNameOf.javaLangObject).value,
        interfaces?.map { it.value }?.toTypedArray()
    )
}

fun ClassWriter.endClass(): ByteArray {
    visitEnd()
    return toByteArray()
}

fun ClassWriter.publicDefaultConstructor(superName: InternalName = InternalNameOf.javaLangObject) {
    publicMethod("<init>", "()V") {
        ALOAD(0)
        INVOKESPECIAL(superName, "<init>", "()V")
        RETURN()
    }
}

fun ClassVisitor.publicStaticMethod(
    name: String,
    desc: String,
    signature: String? = null,
    exceptions: Array<String>? = null,
    deprecated: Boolean = false,
    methodBody: MethodVisitor.() -> Unit,
    annotations: MethodVisitor.() -> Unit
) {
    method(
        Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + if (deprecated) {
            Opcodes.ACC_DEPRECATED
        } else {
            0
        },
        name, desc, signature, exceptions, annotations, methodBody
    )
}

fun ClassVisitor.publicMethod(
    name: String,
    desc: String,
    signature: String? = null,
    exceptions: Array<String>? = null,
    annotations: MethodVisitor.() -> Unit = {},
    methodBody: MethodVisitor.() -> Unit
) {
    method(Opcodes.ACC_PUBLIC, name, desc, signature, exceptions, annotations, methodBody)
}

fun ClassVisitor.method(
    access: Int,
    name: String,
    desc: String,
    signature: String? = null,
    exceptions: Array<String>? = null,
    annotations: MethodVisitor.() -> Unit = {},
    methodBody: MethodVisitor.() -> Unit
) {
    visitMethod(access, name, desc, signature, exceptions).apply {
        annotations()
        visitCode()
        methodBody()
        visitMaxs(0, 0)
        visitEnd()
    }
}


fun MethodVisitor.loadByteArray(byteArray: ByteArray) {
    LDC(byteArray.size)
    NEWARRAY(T_BYTE)
    for ((i, byte) in byteArray.withIndex()) {
        DUP()
        LDC(i)
        LDC(byte)
        BASTORE()
    }
}


fun MethodVisitor.ICONST_0() {
    visitInsn(Opcodes.ICONST_0)
}

fun MethodVisitor.NEW(type: InternalName) {
    visitTypeInsn(Opcodes.NEW, type)
}

fun MethodVisitor.visitTypeInsn(opcode: Int, type: InternalName) {
    visitTypeInsn(opcode, type.value)
}

fun MethodVisitor.NEWARRAY(primitiveType: Int) {
    visitIntInsn(Opcodes.NEWARRAY, primitiveType)
}

fun MethodVisitor.LDC(type: InternalName) {
    visitLdcInsn(Type.getType("L${type.value};"))
}

fun MethodVisitor.LDC(value: Any) {
    visitLdcInsn(value)
}

fun MethodVisitor.INVOKEVIRTUAL(owner: InternalName, name: String, desc: String, itf: Boolean = false) {
    visitMethodInsn_(Opcodes.INVOKEVIRTUAL, owner, name, desc, itf)
}

fun MethodVisitor.INVOKESPECIAL(owner: InternalName, name: String, desc: String, itf: Boolean = false) {
    visitMethodInsn_(Opcodes.INVOKESPECIAL, owner, name, desc, itf)
}

fun MethodVisitor.INVOKEINTERFACE(owner: InternalName, name: String, desc: String, itf: Boolean = true) {
    visitMethodInsn_(Opcodes.INVOKEINTERFACE, owner, name, desc, itf)
}

fun MethodVisitor.INVOKESTATIC(owner: InternalName, name: String, desc: String) {
    visitMethodInsn_(Opcodes.INVOKESTATIC, owner, name, desc, false)
}

private
fun MethodVisitor.visitMethodInsn_(opcode: Int, owner: InternalName, name: String, desc: String, itf: Boolean) {
    visitMethodInsn(opcode, owner.value, name, desc, itf)
}

fun MethodVisitor.BASTORE() {
    visitInsn(Opcodes.BASTORE)
}

fun MethodVisitor.DUP() {
    visitInsn(Opcodes.DUP)
}

fun MethodVisitor.POP() {
    visitInsn(Opcodes.POP)
}

fun MethodVisitor.ARETURN() {
    visitInsn(Opcodes.ARETURN)
}

fun MethodVisitor.RETURN() {
    visitInsn(Opcodes.RETURN)
}

fun MethodVisitor.ALOAD(`var`: Int) {
    visitVarInsn(Opcodes.ALOAD, `var`)
}

fun MethodVisitor.ASTORE(`var`: Int) {
    visitVarInsn(Opcodes.ASTORE, `var`)
}

fun MethodVisitor.GOTO(label: Label) {
    visitJumpInsn(Opcodes.GOTO, label)
}

inline fun <reified T> MethodVisitor.TRY_CATCH(
    noinline tryBlock: MethodVisitor.() -> Unit,
    noinline catchBlock: MethodVisitor.() -> Unit
) =
    TRY_CATCH(T::class.internalName, tryBlock, catchBlock)


fun MethodVisitor.TRY_CATCH(
    exceptionType: InternalName,
    tryBlock: MethodVisitor.() -> Unit,
    catchBlock: MethodVisitor.() -> Unit
) {
    val tryBlockStart = Label()
    val tryBlockEnd = Label()
    val catchBlockStart = Label()
    val catchBlockEnd = Label()
    visitTryCatchBlock(tryBlockStart, tryBlockEnd, catchBlockStart, exceptionType.value)

    visitLabel(tryBlockStart)
    tryBlock()
    GOTO(catchBlockEnd)
    visitLabel(tryBlockEnd)

    visitLabel(catchBlockStart)
    catchBlock()
    visitLabel(catchBlockEnd)
}

fun <T : Enum<T>> MethodVisitor.GETSTATIC(field: T) {
    val owner = field.declaringJavaClass.internalName
    GETSTATIC(owner, field.name, "L$owner;")
}

//
//fun MethodVisitor.GETSTATIC(field: KProperty<*>) {
//    val owner = (field.owner as kotlin.jvm..ClassBasedDeclarationContainer).jClass.internalName
//    GETSTATIC(owner, field.name, "L$owner;")
//}

fun MethodVisitor.GETSTATIC(owner: InternalName, name: String, desc: String) {
    visitFieldInsn(Opcodes.GETSTATIC, owner.value, name, desc)
}

fun MethodVisitor.GETFIELD(owner: InternalName, name: String, desc: String) {
    visitFieldInsn(Opcodes.GETFIELD, owner.value, name, desc)
}

fun MethodVisitor.PUTFIELD(owner: InternalName, name: String, desc: String) {
    visitFieldInsn(Opcodes.PUTFIELD, owner.value, name, desc)
}

fun MethodVisitor.CHECKCAST(type: KClass<*>) {
    CHECKCAST(type.internalName)
}

fun MethodVisitor.CHECKCAST(type: InternalName) {
    visitTypeInsn(Opcodes.CHECKCAST, type)
}

fun MethodVisitor.ACONST_NULL() {
    visitInsn(Opcodes.ACONST_NULL)
}

fun MethodVisitor.kotlinDeprecation(message: String) {
    visitAnnotation("Lkotlin/Deprecated;", true).apply {
        visit("message", message)
        visitEnd()
    }
}

/**
 * A JVM  type name (as in `java/lang/Object` instead of `java.lang.Object`).
 */
@JvmInline
value class InternalName(val value: String) {

    companion object {
        fun from(sourceName: String) = InternalName(sourceName.replace('.', '/'))
    }

    override fun toString() = value
}

object InternalNameOf {
    val javaLangObject = InternalName("java/lang/Object")
}

val KClass<*>.internalName: InternalName
    get() = java.internalName

inline val Class<*>.internalName: InternalName
    get() = InternalName(Type.getInternalName(this))