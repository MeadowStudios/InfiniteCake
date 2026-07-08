# InfiniteCake

Private source repository.

## Requirements

- Java 21
- Gradle
- Paper/Purpur 1.21.x

## Build

To build the plugin, run:

gradlew.bat clean build

Build outputs are created in:

build/libs/

## Obfuscation

If this plugin uses obfuscation, the obfuscation process is defined in build.gradle.kts.

If using the ProGuard setup, run:

gradlew.bat obfuscateJar

The obfuscated jar is created in:

build/libs/

The ProGuard mapping file is created in:

build/reports/

The mapping file is not included in the published plugin jar.

## Notes for Modrinth moderation

This repository contains the source code, Gradle build files, plugin resources, build instructions, and obfuscation information needed to review the submitted jar.
