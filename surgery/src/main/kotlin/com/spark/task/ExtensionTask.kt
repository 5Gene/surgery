package com.spark.task

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.spark.extension.AppleArg
import com.spark.extension.Argument
import com.spark.extension.GreetingPluginExtension
import org.gradle.api.DefaultTask
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskAction

/**
 * @author yun.
 * @date 2021/7/19
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
open class ExtensionTask : DefaultTask() {

    @TaskAction
    fun output() {
        val greetingPluginExtension = project.extensions.getByName("greeting") as GreetingPluginExtension
        val argument = project.extensions.getByName("myArg") as Argument
        println("my ExtensionTask task output >> ${greetingPluginExtension.message.get()}")
        println("my ExtensionTask task output >> ${argument.message}")

        val argu = project.extensions.getByType(Argument::class.java)
        val appleArg = project.extensions.getByType(AppleArg::class.java)

        argu.buildTypes.onEach {
            println("my ExtensionTask task output >> buildTypes ${it.name}")
        }

        println("my ExtensionTask task output >> ${appleArg::class.java.name}")
        println("my ExtensionTask task output >> ${appleArg.phone}")
        println("my ExtensionTask task output >> $argu")
        if (argu is ExtensionAware) {
            //获取 嵌套的extension  使用第二种方式嵌套extension更方便
            val findByType = argu.extensions.findByType(AppleArg::class.java)
            println("my ExtensionTask task output inner >> $findByType")
        }

        //使用的时候最好 用findByType 不用类型转换了
        //不知道啥类型的时候 打印下
        val android = project.extensions.getByName("android")
        println("my ExtensionTask task output >> ${android::class.java.superclass.name}")
        println("my ExtensionTask task output >> ${android::class.java.name}")
        if (android is BaseAppModuleExtension) {
            println("my ExtensionTask task output >> android extension ===============")
            println("my ExtensionTask task output >> ${android.compileSdk}")
            println("my ExtensionTask task output >> ${android.buildToolsVersion}")
            println("my ExtensionTask task output >> $android")
            android.applicationVariants.onEach {
                println("my ExtensionTask task output >> Variants ${it.name} ${it.dirName} ${it.description} ${it.flavorName} " +
                        "${it.baseName}")
                it.outputs.onEach {
                    println("my ExtensionTask task output >> outputs ${it.outputFile}")
                }
                it.assembleProvider?.let {
                    val assembleTask = it.get()
                    println("my ExtensionTask task output >> assemble $assembleTask")
                }
            }
            println("my ExtensionTask task output >> android extension ===============")
        }
        println("my ExtensionTask task output >> buildDir ${project.buildDir}")
        println("my ExtensionTask task output >> buildFile ${project.buildFile}")
        println("my ExtensionTask task output >> rootDir ${project.rootDir}")
        println("my ExtensionTask task output >> projectDir ${project.projectDir}")
    }
}