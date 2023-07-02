val kotlin_version = "1.8.10"

plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
    `maven-publish`
    `java-gradle-plugin`
    // Apply the Plugin Publish plugin to make plugin publication possible
    // The Plugin Publish plugin will in turn auto-apply the Gradle Plugin Development Plugin (java-gradle-plugin)
    // and the Maven Publish plugin (maven-publish)
    id("com.gradle.plugin-publish") version "1.2.0"
}

project.ext {
    set("GROUP_ID", "osp.sparkj.plugin")
    set("ARTIFACT_ID", "surgery")
    set("VERSION", "2023.06.17")
}

//apply("../publish.gradle.kts")
apply("../publish-plugin.gradle")

//思路和booster一样 一个plugin一次文件复制，执行所有transform
//https://github.com/gradle/kotlin-dsl-samples
dependencies{
    ksp("dev.zacsweers.autoservice:auto-service-ksp:+")
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")

    api("osp.sparkj.plugin:surgery-api:2023.06.22")

    compileOnly("com.android.tools.build:gradle:7.4.2")
    compileOnly("com.android.tools.build:gradle-api:7.4.2")
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin", kotlin_version))
    compileOnly(kotlin("gradle-plugin-api", kotlin_version))
}


//<editor-fold desc="for plugin-publish">
version = project.ext["VERSION"]!!
group = project.ext["GROUP_ID"]!!
//</editor-fold>

//https://plugins.gradle.org/docs/publish-plugin
//定义插件  就不需要 resources/META-INF/gradle-plugins/*.properties文件了
gradlePlugin {
//    0.18.0 版本 plugin-publish 不需要 website 对应gradle 7.4.2
//  1.0.0版本 plugin-publish开始 必须要website 必须对应gradle 7.4.6+版本
    website.set("https://github.com/5hmlA/surgery")
    vcsUrl.set("https://github.com/5hmlA/surgery")

    plugins {
        create("surgery") {
            id = "osp.sparkj.surgery"
            implementationClass = "osp.surgery.plugin.Hospital"
            displayName = "${id}.gradle.plugin"
            description = project.description ?: project.name
            tags.set(listOf("asm", "transform", "spark", "plugins"))
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
