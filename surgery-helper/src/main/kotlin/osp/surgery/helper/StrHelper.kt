package osp.surgery.helper

/**
 * @author yun.
 * @date 2022/4/30
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */


fun String.log() {
    println("${Thread.currentThread().id} $ sparkj > $this")
}

@Deprecated("Use SurgeryConfig.isModuleJar() instead", ReplaceWith("SurgeryConfig.isModuleJar(this)"))
fun String.isModuleJar(): Boolean {
    return SurgeryConfig.isModuleJar(this)
}


/**
 * review的时候过滤
 */
@Deprecated("Use SurgeryConfig.shouldSkipClass() instead", ReplaceWith("SurgeryConfig.shouldSkipClass(this)"))
fun String.skipByFileName(): Boolean {
    return SurgeryConfig.shouldSkipClass(this)
}

fun String.className(): String {
    return substringBeforeLast(".").replace("/", ".")
}

fun String.isClass(): Boolean {
    return this.endsWith(".class")
}

fun String.isBuildConfigClass(): Boolean {
    return this == "BuildConfig.class"
}

fun String.isBindingClass(): Boolean {
    return this.endsWith("Binding.class")
}

fun String.isRClass(): Boolean {
    return startsWith("R$") || startsWith("R.")
}

fun String.toPackageName(): String {
    return substring(0, this.indexOf(".class")).replace("/|\\\\", ".")
}

fun String.isJar(): Boolean {
    return endsWith(".jar")
}

fun Any.sout() {
    println("${Thread.currentThread().id} $ sparkj > $this")
}

val String.lookDown: String
    get() = "👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇 $this 👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇"

val String.lookup: String
    get() = "👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆 $this 👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆"