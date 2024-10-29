import june.wing.GroupIdMavenCentral
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
    api(libs.surgery.helper)
}


group = GroupIdMavenCentral
version = "24.10.29"

publishJavaMavenCentral("surgery-api", true)