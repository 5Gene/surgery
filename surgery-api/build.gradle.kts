import june.wing.publishJavaMavenCentral

plugins {
    alias(vcl.plugins.kotlin.jvm)
}

buildscript {
    dependencies {
        classpath(vcl.gene.conventions)
    }
}

dependencies{
//    api("osp.sparkj.plugin:surgery-helper:2024.06.06")
}

publishJavaMavenCentral("surgery-api", true)