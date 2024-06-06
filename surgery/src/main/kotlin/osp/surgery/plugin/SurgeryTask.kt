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
import osp.surgery.plugin.plan.ProjectSurgeryImpl
import osp.surgery.plugin.plan.SurgeryMeds
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.system.measureTimeMillis

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

    @Internal
    val jarPaths = mutableSetOf<String>()

    @get:Internal
    private val surgery = ProjectSurgeryImpl()

    @get:Internal
    abstract var tag: String

    @TaskAction
    fun taskAction() {
        println("$tag ===============================================================================")
        val nanoStartTime = System.nanoTime()
        surgery.surgeryPrepare()
        //xxxx/classes.jar 必须写入这个jar,后续只会处理这个jar
        val outputFile = output.get().asFile
        val allInputJars = allJars.get()
        val allInputDirs = allDirectories.get()
        val jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
        println("$tag > input:jars size: ${allInputJars.size}")
        println("$tag > input:dirs size: ${allInputDirs.size}")
        println("$tag > output:file: $outputFile")
        println("$tag --------------------------------------------------------------------------------")

        // we just copying classes fromjar files without modification
        val jarCost = measureTimeMillis {
            allInputJars.forEach { file ->
                reviewJarFile(file.asFile, jarOutput)
            }
        }
        println("$tag > jar handling cost:$jarCost")
        // Iterating through class files from directories
        // Looking for SomeSource.class to add generated interface and instrument with additional output in
        // toString methods (in our case it's just System.out)
        val dirCost = measureTimeMillis {
            allInputDirs.forEach { directory ->
                val cost = measureTimeMillis {
                    reviewDirectory(directory, jarOutput)
                }
                println("$tag > dir handling ${directory.asFile.absolutePath} cost:$cost")
            }
        }
        println("$tag > dir handling cost:$dirCost")

        surgery.surgeryOver()?.let { grandFinales ->
            grandFinales.forEach {
                jarOutput.writeByte(it.first, it.second)
            }
        }

        jarOutput.close()

        val cost = System.nanoTime() - nanoStartTime
        " # ${this.javaClass.simpleName} == cost:$cost > ${TimeUnit.NANOSECONDS.toSeconds(cost)}".sout()
        Dean.context.release()
        println("$tag ===============================================================================")
    }

    private fun reviewDirectory(directory: Directory, jarOutput: JarOutputStream) {
        directory.asFile.walk().forEach { file ->
            if (file.isFile) {
                val relativePath = directory.asFile.toURI().relativize(file.toURI()).getPath()
                val compileClassName = relativePath.replace(File.separatorChar, '/')
                println("$tag Adding from dir ${file.name}")
                println("$tag Adding from dir $relativePath")
                when (val surgeryMeds = surgery.surgeryOnClass(file.name, compileClassName, file.inputStream())) {
                    is SurgeryMeds.Byte -> jarOutput.writeByte(compileClassName, surgeryMeds.value)
                    is SurgeryMeds.Stream -> jarOutput.writeEntity(compileClassName, surgeryMeds.value)
                    null -> println("null")
                }
            }
        }
    }

    private fun reviewJarFile(file: File, jarOutput: JarOutputStream) {
        val jarFile = JarFile(file)
        val cost = measureTimeMillis {
            val surgeryOnJar = surgery.surgeryCheckJar(file)
            val action: ((Int, JarEntry) -> Unit) = if (surgeryOnJar) {
                { index: Int, jarEntry: JarEntry ->
                    val compileClassName = jarEntry.name
                    val fileName = compileClassName.substring(compileClassName.lastIndexOf('/') + 1)
                    println("$tag Adding from jar $fileName")
                    println("$tag Adding from jar $compileClassName")
                    val inputJarStream = jarFile.getInputStream(jarEntry)
                    when (val surgeryMeds = surgery.surgeryOnClass(fileName, compileClassName, inputJarStream)) {
                        is SurgeryMeds.Byte -> jarOutput.writeByte(compileClassName, surgeryMeds.value)
                        is SurgeryMeds.Stream -> jarOutput.writeEntity(compileClassName, surgeryMeds.value)
                        null -> println("null")
                    }
                }
            } else {
                { _, jarEntry ->
                    val compileClassName = jarEntry.name
                    val fileName = compileClassName.substring(compileClassName.lastIndexOf('/') + 1)
                    println("$tag Adding from jar $fileName")
                    println("$tag Adding from jar $compileClassName")
                    jarOutput.writeEntity(jarEntry.name, jarFile.getInputStream(jarEntry))
                }
            }
            jarFile.entries().asSequence().forEachIndexed { index, jarEntry ->
                if (jarEntry.isDirectory) {
                    return@forEachIndexed
                }
                action(index, jarEntry)
            }
        }
        println("$tag > jar handling $jarFile cost:$cost")
        jarFile.close()
    }


    // writeEntity methods check if the file has name that already exists in output jar
    private fun JarOutputStream.writeByte(name: String, entryByte: ByteArray) {
        // check for duplication name first
        if (jarPaths.contains(name)) {
            printDuplicatedMessage(name)
        } else {
            putNextEntry(JarEntry(name))
            write(entryByte)
            closeEntry()
            jarPaths.add(name)
        }
    }

    private fun JarOutputStream.writeEntity(name: String, inputStream: InputStream) {
        // check for duplication name first
        if (jarPaths.contains(name)) {
            printDuplicatedMessage(name)
        } else {
            putNextEntry(JarEntry(name))
            inputStream.copyTo(this)
            closeEntry()
            jarPaths.add(name)
        }
    }

    private fun printDuplicatedMessage(name: String) =
        println("Cannot add ${name}, because output Jar already has file with the same name.")
}