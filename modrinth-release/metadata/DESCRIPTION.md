# Titan Performance

Boost your Minecraft FPS with intelligent entity culling, dynamic frame rate control, and memory optimization. Titan Performance is an all-in-one performance mod designed to make Minecraft run smoother on any hardware.

Developed by LeoneDev at LeonesModFactory

## Why Titan Performance?

Unlike other performance mods that require complex configuration, Titan Performance works out of the box with intelligent hardware detection and auto-configuration. It combines multiple optimization techniques into a single, unified mod with a beautiful settings interface.

### Key Features

Entity Culling skips rendering entities you cannot see, providing 10 to 30 percent FPS improvement.
Dynamic FPS reduces frame rate when unfocused, saving power.
Memory Optimizer reduces garbage collection stutter for smoother gameplay.
Particle Optimizer culls distant particles when FPS is low, providing 5 to 15 percent FPS improvement.
Entity Throttler reduces tick rate for distant entities, providing 5 to 10 percent FPS improvement.

## Installation

1. Install Fabric Loader for your Minecraft version
2. Install Fabric API
3. Download Titan Performance and place in your mods folder
4. Launch Minecraft and optimizations are enabled by default

## Configuration

Press P in-game to open the settings screen, or access it through:
Options with Titan Perf button (top-left)
Video Settings with Titan Perf button (top-left)
Mod Menu (if installed)

### Keybinds
P opens Titan Performance settings.
F8 toggles Dynamic FPS on/off.

## Module Overview

### Rendering Optimization
Optimizes chunk rendering with intelligent scheduling. Reduces stuttering during chunk loading and improves overall frame times.

### Entity Culling
Skips rendering entities that are beyond the culling distance (configurable), behind the player (back-face culling), or too small to see at distance.

### Dynamic FPS Control
Automatically reduces frame rate when the game window is not focused (default 10 FPS) or in menus or pause screen (default 60 FPS).

### Memory Optimizer
Uses object pooling to reduce garbage collection pauses. Results in smoother frame times, especially during intense gameplay.

### Particle Optimizer
Reduces particle rendering when FPS drops below a threshold. Culls distant particles to improve performance in particle-heavy scenes.

## Compatibility

### Works With
Iris Shaders with automatic compatibility mode.
Sodium can be used alongside (some features auto-disable).
ModMenu with full integration with config screen.
Most content mods with no conflicts.

### Incompatible With
Titan Performance includes similar optimizations to these mods, so do not use them together:
EntityCulling (we have built-in entity culling)
Dynamic FPS (we have built-in dynamic FPS)

## Performance Tips

1. Lower culling distance for more aggressive optimization (Entities, Culling Distance slider)
2. Enable Aggressive Mode for maximum FPS (may cause slight pop-in)
3. Reduce unfocused FPS to save power when alt-tabbed
4. Use with Sodium for maximum performance (our rendering optimizations auto-disable)

## FAQ

Q: Does this work in multiplayer?
A: Yes. Titan Performance is fully multiplayer compatible. Entity culling works on servers.

Q: Will this cause visual glitches?
A: No. We use conservative culling thresholds to prevent pop-in. Enable Aggressive Mode only if you want maximum FPS.

Q: Does this work with shaders?
A: Yes. When Iris is detected, we automatically disable rendering optimizations that might conflict with shaders.

Q: How much FPS will I gain?
A: Depends on your hardware and scene. Typical improvements are 20 to 50 percent for low-end hardware, 10 to 30 percent for mid-range hardware, and 5 to 15 percent for high-end hardware.

## Support and Links

GitHub: https://github.com/Leone7715/titanperformance
Issues: https://github.com/Leone7715/titanperformance/issues
Discord: https://discord.gg/eFKaKdkCAw

## License

Titan Performance is licensed under LGPL-3.0-only. You are free to include it in modpacks.

Copyright 2024 LeonesModFactory
