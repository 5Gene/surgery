
plugins {
    id("java-gradle-plugin")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
}

//repositories {
//    gradlePluginPortal()
//    google()
//}

project.ext {
    set("GROUP_ID", "osp.sparkj.plugin")
    set("ARTIFACT_ID", "surgery")
    set("VERSION", "2023.03.19")
}

//思路和booster一样 一个plugin一次文件复制，执行所有transform
//https://github.com/gradle/kotlin-dsl-samples
dependencies{
    ksp(wings.auto.service)
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation(libs.google.auto.service.anno)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation("osp.sparkj.plugin:surgery-api:2024.06.06")

//    api("osp.sparkj.plugin:surgery-api:2023.03.19")
//    implementation(project(":surgery-api"))

    compileOnly("com.android.tools.build:gradle-api:${libs.versions.android.gradle.plugin.get()}")
    compileOnly(gradleKotlinDsl())
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    compileOnly(kotlin("gradle-plugin-api", libs.versions.kotlin.get()))

}

//定义插件  就不需要 resources/META-INF/gradle-plugins/*.properties文件了
gradlePlugin {
    plugins {
        create("surgery") {
            id = "surgery"
            implementationClass = "osp.surgery.plugin.Hospital"
            displayName = "${id}.gradle.plugin"
            description = project.description ?: project.name
        }
    }
}

//https://github.com/tschuchortdev/kotlin-compile-testing
//https://bnorm.medium.com/exploring-kotlin-ir-bed8df167c23

//完成以下功能
//https://github.com/zhuguohui/MehodInterceptor
//bytex
//huntter
//booster
