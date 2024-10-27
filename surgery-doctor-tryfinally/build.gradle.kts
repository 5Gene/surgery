plugins {
    alias(vcl.plugins.ksp)
    alias(vcl.plugins.kotlin.jvm)
}

dependencies{
    ksp(vcl.auto.service)
    // NOTE: It's important that you _don't_ use compileOnly here, as it will fail to resolve at compile-time otherwise
//    implementation(vcl.google.auto.service.anno)
    implementation("osp.sparkj.plugin:surgery-api:2024.06.06")
}