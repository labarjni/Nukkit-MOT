import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    id("java-library")
    id("maven-publish")
    id("application")
    alias(libs.plugins.shadow)
}

group = "cn.nukkit"
version = "MOT-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://repo.lanink.cn/repository/maven-public/")
    maven("https://repo.okaeri.cloud/releases")
}

dependencies {
    api(libs.raknet)
    api(libs.netty.epoll)
    api(libs.netty.codec.haproxy)
    api(libs.nukkitx.natives)

    api(libs.cloudburst.common) {
        exclude("org.cloudburstmc.math", "immutable")
        exclude("io.netty", "netty-buffer")
        exclude("org.cloudburstmc.fastutil.maps", "int-object-maps")
        exclude("org.cloudburstmc.fastutil.maps", "object-int-maps")
    }

    api(libs.fastutil)
    api(libs.guava)
    api(libs.gson)
    api(libs.caffeine) {
        exclude("org.checkerframework", "checker-qual")
        exclude("com.google.errorprone", "error_prone_annotations")
    }
    api(libs.bundles.snakeyaml)
    api(libs.jackson.dataformat.toml)
    api(libs.okaeri.configs.yaml.snakeyaml)
    api(libs.nimbus.jose.jwt)
    api(libs.asm)
    api(libs.bundles.leveldb)
    api(libs.bundles.terminal)
    api(libs.bundles.log4j)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    compileOnly(libs.jsr305)

    api(libs.snappy)

    api(libs.daporkchop.natives) {
        exclude("io.netty", "netty-buffer")
    }

    api(libs.sentry)
    api(libs.commons.math3)
    api(libs.snappy.java)
    api(libs.oshi.core)
    compileOnly(libs.annotations)

    api(libs.jose4j) {
        exclude("org.slf4j", "slf4j-api")
    }

    api(libs.block.state.updater)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.bundles.mockito)
    testRuntimeOnly(libs.junit.engine)
}

application {
    mainClass.set("cn.nukkit.Nukkit")
}

publishing {
    repositories {
        maven {
            name = "repo-lanink-cn-snapshots"
            url = uri("https://repo.lanink.cn/repository/maven-snapshots/")
            credentials {
                username = System.getenv("DEPLOY_USERNAME")
                password = System.getenv("DEPLOY_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-nowarn", "-Xlint:none"))
    }

    test {
        useJUnitPlatform()
        jvmArgs = listOf(
            "--enable-native-access=ALL-UNNAMED",
            "--add-opens=java.base/sun.misc=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "-Dorg.slf4j.simpleLogger.defaultLogLevel=off"
        )
    }

    jar {
        archiveClassifier.set("dev")
    }

    shadowJar {
        manifest.attributes["Multi-Release"] = "true"
        manifest.attributes["Main-Class"] = "cn.nukkit.Nukkit"

        transform(Log4j2PluginsCacheFileTransformer())

        destinationDirectory.set(file("$projectDir/target"))
        archiveClassifier.set("")

        exclude("javax/annotation/**")
    }

    runShadow {
        val dir = File(projectDir, "run")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        standardInput = System.`in`
        workingDir = dir

        jvmArgs = listOf(
            "--enable-native-access=ALL-UNNAMED",
            "--add-opens=java.base/sun.misc=ALL-UNNAMED",
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "-Dio.netty.tryReflectionSetAccessible=true",
            "-Dorg.slf4j.simpleLogger.defaultLogLevel=off",
            "-Djava.util.logging.config.class=java.util.logging.LogManager"
        )
    }

    javadoc {
        options.encoding = "UTF-8"
        options.quiet()
    }
}
