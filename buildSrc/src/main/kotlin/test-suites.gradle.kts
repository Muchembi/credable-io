import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    java
    `jvm-test-suite`
}

configurations.all {
    exclude(group = "junit", module = "junit")
}

dependencies {
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

testing {
    @Suppress("UnstableApiUsage")
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()

            sources {
                java {
                    setSrcDirs(listOf("src/test/unit-test/java"))
                }
            }
        }

        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter()

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }

            sources {
                java {
                    setSrcDirs(listOf("src/test/integration-test/java"))
                }
            }
        }

        configureEach {
            if (this is JvmTestSuite) {
                useJUnitJupiter()
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)

    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }

    reports {
        html.required.set(false)
        junitXml.required.set(false)
    }
}
