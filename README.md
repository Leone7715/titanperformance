# Titan Performance

The all-in-one Minecraft performance optimization mod for Fabric.

Titan Performance combines 8 powerful optimization modules into a single unified system. Get the benefits of multiple performance mods in one integrated package with automatic hardware detection and configuration.

Developed by LeoneDev at LeonesModFactory

## Features

### 8 Optimization Modules

**Rendering Optimizer**
Intelligent chunk rendering with frustum culling and update scheduling.

**Entity Culler**
Skip rendering entities that cannot be seen.

**Entity Throttler**
Reduce tick frequency for distant and idle entities.

**Lighting Optimizer**
Batch and defer lighting calculations.

**Memory Optimizer**
Object pooling and allocation reduction.

**Particle Optimizer**
Cull distant particles and reduce particle lag.

**Smooth FPS**
Reduce stutters and frame time spikes.

**Dynamic FPS**
Limit frame rate when unfocused or in menus.

### In-Game Settings

Access Titan Performance settings through:
- Options menu with the "Titan Perf" button (top-left)
- Video Settings with the "Titan Perf" button
- Press P anytime to open settings

### Keybinds

P opens the settings menu.
F8 toggles Dynamic FPS.

### Auto Configuration

Detects your hardware and automatically configures optimal settings:
- CPU core count and threading
- Available memory
- Hardware tier (Low/Medium/High/Ultra)

### Iris Shader Compatibility

Works alongside Iris and Sodium. When detected:
- Rendering modules auto-disable since Sodium handles those
- All other optimizations stay active
- Get shaders and performance together

## Installation

1. Install Fabric Loader 0.16.0 or newer
2. Install Fabric API
3. Download Titan Performance and place in your mods folder
4. Launch the game

## Configuration

Titan Performance creates a configuration file at config/titan-performance.json on first launch.

### Module States

All modules are enabled by default:

```json
{
  "moduleStates": {
    "rendering_optimizer": true,
    "entity_culler": true,
    "entity_throttler": true,
    "lighting_optimizer": true,
    "memory_optimizer": true,
    "dynamic_fps": true,
    "particle_optimizer": true,
    "smooth_fps": true
  }
}
```

### Hardware Tiers

LOW: Older or budget hardware with aggressive optimization.
MEDIUM: Average gaming hardware with balanced optimization.
HIGH: Modern gaming hardware with quality focused settings.
ULTRA: High-end hardware with minimal optimization.

## Compatibility

### Works With
- Iris Shaders (auto-compatibility)
- Sodium (auto-compatibility)
- Most content mods
- Resource packs

### Conflicts With
- Lithium (entity and world tick overlap)
- Starlight (lighting engine overlap)
- Entity Culling (entity rendering overlap)
- FerriteCore (memory optimization overlap)
- Dynamic FPS (FPS limiting overlap)

## Performance Impact

Low-End Hardware: 50 to 100 percent FPS boost, 30 to 50 percent memory reduction.
Mid-Range Hardware: 30 to 60 percent FPS boost, 20 to 30 percent memory reduction.
High-End Hardware: 15 to 40 percent FPS boost, 10 to 20 percent memory reduction.

## Building

```bash
./gradlew build
```

Output JAR is in build/libs/

## Links

GitHub: https://github.com/Leone7715/titanperformance
Discord: https://discord.gg/eFKaKdkCAw

## License

LGPL-3.0-only

## Credits

Titan Performance draws inspiration from:
- Sodium by JellySquid
- Lithium by JellySquid
- Starlight by Spottedleaf
- Entity Culling by tr7zw
- FerriteCore by malte0811
- Dynamic FPS by juliand665

Copyright 2024 LeonesModFactory
