# Changelog

All notable changes to Titan Performance will be documented in this file.

The format is based on Keep a Changelog (https://keepachangelog.com/en/1.0.0/), and this project adheres to Semantic Versioning (https://semver.org/spec/v2.0.0.html).

## [1.0.0] Release Date 2026-01-11

### Added

#### Core Features

Entity Culling System with intelligent culling of entities based on distance and visibility. Includes distance-based culling with configurable threshold (16 to 128 blocks), behind-player culling using efficient dot product calculation, aggressive mode for decorative entities (armor stands, item frames), and never culls players or ridden entities.

Dynamic FPS Control with automatic frame rate limiting. Reduces FPS when window is unfocused (default 10 FPS), limits FPS in menus and pause screen (default 60 FPS), and is configurable via sliders in settings.

Memory Optimizer that reduces garbage collection stutters. Uses object pooling for frequently allocated objects, proactive GC during safe moments, and provides smoother frame times during intense gameplay.

Particle Optimizer with performance-aware particle rendering. Culls distant particles automatically, reduces particle count when FPS drops below threshold, and has configurable FPS threshold and reduction percentage.

Entity Throttler that reduces tick frequency for distant entities. Distant entities update less frequently, includes idle entity detection, and preserves gameplay for nearby entities.

Smooth FPS Module for frame time spike reduction. Detects frame time spikes, provides proactive garbage collection, and delivers smoother overall gameplay.

#### User Interface

Modern Settings Screen with three-panel red-themed interface. Includes category sidebar for easy navigation, options panel with toggles and sliders, and description panel with hover information. Features beautiful flat design with red accent color.

Quick Access with multiple ways to access settings. Press P to open settings anywhere, press F8 to toggle Dynamic FPS, button in Video Settings (top-left), button in Options menu (top-left), and Mod Menu integration.

#### Compatibility

Iris Shaders Support with automatic compatibility mode. Detects Iris/Sodium installation, disables conflicting rendering optimizations, and entity culling still works with shaders.

Hardware Auto-Configuration that detects your system capabilities. Includes LOW/MEDIUM/HIGH/ULTRA presets, automatic optimization based on CPU/RAM, and manual override available.

### Technical Details

Extremely lightweight entity culling at approximately 20ns per entity check. No allocations during render checks. Cached module references for minimal overhead. Squared distance calculations (no sqrt). Per-tick statistics instead of per-entity counters.

## Version Support

Mod Version 1.0.0 supports Minecraft 1.21.8 (Current), 1.21 through 1.21.7 (Available), and 1.20 through 1.20.6 (Available).

## Reporting Issues

Found a bug? Please report it on GitHub Issues at https://github.com/Leone7715/titanperformance/issues with Minecraft version, mod version, other mods installed, steps to reproduce, and crash log (if applicable).

## Developer Information

Developer: LeoneDev
Organisation: LeonesModFactory
GitHub: https://github.com/Leone7715/titanperformance
Discord: https://discord.gg/eFKaKdkCAw
