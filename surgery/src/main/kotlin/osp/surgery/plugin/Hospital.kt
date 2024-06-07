package osp.surgery.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import osp.surgery.helper.sout
import java.util.*

/**
 * @author yun.
 * @date 2021/7/20
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */

class Hospital : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType(AppPlugin::class.java) {

            // Queries for the extension set by the Android Application plugin.
            // This is the second of two entry points into the Android Gradle plugin
            val androidComponents =
                project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            // Registers a callback to be called, when a new variant is configured
            androidComponents.onVariants { variant ->
                val taskProvider = project.tasks.register<SurgeryTask2>("Surgery${variant.name}Classes") {
                    tag = "🔥"
                }
                // Register modify classes task
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(taskProvider)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        SurgeryTask2::allJars,
                        SurgeryTask2::allDirectories,
                        SurgeryTask2::output
                    )
            }
        }
        val taskReauests = project.gradle.startParameter.taskRequests
        " taskReauests : $taskReauests ".sout()
        if (taskReauests.size > 0) {
            val args = taskReauests[0].args.filter { !it.equals("clean") }
            "args : $args ".sout()
            if (args.isNotEmpty()) {
//                val predicate: (String) -> Boolean = { it.toLowerCase().contains("release") }
//                if (args.any(predicate)) {
//                }
//                val android = project.extensions.findByType<com.android.build.gradle.BaseExtension>()
//                val android = project.extensions.findByType(com.android.build.gradle.BaseExtension::class.java)
//                val android = project.extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
//                println(project.extensions.findByName("android"))
//                "project name: ${project.name}  $android  ${android?.transforms}".sout()
//                android?.registerTransform(Surgery(project))
            }
        }
    }
}