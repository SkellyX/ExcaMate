# ExcaMate

ExcaMate is an efficient Fabric mining companion that lets you vein mine ores, branch mine tunnels, and excavate 3x3 areas much faster, with optional auto-pickup for drops and direct XP collection.

## Features

- Hold-to-activate mining, no toggle state.
- Three mining modes: Vein, Branch 1x2, and ExcaMate 3x3.
- Tool-aware mining:
  - Pickaxes: stone and ores.
  - Axes: logs and wood.
  - Shovels: dirt, sand, gravel, clay, and similar shovel blocks.
- Smart durability handling so ExcaMate stops before breaking your tool.
- Supports Unbreaking.
- Respects Silk Touch and Fortune.
- Optional direct XP collection.
- Optional item auto-pickup.
- Optional auto-torch placement in Branch mode when torches are in your off-hand.
- Configurable block limits per mode.
- Safety checks for protected or special blocks.

## Controls

- Hold the ExcaMate mining key to activate the selected mining mode.
- Press the cycle mode key to switch between Vein, Branch 1x2, and ExcaMate 3x3.
- Keybinds can be changed in Minecraft's Controls menu under the ExcaMate category.

## Mining Modes

**Vein**

Mines connected valid blocks of the same type. Ideal for ores, logs, and loose materials.

**Branch 1x2**

Creates a clean 1-wide, 2-tall tunnel in front of you. Perfect for branch mining.

**ExcaMate 3x3**

Mines a 3x3 face based on the block side you mine. Great for clearing larger spaces quickly.

## Auto-Pickup and XP

ExcaMate can automatically collect drops and XP during ExcaMate mining, keeping your inventory tidy and reducing clutter.

**Item Auto-Pickup**

Mined drops go directly into your inventory when possible. If your inventory is full, remaining items drop into the world normally.

**Direct XP Collection**

XP can be awarded instantly without spawning XP orbs, making mining smoother and reducing orb clutter.

Both features are configurable and can be enabled or disabled in the config file.

## Configuration

ExcaMate creates this file the first time it runs:

```json
{
  "autoPickup": true,
  "autoCollectXp": true,
  "autoTorchInBranchMode": true,
  "veinMaxBlocks": 64,
  "branchMaxBlocks": 32,
  "excaMateMaxBlocks": 27,
  "defaultMode": "VEIN"
}
```

## Requirements

- Minecraft `26.1.2`
- Fabric Loader `0.19.2` or newer
- Fabric API
- Java `25` or newer

## Building

Run:

```powershell
.\gradlew.bat build
```

The compiled jar is written to `build/libs/`.

## License

This project is licensed under the MIT License.
