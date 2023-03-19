package osp.surgery.plugin.wings

import com.android.build.api.transform.TransformInput
import com.android.build.gradle.AppExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.com.google.common.collect.ImmutableList
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader

/**
 * Created by quinn on 31/08/2018
 */
object ClassLoaderHelper {
    @Throws(MalformedURLException::class)
    fun setClassLoader(
        inputs: Collection<TransformInput>,
        project: Project
    ) {
        val urls = ImmutableList.Builder<URL>()
        val androidJarPath = getAndroidJarPath(project)
        val file = File(androidJarPath)
        val androidJarURL = file.toURI().toURL()
        urls.add(androidJarURL)
        for (totalInputs in inputs) {
            for (directoryInput in totalInputs.directoryInputs) {
                if (directoryInput.file.isDirectory) {
                    urls.add(directoryInput.file.toURI().toURL())
                }
            }
            for (jarInput in totalInputs.jarInputs) {
                if (jarInput.file.isFile) {
                    urls.add(jarInput.file.toURI().toURL())
                }
            }
        }
        val allUrls = urls.build()
        val classLoaderUrls = allUrls.toTypedArray()
        ExtendClassWriter.urlClassLoader = URLClassLoader(classLoaderUrls)
    }

    /**
     * /Users/quinn/Documents/Android/SDK/platforms/android-28/android.jar
     */
    private fun getAndroidJarPath(project: Project): String {
        val appExtension = project.properties["android"] as AppExtension?
        var sdkDirectory = appExtension!!.sdkDirectory.absolutePath
        val compileSdkVersion = appExtension.compileSdkVersion!!
        sdkDirectory = sdkDirectory + File.separator + "platforms" + File.separator
        return sdkDirectory + compileSdkVersion + File.separator + "android.jar"
    }
}