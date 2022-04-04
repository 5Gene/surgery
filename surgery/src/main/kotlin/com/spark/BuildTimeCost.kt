package com.spark

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.*
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState

/**
 * @author yun.
 * @date 2021/7/20
 * @des [一句话描述]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
class BuildTimeCost : Plugin<Project> {


    val taskMap = mutableMapOf<String, TaskExecTimeInfo>()
    val taskList = mutableListOf<TaskExecTimeInfo>()

    override fun apply(project: Project) {
        //监听每个task的执行
        project.gradle.addListener(object : TaskExecutionListener {
            override fun beforeExecute(p0: Task) {
                println("*********** beforeExecute $p0 **************")
                val taskExecTimeInfo = TaskExecTimeInfo(name = p0.name, path = p0.path, start = System.currentTimeMillis())
                taskMap[p0.path] = taskExecTimeInfo
                taskList.add(taskExecTimeInfo)
            }

            override fun afterExecute(p0: Task, p1: TaskState) {
                println("*********** afterExecute $p0 **************")
                taskMap[p0.path]?.let {
                    it.end = System.currentTimeMillis()
                    it.total = it.end - it.start
                }
            }
        })
        project.gradle.addBuildListener(object : BuildListener {

            override fun beforeSettings(settings: Settings) {
                super.beforeSettings(settings)
            }

            override fun settingsEvaluated(p0: Settings) {
                println("*********** settingsEvaluated $p0 **************")
            }

            override fun projectsLoaded(p0: Gradle) {
                println("*********** projectsLoaded $p0 **************")
            }

            override fun projectsEvaluated(p0: Gradle) {
                println("*********** projectsEvaluated $p0 **************")
            }

            override fun buildFinished(p0: BuildResult) {
                println("*********** buildFinished $p0 **************")
//                taskList.onEach { println(it) }
                taskList.onEach(::println)
            }
        })

        project.gradle.addProjectEvaluationListener(object : ProjectEvaluationListener {
            override fun beforeEvaluate(p0: Project) {
                println("*********** beforeEvaluate ${p0.name} **************")
            }

            override fun afterEvaluate(p0: Project, p1: ProjectState) {
                println("*********** afterEvaluate ${p0.name} ${p1.executed} **************")
            }
        })
    }

}

//关于 task 的执行信息
data class TaskExecTimeInfo constructor(
    val name: String, var total: Long = 0L, val path: String = "", var start: Long = 0L, var
    end: Long = 0L
)