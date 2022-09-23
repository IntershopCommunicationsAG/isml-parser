plugins {
    // IDE plugin
    idea
    eclipse

    java

    `maven-publish`

    // artifact signing - necessary on Maven Central
    signing

    id("com.intershop.gradle.javacc") version "4.0.1"

    // intershop version plugin
    id("com.intershop.gradle.scmversion") version "6.2.0"
}

scm {
    version.initialVersion = "11.0.0"
}

group = "com.intershop.icm"
description = "Platform - ISML Parser"
version = scm.version.version

val sonatypeUsername: String? by project
val sonatypePassword: String? by project

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

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    withJavadocJar()
    withSourcesJar()
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
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

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:log4j-over-slf4j:1.7.36")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}