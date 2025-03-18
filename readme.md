# LestoraRPG

LestoraRPG will eventually add a bunch of small features that make Minecraft more hardcore or realistic.  Not graphically, but functionally.  For example, more likelihood for blocks to collapse when you are mining, better trading, and some utilities.

**Swimming complexity:  If you get in over your head, metaphorically speaking, you're going to feel quite the trauma (mentally and physically).  Until you learn to swim.**
**Body Temp: If you're too hot or cold, you will take damage.  To see your temperature, for now, run the showDebug command.  Definitely don't hold a bucket full of lava, for now.**

Current Development 1: Going to be splitting these features out into individual mods, check out the new Lestora Dynamic Lighting mod, Lestora Highlighter, and next is a "Common" dependency mod.
Current Development 2: Learning to swim.  Time in water, Time in game, Particular potions (water breathing), turtle helmet, etc.  As vanilla as possible.  Until then... use a boat!

## Features
- **Villager AI:** If you're running Ollama Mistral, villagers will now chat with you!  Right click one to focus them, and then just chat away! (Wait a few seconds for responses depending on your AI/VRAM setup).  Villagers will now freeze during conversation or focus, or if 10 seconds pass with no other interaction.
- **Configuration:** Added lestora-common.toml, currently for configuring dynamic lighting.
- **Client-Side Only (Currently):** Works locally without affecting multiplayer sessions.
- **Wetness:** In the near future, one form of complexity will be swimming and temperature.  At the moment, you can view the debug info with the showDebug command.  This will tell you the current level of wetness, and your supporting block.  It currently shows you instantly dry out of water, but I'll have dampness persist longer soon.

## Manual Installation
1. Download the mod JAR from CurseForge.
2. Place the JAR file into your `mods` folder.
3. Launch Minecraft with the Forge profile.

## Commands
- Use the command `/lestora setLevels swimLevel <0-3>` to set your swimLevel.  Defaults to 0, which means you essentially insta-die in water that's two deep (punny).  Level 3 means you can swim.
- Use the command `/lestora showDebug [true/false]` to show current debug info related to beta development.

## Compatibility
- **Minecraft Version:** 1.21.4
- **Forge Version:** 54.1.0

## Troubleshooting
If you run into issues (e.g., crashes or unexpected behavior), check the logs in your `crash-reports` or `logs` folder. You can also open an issue on the mod’s GitHub repository.

## Contributing
Contributions are welcome! Please submit pull requests or open issues if you have suggestions or bug reports.

## License
This project is licensed under the MIT License.
