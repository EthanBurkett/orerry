# @orrery/mod — Fabric Mod

Orrery in-game client mod. Kotlin + Fabric + Loom, targeting MC **1.21.11 only**.

## Pinned versions (verified 2026-06-17)

| Component | Version |
|---|---|
| Minecraft | 1.21.11 |
| Yarn mappings | 1.21.11+build.6 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.141.4+1.21.11 |
| Fabric Language Kotlin | 1.13.12+kotlin.2.4.0 |
| Fabric Loom | 1.17.11 |
| Gradle (wrapper) | 9.5.1 |
| Kotlin compiler | 2.4.0 |
| Java target | 21 (build/run with Java 25 Temurin) |

Sources: [meta.fabricmc.net](https://meta.fabricmc.net), [maven.fabricmc.net](https://maven.fabricmc.net)

## Build

```sh
# From apps/mod/
./gradlew build          # Unix / Git Bash
gradlew.bat build        # Windows CMD / PowerShell / Turbo
```

Output jar: `build/libs/orrery-<version>.jar`

## Run in-game (dev)

```sh
./gradlew runClient
```

Loom downloads the MC client jar + assets on first run (~800 MB). This opens a dev MC instance with the mod loaded.

## Turbo integration

Turbo calls `gradlew.bat build/test/lint/clean` via the `package.json` scripts.
`gradlew.bat` is used (not `./gradlew`) so the invocation works correctly from Turbo on Windows without shell-path issues.

## Structure

```
src/main/kotlin/gg/orrery/
  OrreryMod.kt          # ClientModInitializer entrypoint
  generated/Tokens.kt   # DO NOT EDIT — codegen output from @orrery/design-tokens
  eclipse/Eclipse.kt    # Menu override engine (stub — Phase 1)
  lumen/Lumen.kt        # UI toolkit (stub — Phase 2)
  atlas/Atlas.kt        # SkyBlock data model (stub — Phase 1)
  halo/Halo.kt          # HUD layer (stub — Phase 3)
  codex/Codex.kt        # Config system (stub — Phase 3)
  relay/Relay.kt        # Launcher loopback bridge (stub — Phase 3)
src/main/resources/
  fabric.mod.json       # Mod manifest (client entrypoint)
  orrery.mixins.json    # Mixin config (empty mixins array — Phase 1 adds mixins)
  assets/orrery/        # Fonts, textures, atlases (populated in Phase 2+)
```

## Compliance

This mod is **"safe by construction"** per DESIGN_SPEC §2. The §2 contract is
enforced at the Eclipse subsystem's single `clickSlot` chokepoint (§6.3, §11).
No packet manipulation, no automation, no ESP — ever.
