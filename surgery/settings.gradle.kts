pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("io.github.5hmlA.vcl") version "25.04.06"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/5hmlA/sparkj")
            credentials {
                username = """\u005a\u0075\u0059\u0075\u006e"""
                password =
                    """\u0067\u0068\u0070\u005f\u004f\u0043\u0042\u0045\u007a\u006a\u0052\u0069\u006e\u0043\u0065\u0048\u004c\u0068\u006b\u0052\u0036\u0056\u0061\u0041\u0074\u0068\u004f\u004a\u0059\u0042\u0047\u0044\u0073\u0049\u0032\u0070\u0064\u0064\u0069\u0066"""
            }
        }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "surgery"


//https://developer.android.google.cn/build/publish-library/upload-library?hl=zh-cn#kts