plugins {
    `java-library`
}

dependencies {
    compileOnly("io.netty:netty-buffer:4.2.15.Final")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}