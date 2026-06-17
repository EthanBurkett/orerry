import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Fabric Loom — handles MC + Fabric dependency resolution, Mixin, decompilation
    // Version must be hardcoded here; gradle.properties not available in plugins block
    id("fabric-loom") version "1.17.11"
    // Kotlin JVM — Fabric Language Kotlin brings runtime support; this drives compilation
    kotlin("jvm") version "2.4.0"
}

// Eagerly read gradle.properties via extra.properties — snake_case keys match the file exactly.
val minecraftVersion     = extra["minecraft_version"] as String
val yarnMappings         = extra["yarn_mappings"] as String
val loaderVersion        = extra["loader_version"] as String
val fabricApiVersion     = extra["fabric_api_version"] as String
val fabricLangKtVersion  = extra["fabric_language_kotlin_version"] as String
val modVersion           = extra["mod_version"] as String
val mavenGroup           = extra["maven_group"] as String
val archivesBaseName     = extra["archives_base_name"] as String

// Java target: 21 is the minimum for MC 1.21.x / Fabric Loader 0.19.x.
// Building with Java 25 Temurin; javac --release 21 bytecode output is fully supported.
val javaTarget = 21

version = modVersion
group   = mavenGroup

base {
    archivesName = archivesBaseName
}

// Java toolchain declaration
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaTarget)
    }
    // Generate sources jar for IDE support
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

dependencies {
    // Minecraft + Yarn mappings (deobfuscation layer)
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")

    // Fabric Loader
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // Fabric API — the standard set of hooks/events
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    // Fabric Language Kotlin — ships Kotlin stdlib + coroutines into the mod environment
    modImplementation("net.fabricmc:fabric-language-kotlin:$fabricLangKtVersion")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release = javaTarget
    }

    processResources {
        inputs.property("version", project.version)
        inputs.property("loaderVersion", loaderVersion)
        inputs.property("minecraftVersion", minecraftVersion)
        inputs.property("fabricApiVersion", fabricApiVersion)
        inputs.property("fabricLangKtVersion", fabricLangKtVersion)

        filesMatching("fabric.mod.json") {
            expand(
                "version"                        to project.version,
                "loader_version"                 to loaderVersion,
                "minecraft_version"              to minecraftVersion,
                "fabric_api_version"             to fabricApiVersion,
                "fabric_language_kotlin_version" to fabricLangKtVersion,
            )
        }
    }

    jar {
        // Include LICENSE if present at project root (optional for skeleton)
        val licenseFile = rootProject.file("LICENSE")
        if (licenseFile.exists()) {
            from(licenseFile) { rename { "${it}_orrery" } }
        }
    }
}
