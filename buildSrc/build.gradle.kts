repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/ZuYun/sparkj")
        credentials {
            username = "ZuYun"
            password = "\u0067\u0068\u0070\u005f\u0031\u0063\u0062\u0064\u004d\u004a\u0073\u005a\u0042\u0057\u0033\u006a\u0077\u0057\u0053\u004b\u006e\u0066\u0037\u0042\u0053\u0069\u0044\u006d\u0061\u0030\u0066\u0044\u0048\u0076\u0031\u007a\u0059\u0050\u0073\u0044"
        }
    }
}

plugins {
    this.`kotlin-dsl`
//    kotlin("jvm")
//    id("com.google.devtools.ksp")
}


dependencies{
//    ksp("dev.zacsweers.autoservice:auto-service-ksp:+")
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")

    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-analysis:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")
    implementation("org.ow2.asm:asm-util:9.3")
    implementation("ospl.sparkj.plugin:surgery-api:1.0.3")
    implementation("ospl.sparkj.plugin:surgery-helper:1.0.3")
    implementation("commons-io:commons-io:2.10.0")
}

//https://github.com/tschuchortdev/kotlin-compile-testing
//https://bnorm.medium.com/exploring-kotlin-ir-bed8df167c23

//完成以下功能
//https://github.com/zhuguohui/MehodInterceptor
//bytex
//huntter
//booster
