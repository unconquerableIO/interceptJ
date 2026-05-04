plugins {
    id("unconquerable.java-conventions")
    id("unconquerable.publish-conventions")
}

dependencies {
    implementation(platform(project(":interceptJ-bom")))
    implementation(project(":interceptJ-core"))
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("ml.dmlc:xgboost4j_2.13")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.1")
}


