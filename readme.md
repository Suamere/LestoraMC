# LestoraMC

LestoraMC will eventually add a bunch of small features that make Minecraft more hardcore or realistic.  Not graphically, but functionally.  For example, more likelihood for blocks to collapse when you are mining, better trading, and some utilities.

Currently, it features a small test modification that highlights blocks with red boxes based on configurable settings. It’s designed to help with debugging and visualizing block exposure in the game.

## Features
- **Highlight Blocks:** Highlights solid blocks within a specified radius.
- **Exposure Check:** Only highlights blocks that are exposed to air, non-opaque blocks, or lava.
- **Customizable:** Set the highlight radius and center using in-game commands.
- **Client-Side Only:** Works locally without affecting multiplayer sessions.

## Installation
1. Download the mod JAR from CurseForge.
2. Place the JAR file into your `mods` folder.
3. Launch Minecraft with the Forge profile.

## Usage
- Use the command `/setHighlight <radius>` to define the highlight area at your current location.
- The mod will then precompute the block positions and render red boxes around eligible blocks.
- Use radius 0 to disable.

## Compatibility
- **Minecraft Version:** 1.21.4
- **Forge Version:** 54.1.0

## Troubleshooting
If you run into issues (e.g., crashes or unexpected behavior), check the logs in your `crash-reports` or `logs` folder. You can also open an issue on the mod’s GitHub repository.

## Contributing
Contributions are welcome! Please submit pull requests or open issues if you have suggestions or bug reports.

## License
This project is licensed under the MIT License.
