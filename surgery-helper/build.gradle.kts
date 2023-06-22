plugins {
    id("org.jetbrains.kotlin.jvm")
}

project.ext {
    set("GROUP_ID", "osp.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-helper")
    set("VERSION", "2023.06.22")
}
apply("../publish-plugin.gradle")

dependencies {
    api("commons-io:commons-io:2.10.0")
    api("org.ow2.asm:asm:9.3")
    api("org.ow2.asm:asm-analysis:9.3")
    api("org.ow2.asm:asm-commons:9.3")
    api("org.ow2.asm:asm-tree:9.3")
    api("org.ow2.asm:asm-util:9.3")
}
