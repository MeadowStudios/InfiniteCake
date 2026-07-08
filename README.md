# InfiniteCake

InfiniteCake is a Paper/Purpur plugin that lets players use cakes without permanently consuming them.

## Requirements

* Java 21
* Gradle
* Paper/Purpur 1.21.x

## Building

To build the plugin, run:

```bash
./gradlew clean build
```

On Windows, run:

```bat
gradlew.bat clean build
```

Build outputs are created in:

```text
build/libs/
```

## Obfuscation

If this plugin uses obfuscation, the obfuscation process is defined in:

```text
build.gradle.kts
```

If using the ProGuard setup, run:

```bash
./gradlew obfuscateJar
```

On Windows, run:

```bat
gradlew.bat obfuscateJar
```

The obfuscated jar is created in:

```text
build/libs/
```

The ProGuard mapping file is created in:

```text
build/reports/
```

The mapping file is not included in the published plugin jar.

## Plugin Metadata

The Bukkit plugin metadata is located in:

```text
src/main/resources/plugin.yml
```
