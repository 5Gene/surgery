plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
}

project.ext {
    set("GROUP_ID", "osp.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-doctor-tryfinally")
    set("VERSION", rootProject.version.toString())
}

apply(from = "../publish-plugin.gradle")

dependencies{
    ksp(wings.auto.service)
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation(libs.google.auto.service.anno)
    implementation("osp.sparkj.plugin:surgery-api:2024.06.06")
}