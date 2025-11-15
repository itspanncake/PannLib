plugins {
    id("java")
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.9.20"
}

allprojects {
    group = "fr.panncake.pannlib"
    version = "1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:unchecked"))
    }

    tasks.dokkaHtml {
        outputDirectory.set(file("$buildDir/dokka/html"))
    }
}