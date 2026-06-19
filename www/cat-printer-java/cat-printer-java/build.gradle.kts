plugins {
    `java-library`
}

group = "com.catprinter"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // BLE backends:
    // - Linux: copy examples/linux-bluez/BluezTransport.java into your project
    //   and add com.sputnikdev:bluetooth-manager.
    // - Windows: bundled. See windows-helper/ — its compiled .exe is copied
    //   into resources by the copyWindowsHelper task below.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

val helperResourceDir = layout.buildDirectory.dir("generated/windows-helper-resources")
val helperSource = layout.projectDirectory.file("windows-helper/dist/cat-printer-ble-helper.exe")

val copyWindowsHelper by tasks.registering(Copy::class) {
    from(helperSource) {
        rename { "cat-printer-ble-helper.exe" }
    }
    into(helperResourceDir.map { it.dir("com/catprinter/ble/windows") })
    onlyIf {
        val exists = helperSource.asFile.exists()
        if (!exists) {
            logger.warn(
                "windows-helper/dist/cat-printer-ble-helper.exe not found. " +
                "Build it on a Windows machine with .NET 8 SDK; see windows-helper/README.md. " +
                "Without it, com.catprinter.ble.windows.WindowsTransport cannot start at runtime."
            )
        }
        exists
    }
}

sourceSets.main {
    resources.srcDir(helperResourceDir)
}

tasks.processResources {
    dependsOn(copyWindowsHelper)
}
