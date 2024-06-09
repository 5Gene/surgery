import com.andoter.asm_example.utils.ClassOutputUtil
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import trycatch.TryCatchClassVisitor

//https://www.cnblogs.com/beansoft/p/15495762.html
fun openWithJadx(classFilePath: String = """D:\code\dfj\surgery\asm_example\files\Change.class""") {
    // JADX GUI 的 JAR 文件路径
    val jadxGuiExePath = """D:\0buildCache\jadx-gui-1.5.0-with-jre-win\jadx-gui-1.5.0.exe"""
    ProcessBuilder(jadxGuiExePath, classFilePath).start()
}

fun main(vararg args: String) {
    val classReader = ClassReader("trycatch.TryCatchDemo")
    val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
    val classVisitor = TryCatchClassVisitor(classWriter)
    //ClassReader.SKIP_CODE导致代码没了
    //classReader.accept(classVisitor, ClassReader.SKIP_CODE)
    classReader.accept(classVisitor, ClassReader.SKIP_DEBUG or ClassReader.EXPAND_FRAMES)

//    println("==== 删除结果 ======")
//    val printClassVisitor = ClassPrintVisitor(Opcodes.ASM9)
//    val printReader = ClassReader(classWriter.toByteArray())
//    printReader.accept(printClassVisitor, ClassReader.SKIP_CODE)

    //输出文件查看
    openWithJadx(ClassOutputUtil.byte2File("asm_example/files/ASMReaderTemp.class", classWriter.toByteArray()))
}