plugins {
    base
    id("pl.allegro.tech.build.axion-release") version "1.18.2"
}

scmVersion {
    tag {
        prefix.set("v")
    }
    nextVersion {
        suffix.set("SNAPSHOT")
        separator.set("-")
    }
}

allprojects {
    group   = property("projectGroup").toString()
    version = rootProject.scmVersion.version
}