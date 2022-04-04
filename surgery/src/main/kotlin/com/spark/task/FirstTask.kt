package com.spark.task

import com.spark.extension.Argument
import com.spark.extension.GreetingPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * @author yun.
 * @date 2021/7/19
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
abstract class FirstTask : DefaultTask() {

    @TaskAction
    fun output(){
        val greetingPluginExtension = project.extensions.getByName("greeting") as GreetingPluginExtension
        val argument = project.extensions.getByName("myArg") as Argument
        println("this is my custom task output >> ${greetingPluginExtension.message.get()}")
        println("my custom task output >> ${argument.message}")
    }
}