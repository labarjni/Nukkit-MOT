plugins {
    id("java")
}

group = "cn.nukkit.ddui"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://repo.lanink.cn/repository/maven-public/")
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "../target", "include" to "*.jar")))
    compileOnly("cn.nukkit:nukkit:MOT-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        archiveBaseName.set("DDUI-TestPlugin")
        archiveClassifier.set("")
    }
}
