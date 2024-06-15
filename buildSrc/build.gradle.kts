plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
}

//这里配置需要加载进buildSrc编译的代码方便调试
sourceSets.main {
    java.srcDirs(
//        """$rootDir\surgery-doctor-tryfinally\src\main\java""",
//        """$rootDir\surgery-doctor-arouter\src\main\java"""
    )
}

dependencies {
    ksp(wings.auto.service)
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation(libs.google.auto.service.anno)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.core.jvm)
    implementation("osp.sparkj.plugin:surgery-api:2024.06.06")

    compileOnly(gradleApi())
    compileOnly("com.android.tools.build:gradle-api:${libs.versions.android.gradle.plugin.get()}")
    compileOnly(gradleKotlinDsl())
    compileOnly(kotlin("gradle-plugin", libs.versions.kotlin.get()))
    compileOnly(kotlin("gradle-plugin-api", libs.versions.kotlin.get()))
}

//https://github.com/tschuchortdev/kotlin-compile-testing
//https://bnorm.medium.com/exploring-kotlin-ir-bed8df167c23
//https://github.com/Leifzhang/GradleSample.git

//完成以下功能
//https://github.com/zhuguohui/MehodInterceptor
//bytex
//huntter
//booster
