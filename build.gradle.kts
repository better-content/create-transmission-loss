plugins {
    kotlin("jvm") version "1.9.22"
}

group = property("mod_group") as String
version = property("mod_version") as String

repositories {
    mavenCentral()
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src/main/kotlin"))
            include("io/github/transmissionloss/network/**")
            include("io/github/transmissionloss/config/**")
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
