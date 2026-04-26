plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withJavadocJar()
}

repositories {
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        locale = "en"
        addStringOption("Xdoclint:all,-missing", "-quiet")
        links("https://docs.oracle.com/en/java/javase/25/docs/api/")
        links("https://jakarta.ee/specifications/annotations/3.0/apidocs/")
    }
}