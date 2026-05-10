# ExcaMate

ExcaMate is an efficient Fabric mining companion that lets you vein mine ores, branch mine tunnels, and excavate 3x3x3 areas much faster, with optional auto-pickup for drops and direct XP collection.

## Features

- Hold-to-activate mining, no toggle state.
- Three mining modes: Vein, Branch, and Excavate.
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
- Configurable allowlists and blocklists for mass mining and auto-pickup.
- In-game configuration through Mod Menu and Cloth Config.
- Safety checks for protected or special blocks.

## Controls

- Hold the ExcaMate mining key to activate the selected mining mode.
- Press the cycle mode key to switch between Vein, Branch, and Excavate.
- Keybinds can be changed in Minecraft's Controls menu under the ExcaMate category.

## Mining Modes

**Vein**

Mines connected valid blocks of the same type. Ideal for ores, logs, and loose materials.

**Branch**

Creates a clean 1-wide, 2-tall tunnel in front of you. Perfect for branch mining.

**Excavate**

Mines a 3x3x3 volume based on the block side you mine. The excavation extends inward from the targeted face and is great for clearing larger spaces quickly.

## Auto-Pickup and XP

ExcaMate can automatically collect drops and XP during ExcaMate mining, keeping your inventory tidy and reducing clutter.

**Item Auto-Pickup**

Mined drops go directly into your inventory when possible. If your inventory is full, remaining items drop into the world normally.

**Direct XP Collection**

XP can be awarded instantly without spawning XP orbs, making mining smoother and reducing orb clutter.

Both features are configurable from ExcaMate's in-game settings screen.

## Configuration

ExcaMate can be configured in-game through **Mod Menu**.

Open **Mods**, select **ExcaMate**, then click **Configure**. The settings screen saves changes immediately, so singleplayer changes apply without restarting Minecraft.

The in-game settings screen includes:

- **General**
  - Auto-pickup drops
  - Direct XP collection
  - Auto torch placement in Branch mode
  - Default mining mode
- **Block Limits**
  - Vein max blocks: default `12`
  - Branch max blocks: default `16`
  - Excavate max blocks: default `27`
- **Block Lists**
  - Extra vein mine allowlist
  - Vein mine blocklist
  - Auto-pickup blocklist

Block lists use a searchable block selector, so you can search for blocks like `obsidian`, `hay block`, or `gravel` without typing full block IDs manually.

Block limits are total action limits. The first block you break manually counts toward the limit. Excavate defaults to `27`, which allows a full 3x3x3 excavation where all blocks are valid.

`extraVeinMineAllowList` adds extra blocks ExcaMate can mass-mine on top of its normal supported blocks. `veinMineBlockList` prevents listed blocks from being mass-mined.

When `autoPickup` is enabled, ExcaMate tries to pick up drops from every block it successfully mines. `autoPickupBlockList` prevents drops from specific source blocks, such as `minecraft:gravel`, from being inserted into your inventory.

Blocklists override default support and extra allowlists. Adding a block to `extraVeinMineAllowList` does not bypass tool or harvest rules, so ExcaMate still requires the correct tool category and vanilla-compatible mining behavior.

You can still edit `config/excamate.json` manually if preferred. New config files use these defaults:

```json
{
  "autoPickup": true,
  "autoCollectXp": true,
  "autoTorchInBranchMode": true,
  "veinMaxBlocks": 12,
  "branchMaxBlocks": 16,
  "excaMateMaxBlocks": 27,
  "defaultMode": "VEIN",
  "extraVeinMineAllowList": [],
  "veinMineBlockList": [],
  "autoPickupBlockList": []
}
```

## Requirements

- Minecraft `26.1.2`
- Fabric Loader `0.19.2` or newer
- Fabric API
- Java `25` or newer


## License

This project is licensed under the MIT License.
