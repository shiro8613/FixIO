plugins {
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("maven-publish")
}

dependencies {
    minecraft("com.mojang:minecraft:26.2")
    implementation("net.fabricmc:fabric-loader:0.19.3")

    implementation(project(":native-api"))
    include(project(":native-api"))

    implementation("org.lwjgl:lwjgl:3.4.1")
    include("org.lwjgl:lwjgl:3.4.1")

    include("org.lwjgl:lwjgl:3.4.1:natives-windows")
    include("org.lwjgl:lwjgl:3.4.1:natives-linux")
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
