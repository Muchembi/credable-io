plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

object Versions {
    const val LOMBOK = "8.7.1"
}

dependencies {
    implementation("io.freefair.gradle:lombok-plugin:${Versions.LOMBOK}")
}

