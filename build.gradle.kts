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
    //   into resources at build time.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    compileOnly("org.jetbrains:annotations:26.1.0")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

val helperExe = file("windows-helper/dist/cat-printer-ble-helper.exe")

tasks.named<ProcessResources>("processResources") {
    if (helperExe.exists()) {
        from(helperExe) {
            into("com/catprinter/ble/windows")
        }
    } else {
        doFirst {
            logger.warn(
                "windows-helper/dist/cat-printer-ble-helper.exe not found. " +
                "Build it on a Windows machine with .NET 8 SDK; see windows-helper/README.md."
            )
        }
    }
}
