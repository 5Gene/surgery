// Top-level build file where you can add configuration options common to all sub-projects/modules.

//buildscript {
//    dependencies {
//        classpath("osp.sparkj.plugin:surgery:2024.06.06")
//        classpath("osp.sparkj.plugin:surgery-doctors:2024.06.06")
//        classpath("osp.sparkj.plugin:surgery-doctor-arouter:2024.06.05")
//    }
//}

version = "2024.06.06"

plugins {
    alias(vcl.plugins.android.application) apply false
    alias(vcl.plugins.kotlin.android) apply false
    alias(vcl.plugins.kotlin.jvm) apply false
    alias(vcl.plugins.ksp) apply false
    alias(vcl.plugins.compose.compiler) apply false
    alias(vcl.plugins.gene.android) apply false
    alias(vcl.plugins.gene.compose) apply false
}
