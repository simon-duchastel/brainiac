plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.kotest)
}

kotlin {
    jvm()

    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core-process-search-api"))
                implementation(project(":core-fileaccess-impl"))
                implementation(libs.okio)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotest.framework.engine)
                implementation(libs.kotlinx.datetime)
                implementation(libs.okio.fakefilesystem)
                implementation(libs.kotest.assertions.core)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    filter {
        isFailOnNoMatchingTests = false
    }
}
