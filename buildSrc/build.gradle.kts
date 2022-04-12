import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

val kotlin_version = "1.6.20"

plugins {
    this.`kotlin-dsl`
    id("java-gradle-plugin")
    id("maven-publish")
//    id("kotlin-kapt")
//    kotlin("jvm") version "1.6.20"
}
apply(plugin = "kotlin-kapt")

repositories {
    google()
    mavenCentral()
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}


//思路和booster一样 一个plugin一次文件复制，执行所有transform
//https://github.com/gradle/kotlin-dsl-samples
dependencies{
//    kapt("com.google.auto.service:auto-service:1.0")
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:2.3.0")
//    implementation(localGroovy())
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:7.1.3")
    implementation("com.android.tools.build:gradle-api:7.1.3")
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-analysis:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")
    implementation("org.ow2.asm:asm-util:9.3")
    implementation("commons-io:commons-io:2.10.0")
    implementation(kotlin("gradle-plugin", kotlin_version))
    implementation(kotlin("gradle-plugin-api", kotlin_version))
}

//定义插件  就不需要 resources/META-INF/gradle-plugins/*.properties文件了
gradlePlugin {
    plugins {
        create("surgery") {
            id = "surgery"
            implementationClass = "sparkj.surgery.Hospital"
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
