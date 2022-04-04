package com.spark.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * @author yun.
 * @date 2021/7/20
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
abstract class GreetingToFileTask : DefaultTask() {
    @get:OutputFile
    abstract var destination: File

    @TaskAction
    fun greet() {
        println("========= GreetingToFileTask =========")
        if (!destination.parentFile.exists()) {
            destination.parentFile.mkdirs()
        }
        if (!destination.exists()) {
            destination.createNewFile()
        }
        destination.writeText(" Hello Task >> GreetingToFileTask ")
    }
}