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

fun String.isModuleJar(): Boolean {
    return this == "classes.jar"
}


/**
 * review的时候过滤
 */
fun String.skipByFileName(): Boolean {
    return !isClass() || isBindingClass() || isBuildConfigClass() || isRClass()
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
    return startsWith("R$")||startsWith("R.")
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