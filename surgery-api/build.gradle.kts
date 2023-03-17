plugins {
    id("org.jetbrains.kotlin.jvm")
}

project.ext {
    set("GROUP_ID", "ospl.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-api")
    set("VERSION", "1.0.5")
}
apply("../publish-plugin.gradle")

dependencies{
    implementation("org.ow2.asm:asm:9.3")
    implementation("org.ow2.asm:asm-analysis:9.3")
    implementation("org.ow2.asm:asm-commons:9.3")
    implementation("org.ow2.asm:asm-tree:9.3")
    implementation("org.ow2.asm:asm-util:9.3")
    implementation("ospl.sparkj.plugin:surgery-helper:1.0.5")
}