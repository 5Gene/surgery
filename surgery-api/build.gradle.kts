plugins {
    kotlin("jvm")
}

project.ext {
    set("GROUP_ID", "osp.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-api")
    set("VERSION", "2023.06.22")
}

dependencies{
//    implementation("org.ow2.asm:asm:9.3")
//    implementation("org.ow2.asm:asm-analysis:9.3")
//    implementation("org.ow2.asm:asm-commons:9.3")
//    implementation("org.ow2.asm:asm-tree:9.3")
//    implementation("org.ow2.asm:asm-util:9.3")
    api("osp.sparkj.plugin:surgery-helper:2023.06.22")
}