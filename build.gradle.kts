import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("org.spongepowered.mixin") version "0.7.+"
    kotlin("jvm") version "1.9.22"
}

val minecraftVersion = project.property("minecraft_version") as String
val forgeVersion = project.property("forge_version") as String
val kffVersion = project.property("kff_version") as String
val createVersion = project.property("create_version") as String
val createVersionRange = project.property("create_version_range") as String
val ponderVersion = project.property("ponder_version") as String
val modId = project.property("mod_id") as String
val modName = project.property("mod_name") as String
val modVersion = project.property("mod_version") as String
val buildJvmVersion = 17
val vendoredKffJar = file("vendor/mods/kotlinforforge-$kffVersion-all.jar")

group = project.property("mod_group") as String
version = modVersion

base {
    archivesName.set("create-transmission-loss")
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.createmod.net")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
    flatDir {
        dirs("vendor/mods")
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(buildJvmVersion))
    withSourcesJar()
}

kotlin {
    jvmToolchain(buildJvmVersion)
}

minecraft {
    mappings("official", minecraftVersion)

    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") {
            property("forge.enabledGameTestNamespaces", modId)
        }

        create("server") {
            property("forge.enabledGameTestNamespaces", modId)
            arg("--nogui")
        }

        create("gameTestServer") {
            property("forge.enabledGameTestNamespaces", modId)
        }

        create("data") {
            args(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources").absolutePath,
                "--existing", file("src/main/resources").absolutePath
            )
        }
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

dependencies {
    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")
    implementation(fg.deobf("com.simibubi.create:create-$minecraftVersion:$createVersion:slim"))
    implementation(fg.deobf("net.createmod.ponder:Ponder-Forge-$minecraftVersion:$ponderVersion"))
    if (vendoredKffJar.exists()) {
        implementation(files(vendoredKffJar))
    } else {
        implementation("thedarkcolour:kotlinforforge:$kffVersion")
    }

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.processResources {
    val props = mapOf(
        "modId" to modId,
        "modName" to modName,
        "modVersion" to modVersion,
        "minecraftVersion" to minecraftVersion,
        "forgeVersion" to forgeVersion,
        "createVersion" to createVersion,
        "createVersionRange" to createVersionRange,
        "kffVersion" to kffVersion
    )
    inputs.properties(props)
    filesMatching("META-INF/mods.toml") {
        expand(props)
    }
}

val syncGameTestStructures by tasks.registering(Copy::class) {
    from("src/main/resources/gameteststructures")
    into(layout.projectDirectory.dir("run/gameteststructures"))
}

tasks.matching { it.name.startsWith("prepareRun") }.configureEach {
    dependsOn(syncGameTestStructures)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

mixin {
    add(sourceSets.main.get(), "transmissionloss.refmap.json")
    config("transmissionloss.mixins.json")
}
