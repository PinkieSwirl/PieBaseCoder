plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
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
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:$detektVersion")

    testImplementation(kotlin("test:$kotlinVersion"))

    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")

    testImplementation("net.jqwik:jqwik-api:${jqwikVersion}")
    testImplementation("net.jqwik:jqwik-kotlin:${jqwikVersion}")
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
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xnullability-annotations=@org.jspecify.annotations:strict",
            "-Xemit-jvm-type-annotations", // Enable annotations on type variables
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
