plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    application
}

group = "com.duchastel.simon.brainiac.cli"

application {
    mainClass.set("com.duchastel.simon.brainiac.cli.MainKt")
    applicationDefaultJvmArgs = listOf("-Djansi.force=true")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-agent"))
    implementation(project(":tools"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okio)
    implementation(libs.slf4j.nop)
    implementation(libs.mosaic.runtime)
    implementation(libs.circuit.foundation)
}

tasks.withType<JavaExec> {
    standardInput = System.`in`
}
