plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    api("org.slf4j:slf4j-api:2.0.16")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")

    implementation("org.yaml:snakeyaml:2.5")

    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}

tasks.jar {
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("PannLib ORM")
                description.set("Powerful YAML configuration system")
                url.set("https://github.com/itspanncake/PannLib")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("itspanncake")
                        name.set("Panncake")
                        email.set("panncake@europaws.eu")
                        timezone.set("Europe/Paris")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/itspanncake/PannLib.git")
                    developerConnection.set("scm:git:ssh://github.com:itspanncake/PannLib.git")
                    url.set("https://github.com/itspanncake/PannLib")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/itspanncake/PannLib")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
        mavenLocal()
    }
}

tasks.build {
    dependsOn("jar")
}

afterEvaluate {
    tasks.named("generateMetadataFileForMavenPublication") {
        dependsOn("jar")
    }
}