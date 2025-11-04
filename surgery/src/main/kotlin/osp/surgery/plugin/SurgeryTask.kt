package osp.surgery.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import osp.surgery.helper.sout
import osp.surgery.helper.SurgeryConfig
import osp.surgery.plugin.or.OperatingRoom
import osp.surgery.plugin.plan.ProjectSurgeryImpl
import osp.surgery.plugin.plan.SurgeryMeds
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

@Suppress("ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR")
abstract class SurgeryTask : DefaultTask() {

    // This property will be set to all Jar files available in scope
    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    // Gradle will set this property with all class directories that available in scope
    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    // Task will put all classes from directories and jars after optional modification into single jar
    @get:OutputFile
    abstract val output: RegularFileProperty

    // 使用并发集合，避免synchronized性能瓶颈
    @Internal
    val jarPaths = ConcurrentHashMap.newKeySet<String>()

    @get:Internal
    abstract var tag: String

    @get:Internal
    private val surgery = ProjectSurgeryImpl(::info)

    private fun info(msg: String) {
        logger.info("$tag surgery-> $msg")
        println("$tag surgery-> $msg")
    }

    @TaskAction
    fun taskAction() {
        println("$tag ===============================================================================")
        val nanoStartTime = System.nanoTime()
        surgery.surgeryPrepare()
        //xxxx/classes.jar 必须写入这个jar,后续只会处理这个jar
        val outputFile = output.get().asFile
        val allInputJars = allJars.get()
        val allInputDirs = allDirectories.get()
        val jarOutput = JarOutputStream(
            BufferedOutputStream(FileOutputStream(outputFile), SurgeryConfig.BUFFER_SIZE)
        )
        "$tag > input:jars size: ${allInputJars.size}".sout()
        "$tag > input:dirs size: ${allInputDirs.size}".sout()
        "$tag > output:file: $outputFile".sout()
        "$tag --------------------------------------------------------------------------------".sout()

        // we just copying classes fromjar files without modification
        allInputJars.forEach { file ->
            OperatingRoom.submit {
                reviewJarFile(file.asFile, jarOutput)
            }
        }

        allInputDirs.forEach { directory ->
            OperatingRoom.submit {
                reviewDirectory(directory, jarOutput)
            }
        }

        OperatingRoom.await()

        surgery.surgeryOver()?.let { grandFinales ->
            grandFinales.forEach {
                jarOutput.writeByte(it.first, it.second)
            }
        }

        jarOutput.close()

        val cost = System.nanoTime() - nanoStartTime
        val seconds = TimeUnit.NANOSECONDS.toSeconds(cost)
        " # ${this.javaClass.simpleName} == fileSize:${jarPaths.size} cost:$cost > ${seconds}s".sout()
        println("$tag ===============================================================================")
    }

    private fun reviewDirectory(directory: Directory, jarOutput: JarOutputStream) {
        val directoryFile = directory.asFile
        println("$tag reviewDirectory > ${directoryFile.absolutePath}")
        val directoryUri = directoryFile.toURI()
        directoryFile.walk().forEach { file ->
            if (file.isFile) {
                val relativePath = directoryUri.relativize(file.toURI()).path
                val compileClassName = relativePath.replace(File.separatorChar, '/')
                info("reviewClass from Dir: $relativePath")
                file.inputStream().use {
                    when (val surgeryMeds = surgery.surgeryOnClass(file.name, compileClassName, it)) {
                        is SurgeryMeds.Byte -> jarOutput.writeByte(compileClassName, surgeryMeds.value)
                        is SurgeryMeds.Stream -> jarOutput.writeEntity(compileClassName, surgeryMeds.value)
                        null -> {
                            // null表示需要最后处理，跳过
                        }
                    }
                }
            }
        }
    }

    private fun reviewJarFile(file: File, jarOutput: JarOutputStream) {
        val jarFile = JarFile(file)
        println("$tag reviewJarFile > ${file.absolutePath}")
        val surgeryOnJar = surgery.surgeryCheckJar(file)
        val action: ((Int, JarEntry) -> Unit) = if (surgeryOnJar) {
            { index: Int, jarEntry: JarEntry ->
                val compileClassName = jarEntry.name
                val fileName = compileClassName.substring(compileClassName.lastIndexOf('/') + 1)
                println("reviewClass from Jar: $compileClassName")
                jarFile.getInputStream(jarEntry).use {
                    when (val surgeryMeds = surgery.surgeryOnClass(fileName, compileClassName, it)) {
                        is SurgeryMeds.Byte -> jarOutput.writeByte(compileClassName, surgeryMeds.value)
                        is SurgeryMeds.Stream -> jarOutput.writeEntity(compileClassName, surgeryMeds.value)
                        null -> {
                            // null表示需要最后处理，跳过
                        }
                    }
                }
            }
        } else {
            { _, jarEntry ->
                val compileClassName = jarEntry.name
                logger.debug("ignore from jar:$compileClassName")
                jarFile.getInputStream(jarEntry).use {
                    jarOutput.writeEntity(compileClassName, it)
                }
            }
        }
        jarFile.entries().asSequence().forEachIndexed { index, jarEntry ->
            if (jarEntry.isDirectory) {
                return@forEachIndexed
            }
            action(index, jarEntry)
        }
        jarFile.close()
    }

    // writeEntity methods check if the file has name that already exists in output jar
    // 使用并发集合，移除@Synchronized，提高性能
    private fun JarOutputStream.writeByte(name: String, entryByte: ByteArray) {
        // check for duplication name first
        if (!jarPaths.add(name)) {
            printDuplicatedMessage(name)
            return
        }
        putNextEntry(JarEntry(name))
        write(entryByte)
        closeEntry()
    }

    private fun JarOutputStream.writeEntity(name: String, inputStream: InputStream) {
        // check for duplication name first
        if (!jarPaths.add(name)) {
            printDuplicatedMessage(name)
            return
        }
        putNextEntry(JarEntry(name))
        inputStream.copyTo(this)
        closeEntry()
    }

    private fun printDuplicatedMessage(name: String) =
        logger.debug("Cannot add ${name}, because output Jar already has file with the same name.")
}