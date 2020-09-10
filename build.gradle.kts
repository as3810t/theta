plugins {
    base
    id("jacoco-common")
    checkstyle
    id("org.sonarqube").version("3.0")
}

buildscript {
    val libPath: String by extra { rootProject.projectDir.resolve("lib").path }
    extra["execPath"] = "$libPath${File.pathSeparator}${System.getenv("PATH")}"
}

allprojects {
    group = "hu.bme.mit.inf.theta"
    version = "2.4.0"

    apply(from = rootDir.resolve("gradle/shared-with-buildSrc/mirrors.gradle.kts"))
    apply(plugin = "checkstyle")

    checkstyle {
        configFile = file("${project.rootDir}/checkstyle.xml")
        isIgnoreFailures = true
        isShowViolations = true
    }

    sonarqube {
        properties {
            property("sonar.projectKey", "as3810t_theta")
            property("sonar.organization", "as3810t")
            property("sonar.host.url", "https://sonarcloud.io")
            property("sonar.coverage.jacoco.reportPaths", "build/jacoco/test.exec")
            property("sonar.java.checkstyle.reportPaths", "build/reports/checkstyle/main.xml,build/reports/checkstyle/test.xml")
        }
    }
}

evaluationDependsOnChildren()

tasks {
    val jacocoRootReport by creating(JacocoReport::class) {
        group = "verification"
        description = "Generates merged code coverage report for all test tasks."

        reports {
            html.isEnabled = false
            xml.isEnabled = true
            csv.isEnabled = false
        }

        val reportTasks = subprojects.mapNotNull { subproject ->
            subproject.tasks.named("jacocoTestReport", JacocoReport::class).orNull
        }

        dependsOn(reportTasks.flatMap { it.dependsOn })

        sourceDirectories = files(reportTasks.map { it.allSourceDirs })
        classDirectories = files(reportTasks.map { it.allClassDirs })
        val allExecutionData = files(reportTasks.map { it.executionData })
        // We only set executionData for declaring dependencies during task graph construction,
        // subprojects without tests will be filtered out in doFirst.
        executionData = allExecutionData

        doFirst {
            executionData = allExecutionData.filter { it.exists() }
        }
    }

    // Dummy test task for generating coverage report after ./gradlew test and ./gradlew check.
    val test by creating {
        finalizedBy(jacocoRootReport)
    }

    check {
        dependsOn(test)
    }

    withType<Checkstyle>().configureEach {
        includes.add("**/src/**")

        reports {
            xml.isEnabled = true
            html.isEnabled = true
        }
    }
}
