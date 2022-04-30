package sparkj.surgery

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.gradle.api.Project
import sparkj.surgery.more.*
import java.io.File
import org.apache.commons.io.FileUtils
import sparkj.surgery.plan.ProjectSurgeryImpl
import java.util.concurrent.TimeUnit
import ospl.surgery.helper.*

/**
 * @author yun.
 * @date 2021/7/20
 * @des [用户自定义的Transform，会比系统的Transform先执行]
 * @since [https://github.com/ZuYun]
 * <p><a href="https://github.com/ZuYun">github</a>
 */
class Surgery constructor(val project: Project) : Transform() {

    init {
        Dean.context.project = project
    }

    private val surgery = ProjectSurgeryImpl()
    //    用户自定义的Transform，会比系统的Transform先执行
    override fun getName() = "Surgery"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> = TransformManager.CONTENT_CLASS

    //    EXTERNAL_LIBRARIES：只有外部库
//    PROJECT：只有项目内容
//    PROJECT_LOCAL_DEPS：只有项目的本地依赖(本地jar)
//    PROVIDED_ONLY：只提供本地或远程依赖项
//    SUB_PROJECTS：只有子项目
//    SUB_PROJECTS_LOCAL_DEPS：只有子项目的本地依赖项(本地jar)
//    TESTED_CODE：由当前变量(包括依赖项)测试的代码
    override fun getScopes(): MutableSet<QualifiedContent.ScopeType> = when {
        surgery.classSurgeries.isEmpty() -> mutableSetOf()
        project.plugins.hasPlugin("com.android.library") -> TransformManager.PROJECT_ONLY
        project.plugins.hasPlugin("com.android.application") -> TransformManager.SCOPE_FULL_PROJECT
        project.plugins.hasPlugin("com.android.dynamic-feature") -> TransformManager.SCOPE_FULL_WITH_FEATURES
        else -> TODO("Not an Android project")
    }

    override fun isIncremental() = true

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        Dean.context.transformInvocation = transformInvocation
        (" # ${this.javaClass.simpleName} >>>>>> incremental:${transformInvocation.isIncremental} " +
                " variantName: ${transformInvocation.context.variantName} <<<<<<<< ").sout()
        ClassLoaderHelper.setClassLoader(transformInvocation.inputs,project)
        val nanoStartTime = System.nanoTime()
        surgery.surgeryPrepare()
        if (!transformInvocation.isIncremental) {
            //不是增量编译，则清空output目录
            transformInvocation.outputProvider.deleteAll()
        }
//        增量编译，则要检查每个文件的Status，Status分为四种，并且对四种文件的操作不尽相同
//        NOTCHANGED 当前文件不需要处理，甚至复制操作都不用
//        ADDED、CHANGED 正常处理，输出给下一个任务
//        REMOVED 移除outputProvider获取路径对应的文件
        transformInvocation.inputs.onEach {
            "=============== transformInvocation.inputs.onEach ==================== start".sout()
            reviewDirectory(it, transformInvocation)
            reviewJarFile(it, transformInvocation)
            "================ transformInvocation.inputs.onEach =================== end ".sout()
        }

        surgery.surgeryOver()
        val cost = System.nanoTime() - nanoStartTime
        " # ${this.javaClass.simpleName} == cost:$cost > ${TimeUnit.NANOSECONDS.toSeconds(cost)}".sout()
        Dean.context.release()
    }

    private fun reviewJarFile(
        it: TransformInput,
        transformInvocation: TransformInvocation
    ) {
        it.jarInputs.onEach { jar ->
            //                JarInput：它代表着以jar包方式参与项目编译的所有本地jar包或远程jar包，可以借助于它来实现动态添加jar包操作。
            //这里包括子模块打包的class文件debug\classes.jar
            //目标文件都被用数字重命名了，只有第一个transform的文件没被重命名
            val destJarFile = transformInvocation.outputProvider.getContentLocation(
                jar.name, jar.contentTypes, jar.scopes,
                Format.JAR
            )

            if (transformInvocation.isIncremental) {
                " # ${this.javaClass.simpleName} ***** surgeryOnJar incremental: ${jar.status} : ${jar.file.name}".sout()
                when (jar.status!!) {
                    Status.NOTCHANGED -> {
                        surgery.surgeryOnJar(jar.file, destJarFile, ospl.sparkj.surgery.api.Status.NOTCHANGED)
                    }
                    Status.ADDED, Status.CHANGED -> {
                        surgery.surgeryOnJar(jar.file, destJarFile, ospl.sparkj.surgery.api.Status.ADDED)
                    }
                    Status.REMOVED -> {
                        if (destJarFile.exists()) {
                            FileUtils.deleteQuietly(destJarFile)
                        }
                    }
                }
            } else {
                " # ${this.javaClass.simpleName} ***** surgeryOnJar: ${jar.file.name}".sout()
                surgery.surgeryOnJar(jar.file, destJarFile, ospl.sparkj.surgery.api.Status.ADDED)
            }
        }
    }

    private fun reviewDirectory(
        it: TransformInput,
        transformInvocation: TransformInvocation
    ) {
        //input 是循环
        //可能出现 direct没有jar多，direct很多jar没有
        it.directoryInputs.onEach { dir ->
            //DirectoryInput：它代表着以源码方式参与项目编译的所有目录结构及其目录下的源码文件，可以借助于它来修改输出文件的目录结构、目标字节码文件。
            val destDirectory = transformInvocation.outputProvider.getContentLocation(
                dir.name, dir.contentTypes, dir.scopes,
                Format.DIRECTORY
            )
            val srcDirectory = dir.file
            if (transformInvocation.isIncremental) {
                " # ${this.javaClass.simpleName} ***** surgeryOnDirectory incremental: ${srcDirectory.name}".sout()
                //https://juejin.cn/post/6916304559602139149
                //https://github.com/Leifzhang/AndroidAutoTrack
                dir.changedFiles.onEach { entry ->
                    " # ${this.javaClass.simpleName} ***** surgeryOnDirectory file: ${entry.value} : ${entry.key.name}".sout()
                    when (entry.value!!) {
                        Status.NOTCHANGED -> {
                            //主module下的未改变文件不会被遍历到这，子module的代码会被打包成 class.jar 不会走这里
                            surgery.surgeryOnFile(entry.key, srcDirectory, destDirectory, ospl.sparkj.surgery.api.Status.NOTCHANGED)
                        }
                        Status.ADDED, Status.CHANGED -> {
                            surgery.surgeryOnFile(entry.key, srcDirectory, destDirectory, ospl.sparkj.surgery.api.Status.ADDED)
                        }
                        Status.REMOVED -> {
                            val path = entry.key.absolutePath.replace(srcDirectory.absolutePath, destDirectory.absolutePath)
                            val destFile = File(path)
                            if (destFile.exists()) {
                                FileUtils.deleteQuietly(destFile)
                            }
                        }
                    }
                }
            } else {
                " # ${this.javaClass.simpleName} ***** surgeryOnDirectory: ${srcDirectory.name}".sout()
                srcDirectory.walk().filter { it.isFile }.forEach {
                    surgery.surgeryOnFile(it, srcDirectory, destDirectory, ospl.sparkj.surgery.api.Status.ADDED)
                }
            }
        }
    }

}