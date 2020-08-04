plugins {
    kotlin("jvm") version "1.3.72"
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
    implementation("com.badlogicgames.gdx:gdx-bullet:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-bullet-platform:$gdxVersion:natives-desktop")
    implementation("org.joml:joml:1.9.25")
    implementation("org.apache.commons", "commons-math3", "3.6.1")
    implementation("com.datumbox", "lpsolve", "5.5.2.0")
    implementation("com.google.guava:guava:29.0-jre")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformats-binary", jacksonVersion)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}