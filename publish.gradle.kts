val kotlin_v: String by extra

//Publish_gradle  为什么不是 Build_gradle

println("===========================$this =========")
kotlin.RuntimeException("test").printStackTrace()
println("===========================${this::class.java.superclass} =========")
println("===========================${this::class} ==========")


//plugins {
//    id("maven-publish")
//}
//
//
////打包源码
//val sourcesJar by tasks.registering(Jar::class) {
//    //如果没有配置main会报错
//    from(sourceSets["main"].allSource)
//    archiveClassifier.set("sources")
//}
////
//// Gradle task -> publishing -> publish*
//publishing {
//    //配置maven仓库
//    repositories {
//        maven {
//            name = "GithubPackages"
//            url = uri("https://maven.pkg.github.com/5hmlA/sparkj")
//            credentials {
//                username = System.getenv("GITHUB_USER")
//                password = System.getenv("GITHUB_TOKEN")
//            }
//        }
//        maven {
//            //name会成为任务名字的一部分 publishSurgeryPublicationTo [LocalTest] Repository
//            name = "LocalTest"
//            // change to point to your repo, e.g. http://my.org/repo
//            url = uri("$rootDir/repo")
//        }
//    }
//    publications {
//        //name会成为任务名字的一部分 publish [Surgery] PublicationToLocalTestRepository
//        create<MavenPublication>("surgery") {
//            artifact(sourcesJar)
//            artifact("$buildDir/libs/surgery.jar")
//            groupId = "sparkj.ospl"
//            artifactId = "surgery"
//            version = "1.0.0"
//        }
//
//    }
//}
