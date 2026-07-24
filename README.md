# KitMod — Fabric Client Mod

Save and load full inventory snapshots (kits) with a clean GUI.  
**No server mod required to save.** Loading requires Creative mode.

## Features
- Press **V** to open the Kit Manager
- Save your full inventory (36 slots + armor + offhand = 41 slots total) as a named kit
- Pick a custom icon from your inventory when saving
- Load kits in Creative mode — restores every item to its exact original slot
- Rename or delete kits from the GUI
- Kits stored in `.minecraft/kits/*.json` — human-readable, easy to backup

## Installation
1. Install [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Drop `kitmod-1.0.0.jar` into your `.minecraft/mods/` folder
3. Launch Minecraft 1.21+

## Building from Source
```bash
./gradlew build
# Output: build/libs/kitmod-1.0.0.jar
```

Requires Java 21+.

## File Layout
```
src/main/java/com/kitmod/
  KitModClient.java              ← Fabric entrypoint
  client/
    gui/KitManagerScreen.java    ← Main GUI (screen)
    keybind/KitKeybind.java      ← V key binding
  data/
    Kit.java                     ← Kit data model
    KitStorage.java              ← JSON persistence (.minecraft/kits/)
  util/
    InventoryHelper.java         ← Snapshot & restore all 41 slots
```

## Compatibility
Targets Minecraft 1.21.4+ with Yarn mappings.  
For future versions: update `minecraft_version`, `yarn_mappings`, and `fabric_version` in `gradle.properties`.

## Kit JSON Format
Kits are stored in `.minecraft/kits/<name>.json`:
```json
{
  "name": "PvP Kit",
  "iconItemId": "minecraft:diamond_sword",
  "savedAt": "2025-01-15T10:30:00Z",
  "slots": {
    "0":  "{ id: \"minecraft:diamond_sword\", ... }",
    "36": "{ id: \"minecraft:diamond_boots\", ... }",
    "40": "{ id: \"minecraft:shield\", ... }"
  }
}
```
Slots 0–35 = main inventory, 36–39 = armor (boots→helmet), 40 = offhand.
