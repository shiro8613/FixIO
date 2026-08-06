plugins {
    `java-library`
}

dependencies {
    compileOnly("io.netty:netty-buffer:4.2.15.Final")
    compileOnly("it.unimi.dsi:fastutil:8.5.18")
    implementation("org.lwjgl:lwjgl:3.4.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
