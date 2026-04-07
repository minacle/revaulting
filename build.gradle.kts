plugins {
    java
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

group = "moe.minacle.minecraft"
version = "0.5.0"

repositories {
    mavenCentral()
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${libs.versions.minecraft.get()}.build.${libs.versions.paper.build.get()}")
    implementation(libs.bstats.bukkit)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }

    shadowJar {
        archiveClassifier.set("")
        enableAutoRelocation.set(true)
        relocationPrefix.set("moe.minacle.minecraft.plugins.revaulting.shadowjar")
        minimize()
    }
}

runPaper.folia.registerTask()
