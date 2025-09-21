dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:fs"))
    implementation(project(":core:llm"))
    implementation(project(":core:search"))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.mockk)
}