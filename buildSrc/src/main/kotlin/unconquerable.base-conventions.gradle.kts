plugins {
    `maven-publish`
}

group = "io.unconquerable"
version = project.findProperty("projectVersion") ?: "0.0.1-SNAPSHOT"
