
val kotlin_version = "1.7.10"

plugins {
    id("java-gradle-plugin")
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

project.ext {
    set("GROUP_ID", "ospl.sparkj.plugin")
    set("ARTIFACT_ID", "surgery")
    set("VERSION", "1.0.5")
}
//apply {
//    from("../publish.gradle.kts")
//}
//apply("../publish.gradle.kts")
apply("../publish-plugin.gradle")

//思路和booster一样 一个plugin一次文件复制，执行所有transform
//https://github.com/gradle/kotlin-dsl-samples
dependencies{
    ksp("dev.zacsweers.autoservice:auto-service-ksp:+")
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")

    api("commons-io:commons-io:2.10.0")
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")

    compileOnly("com.android.tools.build:gradle:7.2.2")
    compileOnly("com.android.tools.build:gradle-api:7.2.2")
    compileOnly(gradleApi())
    compileOnly(kotlin("gradle-plugin", kotlin_version))
    compileOnly(kotlin("gradle-plugin-api", kotlin_version))
}

//定义插件  就不需要 resources/META-INF/gradle-plugins/*.properties文件了
gradlePlugin {
    plugins {
        create("surgery") {
            id = "surgery"
            implementationClass = "ospl.surgery.plugin.Hospital"
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
