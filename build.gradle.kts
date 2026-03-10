buildscript {
    repositories {
        maven("https://maven.minecraftforge.net")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:6.0.36")
        classpath("org.spongepowered:mixingradle:0.7-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

apply(plugin = "org.jetbrains.kotlin.jvm")
apply(plugin = "net.minecraftforge.gradle")
apply(plugin = "org.spongepowered.mixin")

group = property("mod_group") as String
version = property("mod_version") as String

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://maven.createmod.net")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}

minecraft {
    mappings("official", property("minecraft_version") as String)

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            arg("-mixin.config=${property("mod_id")}.mixins.json")
            mods {
                create(property("mod_id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("server") {
            workingDirectory(project.file("run"))
            arg("-mixin.config=${property("mod_id")}.mixins.json")
            mods {
                create(property("mod_id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("gameTestServer") {
            workingDirectory(project.file("run"))
            property("forge.enabledGameTestNamespaces", property("mod_id") as String)
            arg("-mixin.config=${property("mod_id")}.mixins.json")
            mods {
                create(property("mod_id") as String) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

dependencies {
    "minecraft"("net.minecraftforge:forge:${property("minecraft_version")}-${property("forge_version")}")
    "implementation"("thedarkcolour:kotlinforforge:${property("kff_version")}")
    "implementation"(fg.deobf("com.simibubi.create:create-${property("minecraft_version")}:${property("create_version")}"))

    "testImplementation"(kotlin("test"))
    "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.processResources {
    val props = mapOf(
        "modId" to property("mod_id"),
        "modName" to property("mod_name"),
        "modVersion" to property("mod_version"),
        "minecraftVersion" to property("minecraft_version"),
        "forgeVersion" to property("forge_version"),
        "createVersion" to property("create_version")
    )
    inputs.properties(props)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

mixin {
    add(sourceSets.main.get(), "${property("mod_id")}.refmap.json")
    config("${property("mod_id")}.mixins.json")
}
