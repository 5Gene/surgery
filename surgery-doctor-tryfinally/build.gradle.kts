plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

project.ext {
    set("GROUP_ID", "osp.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-doctor-tryfinally")
    set("VERSION", "2023.06.22")
}
apply("../publish-plugin.gradle")

dependencies{
    ksp("dev.zacsweers.autoservice:auto-service-ksp:+")
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")

    api("osp.sparkj.plugin:surgery-api:2023.06.22")
}