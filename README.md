# CarpetGUI Rewrite

A client-side GUI for browsing and editing Carpet mod rules in-game, rewritten for Minecraft 26.1.

## Features

- **Rule Browser** — Browse all Carpet rules and gamerules in a scrollable list with category tabs
- **Edit Rules** — Click any rule to view details and change its value
- **Favorites** — Star rules for quick access
- **Toggle Switches** — Boolean rules show a clickable toggle on the right
- **Rule Stack** — Track and manage rule changes with layer-based history
- **Prefabs** — Save and apply groups of rule configurations
- **Multi-language** — Supports all languages that Carpet provides translations for
- **Rule Groups** — Batch select and reset rules to defaults

## Usage

Press **F9** to open the GUI.

## Requirements

- Minecraft 26.1
- Fabric Loader
- Carpet Mod 26.1+
- Fabric API

## Building

```bash
./gradlew build
```

The built JAR will be in `build/libs/`.
