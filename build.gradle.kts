import proguard.gradle.ProGuardTask
import java.io.File

plugins {
    java
}

group = "me.meadow"
version = "1.0"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.6.1")
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.isDebug = false
    }

    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    jar {
        archiveBaseName.set("InfiniteCake")
        archiveVersion.set(project.version.toString())
    }
}

val obfuscateJarTask = tasks.register<ProGuardTask>("obfuscateJar") {
    group = "build"
    description = "Creates an obfuscated release jar with ProGuard."

    dependsOn("jar")

    outputs.upToDateWhen { false }

    doFirst {
        val libsDir = layout.buildDirectory.dir("libs").get().asFile

        val inputJar = File(libsDir, "InfiniteCake-${project.version}.jar")
        if (!inputJar.exists()) {
            throw GradleException("Could not find jar to obfuscate: ${inputJar.absolutePath}")
        }

        val outputJar = File(libsDir, "InfiniteCake-${project.version}-obf.jar")
        val mapFile = layout.buildDirectory.file("reports/proguard-infinitecake-map.txt").get().asFile

        outputJar.parentFile.mkdirs()
        mapFile.parentFile.mkdirs()

        if (outputJar.exists() && !outputJar.delete()) {
            throw GradleException("Could not delete old obfuscated jar: ${outputJar.absolutePath}")
        }

        println("ProGuard input jar: ${inputJar.absolutePath}")
        println("ProGuard output jar: ${outputJar.absolutePath}")

        injars(inputJar)
        outjars(outputJar)
        printmapping(mapFile)

        dontshrink()
        dontoptimize()

        keepattributes(
            "RuntimeVisibleAnnotations," +
                    "RuntimeInvisibleAnnotations," +
                    "Signature," +
                    "InnerClasses," +
                    "EnclosingMethod," +
                    "Record"
        )

        dontwarn()
        ignorewarnings()

        libraryjars("${System.getProperty("java.home")}/jmods/java.base.jmod")

        keep("public class me.meadow.InfiniteCake { *; }")

        keep("public class me.meadow.command.InfiniteCakeCommand { *; }")

        keep("public class me.meadow.cake.InfiniteCakeManager { *; }")
        keep("public class me.meadow.cake.InfiniteCakeManager\$CakeLocation { *; }")

        keepclassmembers("enum * { public static **[] values(); public static ** valueOf(java.lang.String); }")
    }

    doLast {
        val outputJar = layout.buildDirectory.file("libs/InfiniteCake-${project.version}-obf.jar").get().asFile

        if (!outputJar.exists()) {
            throw GradleException("ProGuard finished, but the obfuscated jar was not created: ${outputJar.absolutePath}")
        }

        println("Obfuscated jar created: ${outputJar.absolutePath}")
        println("Do not upload build/reports/proguard-infinitecake-map.txt")
    }
}

tasks.named("build") {
    dependsOn(obfuscateJarTask)
}
