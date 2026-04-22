plugins {
    id("unconquerable.base-conventions")
    `java-platform`
}

dependencies {
    constraints {
        api(project(":intercept-java-core"))
        api("tools.jackson.core:jackson-databind:3.1.2")
        api("org.junit.jupiter:junit-jupiter:5.10.1")
        api("org.junit.platform:junit-platform-launcher:1.10.1")
        api("jakarta.annotation:jakarta.annotation-api:3.0.0")
    }
}