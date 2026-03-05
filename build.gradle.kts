plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.10"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.github.hlibkoval"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3")
        bundledPlugin("org.jetbrains.plugins.terminal")
        plugin("com.anthropic.code.plugin", "0.1.14-beta")
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.hlibkoval.claudecodex"
        name = "Claude Code X"
        version = project.version.toString()
        description = "Claude Code X: QoL extensions for the official Claude Code [Beta] plugin by Anthropic"
        vendor {
            name = "Hlib Koval"
            url = "https://github.com/hlibkoval"
        }
        ideaVersion {
            sinceBuild = "253"
            untilBuild = provider { null }
        }
    }
}
