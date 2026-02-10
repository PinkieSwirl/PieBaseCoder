plugins {
    kotlin("jvm")
    id("dev.detekt")
    id("com.autonomousapps.dependency-analysis")
    jacoco
}

// main
val kotlinVersion: String by project
// test
val junitVersion: String by project
val jqwikVersion: String by project
// quality
val jacocoVersion: String by project
val detektVersion: String by project

group = "eu.pieland"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins("dev.detekt:detekt-rules-libraries:$detektVersion")

    testImplementation(kotlin("test:$kotlinVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("net.jqwik:jqwik-api:$jqwikVersion")
    testImplementation("net.jqwik:jqwik-kotlin:$jqwikVersion")
}

tasks.test {
    minHeapSize = "8g"
    maxHeapSize = "16g"
    useJUnitPlatform {
        includeEngines("junit-jupiter", "jqwik")
        excludeTags("slow")
    }
}

sourceSets.main {
    java.srcDirs("src")
}

sourceSets.test {
    java.srcDirs("test")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report

    reports {
        xml.required = true
        html.required = false
    }
}

jacoco {
    toolVersion = jacocoVersion
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xnullability-annotations=@org.jspecify.annotations:strict",
            "-Xemit-jvm-type-annotations",
            "-Xcontext-sensitive-resolution",
            "-Xdata-flow-based-exhaustiveness",
            "-Xcontext-parameters",
            "-Xreturn-value-checker=full",
        )
        progressiveMode = true
        javaParameters = true
    }
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
}

dependencyAnalysis {
    usage {
        analysis {
            checkSuperClasses(true) // false by default
        }
    }
}
