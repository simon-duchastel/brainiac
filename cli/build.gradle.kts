plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    application
}

group = "com.duchastel.simon.brainiac.cli"

application {
    mainClass.set("com.duchastel.simon.brainiac.cli.MainKt")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-agent"))
    implementation(project(":tools"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okio)
    implementation(libs.slf4j.simple)
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}
