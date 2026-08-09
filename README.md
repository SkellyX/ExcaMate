# ExcaMate

ExcaMate is an efficient Fabric mining companion that lets you vein mine ores, branch mine tunnels, and excavate 3x3x3 areas much faster, with optional auto-pickup for drops and direct XP collection.

## ExcaMate 1.3.0

### Compatibility

- Updated for Minecraft 26.2.
- Updated to Fabric Loader 0.19.3.
- Updated Fabric API support and tested against Fabric API 0.156.0+26.2.
- Improved compatibility with newer Fabric API versions within the Minecraft 26.2 release line.

### New Feature: Auto-Replant Crops

ExcaMate can now automatically replant crops after harvesting them.

To use it, hold the seeds or crop you want to replant in your off-hand before harvesting and harvest while holding down the excavate keybind.

Supported crops:

- Wheat
- Carrots
- Potatoes
- Beetroot
- Nether wart
- Torchflowers
- Pitcher plants

Auto-replanting can be turned off in ExcaMate's settings.

### New Vein-Mineable Blocks

#### Mining & Stone

Pickaxe:

- Sandstone and its variants
- Tuff
- Calcite
- Dripstone
- Blackstone
- Blackstone slabs, stairs and walls
- Chiseled blackstone
- Gilded blackstone

#### The Nether

Pickaxe:

- Netherrack
- Magma blocks
- Nether brick blocks
- Basalt
- Polished basalt
- Smooth basalt
- Bone blocks

Axe:

- Crimson stems and wood
- Warped stems and wood
- Mushroom blocks

Shovel:

- Crimson nylium
- Warped nylium

Crimson and warped wood now behave like normal overworld wood when using ExcaMate.

#### The End

Pickaxe:

- End stone
- End stone bricks
- Purpur blocks
- Purpur pillars
- Crying obsidian

Axe:

- Chorus plants

Chorus flowers are left to fall naturally after the supporting plant is removed.

#### Deep Dark

Hoe:

- Sculk
- Sculk catalysts
- Sculk sensors
- Sculk shriekers

#### Farming & Plants

Hoe:

- Crops
- Sugar cane
- Cactus
- Kelp
- Bamboo
- Vines
- Other supported tall plants

Sword:

- Bamboo can also be vein-mined with a sword, matching normal Minecraft tool behaviour.

#### Soil & Terrain

Shovel:

- Soul sand
- Soul soil
- Mud
- Snow

### Behaviour Changes

- Chests, barrels, furnaces and other storage or utility blocks are no longer vein-mined by default.
- These blocks can still be added manually through ExcaMate's settings if you specifically want them included.
- Only vanilla Minecraft blocks are included by default.
- Blocks added by other mods can still be added manually through the settings.

### Configuration Improvements

- Added the Auto-Replant Crops option to the in-game configuration screen.
- Added clearer text and tooltip information for the new setting.
- Added internal configuration versioning to make future config migrations safer.
- Removed an old migration rule that could potentially overwrite a user's deliberately chosen block-limit settings.
- Existing Deepstash configuration migration support has been preserved.

### Mod Compatibility Improvements

- Mod Menu is now fully optional.
- Cloth Config is now fully optional.
- ExcaMate can run on dedicated servers without either Mod Menu or Cloth Config installed.

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
- Optional automatic crop replanting using seeds or crops in your off-hand.
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
  - Automatically replant crops
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
  "configVersion": 1,
  "autoPickup": true,
  "autoCollectXp": true,
  "autoTorchInBranchMode": true,
  "autoReplantCrops": true,
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

- Minecraft `26.2`
- Fabric Loader `0.19.3` or newer
- Fabric API
- Java `25` or newer


## License

This project is licensed under the MIT License.
