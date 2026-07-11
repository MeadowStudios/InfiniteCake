# InfiniteCake

InfiniteCake is a Paper/Purpur plugin that lets players use cakes without permanently consuming them.

## Features

- Infinite cake block that can be eaten without being consumed
- Admin command to set the cake location
- Protected cake and support block
- Prevents breaking, burning, physics removal, piston movement, liquid flow, and explosions from removing the cake
- Configurable food amount and saturation
- Optional sound, particles, and action bar effects
- Configurable messages
- Hex color support in messages

## Requirements

- Java 21
- Gradle
- Paper/Purpur 1.21.x

## Commands

| Command | Description | Permission |
|---|---|---|
| `/infinitecake set` | Enter placement mode and right-click where the infinite cake should be placed | `infinitecake.admin` |
| `/infinitecake cancel` | Cancel placement mode | `infinitecake.admin` |
| `/infinitecake info` | Show the current infinite cake location | `infinitecake.admin` |
| `/infinitecake reload` | Reload the plugin configuration | `infinitecake.admin` |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `infinitecake.admin` | Allows managing InfiniteCake | `op` |

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

## Release Jar

This project does not use ProGuard obfuscation.

The production jar is created by the normal Gradle build and can be found in:

```text
build/libs/
```

## Plugin Metadata

The Bukkit plugin metadata is located in:

```text
src/main/resources/plugin.yml
```

Main class:

```text
me.meadow.InfiniteCake
```
