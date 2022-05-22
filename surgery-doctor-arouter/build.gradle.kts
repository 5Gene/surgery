plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

project.ext {
    set("GROUP_ID", "ospl.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-doctor-arouter")
    set("VERSION", "1.0")
}
apply("../publish-plugin.gradle")

dependencies{
    ksp("dev.zacsweers.autoservice:auto-service-ksp:+")
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    api("com.google.auto.service:auto-service-annotations:1.0.1")
    api("org.ow2.asm:asm:9.3")
    api("org.ow2.asm:asm-analysis:9.3")
    api("org.ow2.asm:asm-commons:9.3")
    api("org.ow2.asm:asm-tree:9.3")
    api("org.ow2.asm:asm-util:9.3")
    api("ospl.sparkj.plugin:surgery-api:1.0.4")
    api("ospl.sparkj.plugin:surgery-helper:1.0.3")
}