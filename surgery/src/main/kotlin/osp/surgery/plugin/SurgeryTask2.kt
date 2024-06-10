package osp.surgery.plugin

import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import osp.surgery.plugin.or.OperatingRoom
import osp.surgery.plugin.plan.ProjectSurgeryImpl
import osp.surgery.plugin.plan.SurgeryMeds
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import kotlin.system.measureTimeMillis

abstract class SurgeryTask2 : DefaultTask() {

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

    inner class Write(val surgeryMeds: SurgeryMeds? = null, val jarFile: JarFile? = null)

    @TaskAction
    fun taskAction() {
        runBlocking {
            println("$tag ===============================================================================")
            val nanoStartTime = System.nanoTime()
            surgery.surgeryPrepare()

            //xxxx/classes.jar 必须写入这个jar,后续只会处理这个jar
            val outputFile = output.get().asFile
            val allInputJars = allJars.get()
            val allInputDirs = allDirectories.get()
            val jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))

            "$tag > input:jars size: ${allInputJars.size}".sout()
            "$tag > input:dirs size: ${allInputDirs.size}".sout()
            "$tag > output:file: $outputFile".sout()
            "$tag --------------------------------------------------------------------------------".sout()

            val outputChannel = Channel<Write>()
            val outputJob = launch {
                println("$tag 🍔--- outputChannel ready -----------------------------------------------------------------------------")
                for (write in outputChannel) {
                    write.surgeryMeds?.let { surgeryMeds ->
                        val compileClassName = surgeryMeds.compileClassName
                        println("$tag 🍔--- outputChannel write ---> $compileClassName")
                        when (surgeryMeds) {
                            is SurgeryMeds.Byte -> jarOutput.writeByte(compileClassName, surgeryMeds.value)
                            is SurgeryMeds.Stream -> jarOutput.writeEntity(compileClassName, surgeryMeds.value)
                        }
                    }
                    write.jarFile?.let {
                        it.close()
                    }

                }
            }

            val transformDispatcher = Executors.newFixedThreadPool(OperatingRoom.cpuCount).asCoroutineDispatcher()

            val transformJobs = mutableListOf<Job>()

            // we just copying classes fromjar files without modification
            val jarCost = measureTimeMillis {
                allInputJars.forEach { file ->
                    transformJobs.add(launch(transformDispatcher) {
                        reviewJarFile(file.asFile, outputChannel)
                    })
                }
            }
            "$tag > jar handling cost:${jarCost}ms".sout()
            // Iterating through class files from directories
            // Looking for SomeSource.class to add generated interface and instrument with additional output in
            // toString methods (in our case it's just System.out)
            val dirCost = measureTimeMillis {
                allInputDirs.forEach { directory ->
                    transformJobs.add(launch {
                        val cost = measureTimeMillis {
                            reviewDirectory(directory, outputChannel)
                        }
                        "$tag > dir handling ${directory.asFile.absolutePath} cost:${cost}ms".sout()
                    })
                }
            }
            "$tag > dir handling cost:${dirCost}ms".sout()

            // Wait for all producers to finish
            transformJobs.joinAll()

            //最后处理最后要处理的
            surgery.surgeryOver()?.let { grandFinales ->
                grandFinales.forEach {
                    outputChannel.send(Write(SurgeryMeds.Byte(it.first, it.second)))
                }
            }

            // Signal the consumer to finish
            outputChannel.close()

            // Wait for the consumer to finish
            outputJob.join()

            jarOutput.close()

            val cost = System.nanoTime() - nanoStartTime
            " # ${this@SurgeryTask2.javaClass.simpleName} == fileSize:${jarPaths.size} == cost:$cost > ${TimeUnit.NANOSECONDS.toSeconds(cost)}s".sout()
            println("$tag ===============================================================================")
        }
    }

    private suspend fun reviewDirectory(directory: Directory, outputChannel: Channel<Write>) {
        directory.asFile.walk().forEach { file ->
            if (file.isFile) {
                val relativePath = directory.asFile.toURI().relativize(file.toURI()).getPath()
                val compileClassName = relativePath.replace(File.separatorChar, '/')
//                println("$tag Adding from dir ${file.name}")
                println("$tag Adding from dir $relativePath")
                surgery.surgeryOnClass(file.name, compileClassName, file.inputStream())?.let {
                    outputChannel.send(Write(it))
                }
            }
        }
    }

    private suspend fun reviewJarFile(file: File, outputChannel: Channel<Write>) {
        val jarFile = JarFile(file)
        val cost = measureTimeMillis {
            val surgeryOnJar = surgery.surgeryCheckJar(file)
            val action: suspend ((Int, JarEntry) -> Unit) = if (surgeryOnJar) {
                { index: Int, jarEntry: JarEntry ->
                    val compileClassName = jarEntry.name
                    val fileName = compileClassName.substring(compileClassName.lastIndexOf('/') + 1)
//                    println("$tag Adding from jar $fileName")
                    println("$tag Adding from jar $compileClassName")
                    val inputJarStream = jarFile.getInputStream(jarEntry)
                    surgery.surgeryOnClass(fileName, compileClassName, inputJarStream)?.let {
                        outputChannel.send(Write(it))
//                        if (it is SurgeryMeds.Stream) {
//                            //trySend的问题是不会等待,如果Channel为BufferOverflow.SUSPEND当处理耗时的时候channel.send()方法会'等待'
//                            //channel消费方是耗时的,而channel生产方可以当作不耗时trySend不会等待消费方处理完在发送
//                            //会导致所有transform执行完后 就把channel给close了导致处理保存的时候提前结束
//                            //trySend 方法尝试向通道发送一个元素，如果通道完全满了（在限制的通道中）或者通道被关闭了，这个方法就会返回 false
//                            outputChannel.send(SurgeryMeds.Byte(it.compileClassName, it.value.readBytes()))
//                        } else {
//                            outputChannel.send(it)
//                        }
                    }
                }
            } else {
                { _, jarEntry ->
                    val compileClassName = jarEntry.name
                    println("$tag Adding from jar $compileClassName")
                    outputChannel.send(Write(SurgeryMeds.Stream(jarEntry.name, jarFile.getInputStream(jarEntry))))
                }
            }
            jarFile.entries().asSequence().forEachIndexed { index, jarEntry ->
                if (jarEntry.isDirectory) {
                    return@forEachIndexed
                }
                action(index, jarEntry)
            }
        }
        "$tag > jar handling ${jarFile.name} cost:${cost}ms".sout()
        outputChannel.send(Write(jarFile = jarFile))
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