plugins {
    id("java")
}

group = "io.wesner.robert.cb1060.moderninventory"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://maven.robert.wesner.io/repository/maven-public/")
    maven("https://maven.robert.wesner.io/repository/johnymuffin-maven-public/")
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    implementation("org.jspecify:jspecify:1.0.0")
    implementation("com.legacyminecraft.poseidon:poseidon-craftbukkit:1.+")
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand(project.properties)
    }
}
