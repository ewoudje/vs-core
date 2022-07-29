plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    java
    checkstyle
    `maven-publish`
}

group = "org.valkyrienskies.core"
// Determine the version
version = if (project.hasProperty("CustomReleaseVersion")) {
    project.property("CustomReleaseVersion") as String
} else {
    // Yes, I know there is a gradle plugin to detect git version.
    // But its made by Palantir 0_0.
    val gitRevision = "git rev-parse HEAD".execute()
    "1.0.0+" + gitRevision.substring(0, 10)
}

repositories {
    mavenCentral()
    maven {
        name = "VS Maven"
        url = uri(project.findProperty("vs_maven_url") ?: "https://maven.valkyrienskies.org/")

        val vsMavenUsername = project.findProperty("vs_maven_username") as String?
        val vsMavenPassword = project.findProperty("vs_maven_password") as String?

        if (vsMavenPassword != null && vsMavenUsername != null) {
            credentials {
                username = vsMavenUsername
                password = vsMavenPassword
            }
        }
    }
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    val jacksonVersion = "2.13.3"
    val nettyVersion = "4.1.25.Final"

    // VS Physics
    api("org.valkyrienskies:physics_api_krunch:1.0.0+3df5535a71")

    // JOML for Math
    api("org.joml:joml:1.10.0")
    api("org.joml:joml-primitives:1.10.0")

    // Apache Commons Math for Linear Programming
    implementation("org.apache.commons", "commons-math3", "3.6.1")

    // Guava
    implementation("com.google.guava:guava:29.0-jre")

    // Jackson Binary Dataformat for Object Serialization
    api("com.fasterxml.jackson.module", "jackson-module-kotlin", jacksonVersion)
    api("com.fasterxml.jackson.dataformat", "jackson-dataformat-cbor", jacksonVersion)
    api("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", jacksonVersion)
    api("com.fasterxml.jackson.datatype", "jackson-datatype-guava", jacksonVersion)

    api("com.networknt", "json-schema-validator", "1.0.71")
    api("com.github.imifou", "jsonschema-module-addon", "1.2.1")
    implementation("com.github.victools", "jsonschema-module-jackson", "4.25.0")
    implementation("com.github.victools", "jsonschema-generator", "4.25.0")
    implementation("com.flipkart.zjsonpatch", "zjsonpatch", "0.4.11")

    // FastUtil for Fast Primitive Collections
    implementation("it.unimi.dsi", "fastutil", "8.2.1")

    // Netty for networking
    implementation("io.netty", "netty-buffer", nettyVersion)

    // Junit 5 for Unit Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")

    // Log4j2 for Logging
    implementation("org.apache.logging.log4j:log4j-api:${properties["mc_log4j2_version"]}")
}

tasks.withType<Checkstyle> {
    reports {
        // Do not output html reports
        html.isEnabled = false
        // Output xml reports
        xml.isEnabled = true
    }
}

checkstyle {
    toolVersion = "8.41"
    configFile = file("$rootDir/.checkstyle/checkstyle.xml")
    isIgnoreFailures = true
}

ktlint {
    disabledRules.set(setOf("parameter-list-wrapping"))
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    compileTestJava {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

// Publish javadoc and sources to maven
java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        val ghpUser = (project.findProperty("gpr.user") ?: System.getenv("USERNAME")) as String?
        val ghpPassword = (project.findProperty("gpr.key") ?: System.getenv("TOKEN")) as String?
        // Publish to Github Packages
        if (ghpUser != null && ghpPassword != null) {
            println("Publishing to GitHub Packages")
            maven {
                name = "GithubPackages"
                url = uri("https://maven.pkg.github.com/ValkyrienSkies/vs-core")
                credentials {
                    username = ghpUser
                    password = ghpPassword
                }
            }
        }

        val vsMavenUsername = project.findProperty("vs_maven_username") as String?
        val vsMavenPassword = project.findProperty("vs_maven_password") as String?
        val vsMavenUrl = project.findProperty("vs_maven_url") as String?
        if (vsMavenUrl != null && vsMavenPassword != null && vsMavenUsername != null) {
            println("Publishing to VS Maven")
            maven {
                url = uri(vsMavenUrl)
                credentials {
                    username = vsMavenUsername
                    password = vsMavenPassword
                }
            }
        }
    }
    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "org.valkyrienskies.core"
                artifactId = "vs-core"
                version = project.version as String

                from(components["java"])
            }
        }
    }
}

// region Util functions

fun String.execute(envp: Array<String>? = null, dir: File = projectDir): String {
    val process = Runtime.getRuntime().exec(this, envp, projectDir)
    return process.inputStream.reader().readText()
}

// endregion
