plugins {
    // IDE plugin
    idea
    eclipse

    java

    `maven-publish`

    // artifact signing - necessary on Maven Central
    signing

    id("com.intershop.gradle.javacc") version "5.0.1"
}

group = "com.intershop.icm"
description = "Platform - ISML Parser"
// apply gradle property 'projectVersion' to project.version, default to 'LOCAL'
val projectVersion : String? by project
version = projectVersion ?: "LOCAL"

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

repositories {
    mavenCentral()
    mavenLocal()
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// set correct project status
if (project.version.toString().endsWith("-SNAPSHOT")) {
    status = "snapshot"
}

javacc {
    configs {
        register("ismlParser") {
            inputFile = file("src/main/resources/com/intershop/beehive/isml/internal/parser/ISMLparser.jj")
            packageName = "com.intershop.beehive.isml.internal.parser"
            javaUnicodeEscape = "true"
            ignoreCase = "true"
        }
    }
}

testing {
    suites.withType<JvmTestSuite> {
        useJUnitJupiter()
        dependencies {
            implementation("org.junit.jupiter:junit-jupiter:5.10.1")
            implementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
            implementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")
        }

        targets {
            all {
                testTask.configure {
                    testLogging {
                        showStandardStreams = true
                    }
                }
            }
        }
    }
}

publishing {
    publications {
        create("intershopMvn", MavenPublication::class.java) {

            from(components["java"])

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/IntershopCommunicationsAG/${project.name}")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                organization {
                    name.set("Intershop Communications AG")
                    url.set("http://intershop.com")
                }
                developers {
                    developer {
                        id.set("m-raab")
                        name.set("M. Raab")
                        email.set("mraab@intershop.de")
                    }
                    developer {
                        id.set("Thomas-Bergmann")
                        name.set("T. Bergmann")
                        email.set("t.bergmann@intershop.de")
                    }
                    developer {
                        id.set("stefanh1")
                        name.set("S. Holzknecht")
                        email.set("s.holzknecht@intershop.de")
                    }
                }
                scm {
                    connection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    developerConnection.set("git@github.com:IntershopCommunicationsAG/${project.name}.git")
                    url.set("https://github.com/IntershopCommunicationsAG/${project.name}")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
}

signing {
    sign(publishing.publications["intershopMvn"])
}

dependencies {
    implementation("org.slf4j:log4j-over-slf4j:1.7.36")
}