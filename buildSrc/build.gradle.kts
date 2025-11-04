plugins {
    alias(vcl.plugins.ksp)
    alias(vcl.plugins.kotlin.jvm)
}

val isCi = System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true" || System.getenv("JENKINS_HOME") != null
if (isCi) {//这里配置需要加载进buildSrc编译的代码方便调试
    sourceSets.main {
        java.srcDirs(
            """..\surgery-doctor-tryfinally\src\main\java""",
            """..\surgery-doctor-arouter\src\main\java"""
        )
    }
}

dependencies {
    ksp(vcl.gene.auto.service)
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation(vcl.google.auto.service.anno)

    implementation(vcl.kotlinx.coroutines.core)
    implementation(vcl.kotlinx.coroutines.core.jvm)
    implementation(libs.surgery.api)
    implementation(libs.surgery.helper)

    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle-api:${vcl.versions.android.gradle.plugin.get()}")
    compileOnly(gradleKotlinDsl())
    compileOnly(kotlin("gradle-plugin", vcl.versions.kotlin.asProvider().get()))
    compileOnly(kotlin("gradle-plugin-api", vcl.versions.kotlin.asProvider().get()))
}

//https://github.com/tschuchortdev/kotlin-compile-testing
//https://bnorm.medium.com/exploring-kotlin-ir-bed8df167c23
//https://github.com/Leifzhang/GradleSample.git

//完成以下功能
//https://github.com/zhuguohui/MehodInterceptor
//bytex
//huntter
//booster
