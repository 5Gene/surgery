plugins {
    id("org.jetbrains.kotlin.jvm")
}

project.ext {
    set("GROUP_ID", "osp.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-helper")
    set("VERSION", "2023.06.22")
}

dependencies {
    api("commons-io:commons-io:2.15.1")
    api("org.ow2.asm:asm:9.6")
    api("org.ow2.asm:asm-analysis:9.6")
    api("org.ow2.asm:asm-commons:9.6")
    api("org.ow2.asm:asm-tree:9.6")
    api("org.ow2.asm:asm-util:9.6")
}
