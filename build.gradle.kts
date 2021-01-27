plugins {
    kotlin("jvm") version "1.4.21"
    maven
}

group = "org.valkyrienskies.core"
version = "1.0"

repositories {
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
}

val gdxVersion = "1.9.11"
val jacksonVersion = "2.11.1"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Bullet
    implementation("com.badlogicgames.gdx:gdx-bullet:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-bullet-platform:$gdxVersion:natives-desktop")

    // JOML
    api("org.joml:joml:1.10.0")
    api("org.joml:joml-primitives:1.10.0")

    // Apache Commons Math for Linear Programming
    implementation("org.apache.commons", "commons-math3", "3.6.1")

    // Guava
    implementation("com.google.guava:guava:29.0-jre")

    // Jackson Binary Dataformat
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformats-binary", jacksonVersion)
    implementation("org.junit.jupiter:junit-jupiter:5.4.2")

    // CQEngine
    implementation("com.googlecode.cqengine", "cqengine", "3.4.0") {
        exclude("sqlite-jdbc") //11.1 MB
        exclude("antlr4-runtime") //648.0 KB
        exclude("javassist") //1.5 MB
    }

    // Trove
    implementation("net.sf.trove4j", "trove4j", "3.0.3")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}