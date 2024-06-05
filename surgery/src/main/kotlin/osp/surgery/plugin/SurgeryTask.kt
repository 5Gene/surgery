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
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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
    abstract var tag: String

    @TaskAction
    fun taskAction() {
        println("$tag ===============================================================================")
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
                val inputFile = file.asFile
                val jarFile = JarFile(inputFile)
                val cost = measureTimeMillis {
                    val entries = jarFile.entries()
                    entries.asSequence().forEachIndexed { index, jarEntry ->
//                    entries.iterator().forEach { jarEntry ->
//                    println("$tag Adding from jar ${jarEntry.name}")
                        jarOutput.writeEntity(jarEntry.name, jarFile.getInputStream(jarEntry))
                    }
                }
                println("$tag > jar handling $inputFile cost:$cost")
                jarFile.close()
            }
        }
        println("$tag > jar handling cost:$jarCost")
        // Iterating through class files from directories
        // Looking for SomeSource.class to add generated interface and instrument with additional output in
        // toString methods (in our case it's just System.out)
        val dirCost = measureTimeMillis {
            allInputDirs.forEach { directory ->
                val cost = measureTimeMillis {
                    directory.asFile.walk().forEach { file ->
                        if (file.isFile) {
                            if (file.name.endsWith("SomeSource.class")) {
                                println("Found $file.name")
                                val relativePath = directory.asFile.toURI().relativize(file.toURI()).getPath()
                                jarOutput.writeEntity(relativePath.replace(File.separatorChar, '/'), file.inputStream())
                            } else {
                                // if class is not SomeSource.class - just copy it to output without modification
                                val relativePath = directory.asFile.toURI().relativize(file.toURI()).getPath()
//                            println("$tag Adding from directory ${relativePath.replace(File.separatorChar, '/')}")
                                jarOutput.writeEntity(relativePath.replace(File.separatorChar, '/'), file.inputStream())
                            }
                        }
                    }
                }
                println("$tag > dir handling ${directory.asFile.absolutePath} cost:$cost")
            }
        }
        println("$tag > dir handling cost:$dirCost")

        jarOutput.close()
        println("$tag ===============================================================================")
    }


    // writeEntity methods check if the file has name that already exists in output jar
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

    private fun JarOutputStream.writeEntity(relativePath: String, byteArray: ByteArray) {
        // check for duplication name first
        if (jarPaths.contains(relativePath)) {
            printDuplicatedMessage(relativePath)
        } else {
            putNextEntry(JarEntry(relativePath))
            write(byteArray)
            closeEntry()
            jarPaths.add(relativePath)
        }
    }

    private fun printDuplicatedMessage(name: String) =
        println("Cannot add ${name}, because output Jar already has file with the same name.")
}