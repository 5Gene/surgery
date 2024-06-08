plugins {
    kotlin("jvm")
}


dependencies {
    api("commons-io:commons-io:2.16.1")
    val asm = "9.7"
    api("org.ow2.asm:asm:$asm")
    api("org.ow2.asm:asm-analysis:$asm")
    api("org.ow2.asm:asm-commons:$asm")
    api("org.ow2.asm:asm-tree:$asm")
    api("org.ow2.asm:asm-util:$asm")
    implementation(files("libs/jd-core.jar"))
    // https://mvnrepository.com/artifact/org.benf/cfr
    implementation("org.benf:cfr:0.152")
    implementation("org.bitbucket.mstrobel:procyon-compilertools:0.6.0")
}
