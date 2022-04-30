plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

project.ext {
    set("GROUP_ID", "ospl.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-doctors")
    set("VERSION", "1.0.0-autoservice")
}
apply("../publish-plugin.gradle")

dependencies{
    ksp("dev.zacsweers.autoservice:auto-service-ksp:+")
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
    implementation("com.google.auto.service:auto-service-annotations:1.0.1")

    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-analysis:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")
    implementation("org.ow2.asm:asm-util:9.3")
    implementation("ospl.sparkj.plugin:surgery-api:1.0.1")
    implementation("ospl.sparkj.plugin:surgery-helper:1.0.1")
}