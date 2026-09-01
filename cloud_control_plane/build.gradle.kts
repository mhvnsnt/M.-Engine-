plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    val ktorVersion = "2.3.4"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.example.ai.cloud.MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.example.ai.cloud.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
