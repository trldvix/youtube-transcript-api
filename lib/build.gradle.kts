import com.vanniktech.maven.publish.SonatypeHost

object Metadata {
    const val DESC = "Java library for retrieving YouTube transcripts. " +
            "It supports manual and automatically generated subtitles and does not use headless browser for scraping"
    const val GROUP_ID = "io.github.trldvix"
    const val LICENSE = "MIT"
    const val LICENSE_URL = "https://opensource.org/licenses/MIT"
    const val GITHUB_REPO = "trldvix/youtube-transcript-api"
    const val DEVELOPER_ID = "trldvix"
    const val DEVELOPER_NAME = "Alexey Bobkov"
    const val DEVELOPER_EMAIL = "dignitionn@gmail.com"
}

plugins {
    `java-library`
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.gradle.release)
}

repositories {
    mavenCentral()
}

tasks.withType<JavaCompile> {
    val javaVersion = if (name == "compileTestJava") 21 else 11
    javaCompiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(javaVersion) }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.apache.commons.text)

    testRuntimeOnly(libs.junit.jupiter.platform.launcher)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.mockito.junit.jupiter)
}

release {
    versionPropertyFile = "../gradle.properties"
    newVersionCommitMessage = "chore: set next development version to"
    preTagCommitMessage = "chore: bump current version to"
}

mavenPublishing {
    coordinates(groupId = Metadata.GROUP_ID, artifactId = rootProject.name, version = project.version.toString())
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    pom {
        name = project.name
        description = Metadata.DESC
        url = "https://github.com/${Metadata.GITHUB_REPO}"
        inceptionYear = "2024"

        licenses {
            license {
                name = Metadata.LICENSE
                url = Metadata.LICENSE_URL
            }
        }

        developers {
            developer {
                id = Metadata.DEVELOPER_ID
                name = Metadata.DEVELOPER_NAME
                email = Metadata.DEVELOPER_EMAIL
            }
        }

        scm {
            connection = "scm:git:git://github.com/${Metadata.GITHUB_REPO}.git"
            developerConnection = "scm:git:ssh://github.com:${Metadata.GITHUB_REPO}.git"
            url = "https://github.com/${Metadata.GITHUB_REPO}.git"
        }

        issueManagement {
            url = "https://github.com/${Metadata.GITHUB_REPO}/issues"
        }
    }
    signAllPublications()
}