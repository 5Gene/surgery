// Top-level build file where you can add configuration options common to all sub-projects/modules.

//buildscript {
//    dependencies {
//        classpath("osp.sparkj.plugin:surgery:2023.03.19")
//        classpath("osp.sparkj.plugin:surgery-doctors:2023.03.19")
//    }
//}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(wings.plugins.android) apply false
}
