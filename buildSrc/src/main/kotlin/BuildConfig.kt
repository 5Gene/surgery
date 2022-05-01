import org.gradle.api.Project

object BuildConfig {
    const val compileSdkVersion = 31
    const val minSdkVersion = 22
    const val targetSdkVersion = 31
    const val versionCode = 10001
    const val versionName = "1.0.1"
}

const val kotlin_version = "1.6.21"
//This version (1.0.1) of the Compose Compiler requires Kotlin version 1.5.21
const val compose_version = "1.1.0"

const val libphonenumber = "com.googlecode.libphonenumber:libphonenumber:7.2.2"
//fun Project.configureAndroid() = this.extensions.getByType(com.android.build.gradle.BaseExtension::class.java).run{
//    this.compileSdkVersion(30)
//}