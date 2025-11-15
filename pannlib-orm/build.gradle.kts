plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    api("com.zaxxer:HikariCP:5.1.0")
    api("org.slf4j:slf4j-api:2.0.16")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")
    api("jakarta.persistence:jakarta.persistence-api:3.1.0")
    api("jakarta.validation:jakarta.validation-api:3.1.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}

tasks.jar {
    archiveClassifier.set("")
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
            artifact(tasks["sourcesJar"])
            pom {
                name.set("PannLib ORM")
                description.set("Lightweight, standalone ORM for Java – MySQL, PostgreSQL, SQLite bundled")
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