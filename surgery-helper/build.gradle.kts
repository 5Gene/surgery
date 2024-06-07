plugins {
    alias(libs.plugins.kotlin.jvm)
}


project.ext {
    set("GROUP_ID", "osp.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-helper")
    set("VERSION", "2024.06.06")
}

apply(from = "../publish-plugin.gradle")


dependencies {
    api("commons-io:commons-io:2.16.1")
    val asm = "9.7"
    api("org.ow2.asm:asm:$asm")
    api("org.ow2.asm:asm-analysis:$asm")
    api("org.ow2.asm:asm-commons:$asm")
    api("org.ow2.asm:asm-tree:$asm")
    api("org.ow2.asm:asm-util:$asm")
}
