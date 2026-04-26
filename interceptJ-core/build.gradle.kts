plugins {
    id("unconquerable.java-conventions")
}

dependencies {

    implementation(platform(project(":interceptJ-bom")))
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("tools.jackson.core:jackson-databind")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")

    
}