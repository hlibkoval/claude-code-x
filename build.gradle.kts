import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
        intellijIdea("2026.1.1")
        bundledPlugin("org.jetbrains.plugins.terminal")
        plugin("com.anthropic.code.plugin", "0.1.14-beta")
    }
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
    testRuntimeOnly("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    test {
        useJUnitPlatform()
    }
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
            sinceBuild = "261"
            untilBuild = provider { null }
        }
    }
}
