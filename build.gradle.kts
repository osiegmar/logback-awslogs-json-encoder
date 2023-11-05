plugins {
    `java-library`
    `maven-publish`
    signing
    checkstyle
    id("com.github.spotbugs") version "5.1.4"
}

group = "de.siegmar"
version = "2.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("ch.qos.logback:logback-classic:1.4.11")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.11.1")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.38.0")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    excludeFilter = file("${project.rootDir}/config/spotbugs/config.xml")
    reports.maybeCreate("xml").required = false
    reports.maybeCreate("html").required = true
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "logback-awslogs-json-encoder"
            from(components["java"])

            pom {
                name = "Logback awslogs JSON encoder"
                description = "Logback encoder for producing JSON formatted log output for Amazon CloudWatch Logs Insights."
                url = "https://github.com/osiegmar/logback-awslogs-json-encoder"
                licenses {
                    license {
                        name = "GNU Lesser General Public License version 2.1"
                        url = "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt"
                    }
                }
                scm {
                    url = "https://github.com/osiegmar/logback-awslogs-json-encoder"
                    connection = "scm:git:https://github.com/osiegmar/logback-awslogs-json-encoder.git"
                }
                developers {
                    developer {
                        id = "osiegmar"
                        name = "Oliver Siegmar"
                        email = "oliver@siegmar.de"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "ossrh"
            credentials(PasswordCredentials::class)
            url = if (version.toString().endsWith("SNAPSHOT")) {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            } else {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}
