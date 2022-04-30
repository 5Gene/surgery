
val kotlin_version = "1.6.21"

plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

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
    ksp("dev.zacsweers.autoservice:auto-service-ksp:+")
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")
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


//打包源码
val sourcesJar by tasks.registering(Jar::class) {
    //如果没有配置main会报错
    from(sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

// Gradle task -> publishing -> publish*
publishing {
    //配置maven仓库
    repositories {
        maven {
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/Leaking/Hunter")
            credentials {
                username = "System.getenv('GITHUB_USER') ?: project.properties['GITHUB_USER']"
                password = "System.getenv('GITHUB_PERSONAL_ACCESS_TOKEN') ?: project.properties['GITHUB_PERSONAL_ACCESS_TOKEN']"
            }
        }
        maven {
            //name会成为任务名字的一部分 publishSurgeryPublicationTo [LocalTest] Repository
            name = "LocalTest"
            // change to point to your repo, e.g. http://my.org/repo
            url = uri("$rootDir/repo")
        }
    }
    publications {
        //name会成为任务名字的一部分 publish [Surgery] PublicationToLocalTestRepository
        create<MavenPublication>("surgery") {
            artifact(sourcesJar)
            artifact("$buildDir/libs/surgery.jar")
            groupId = "ospl"
            artifactId = "surgery"
            version = "1.0.0"
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
