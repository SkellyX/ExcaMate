# ExcaMate

ExcaMate is a Fabric utility mod for players who want mining trips to stay focused. It combines direct-to-inventory pickup with a toggleable vein-mining helper for common stone, ore, and wood blocks.

## Features

- Sends mined drops straight into your inventory when space is available.
- Vein-mines connected matching stone, ore, and wood blocks.
- Uses separate per-mode block caps to avoid runaway breaks.
- Uses a single in-game toggle, bound to `V` by default.
- Saves settings in `config/excamate.json`.

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

ExcaMate is released under the CC0 license.
