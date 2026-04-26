import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    pom {
        name.set(project.name)
        description.set("interceptJ — Fraud detection Java library.")
        inceptionYear.set("2026")
        url.set("https://github.com/unconquerableIO/interceptJ")

        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("rizwanidrees")
                name.set("Rizwan Idrees")
                url.set("https://github.com/unconquerableIO")
            }
        }

        scm {
            url.set("https://github.com/unconquerableIO/interceptJ")
            connection.set("scm:git:git://github.com/unconquerableIO/interceptJ.git")
            developerConnection.set("scm:git:ssh://git@github.com/unconquerableIO/interceptJ.git")
        }
    }
}