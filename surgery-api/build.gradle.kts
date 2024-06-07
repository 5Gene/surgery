plugins {
    kotlin("jvm")
}

project.ext {
    set("GROUP_ID", "osp.sparkj.plugin")
    set("ARTIFACT_ID", "surgery-api")
    set("VERSION", "2024.06.06")
}

apply(from = "../publish-plugin.gradle")

dependencies{
    api("osp.sparkj.plugin:surgery-helper:2024.06.06")
}
