# Create Transmission Loss (Forge 1.20.1, KotlinForForge)

Adds a virtual stress consumer to each Create kinetic network so transmission infrastructure (shafts/cogs/gearboxes/belts) contributes configurable SU loss.

## Version targeting

- Minecraft: 1.20.1
- Forge: 47.2.0
- KotlinForForge: 4.11.0
- Create: **0.5.1.j** (hard pinned in `mods.toml`)

## Testing strategy

Yes — Forge provides a built-in headless framework via **GameTest**. This project now includes:

- Unit tests for deterministic math/cache behavior: `src/test/kotlin/...`
- Forge GameTests for mod runtime assertions: `src/main/kotlin/.../gametest/...`
- A `gameTestServer` run config with `forge.enabledGameTestNamespaces=transmissionloss`

Useful commands:

- `./gradlew clean build`
- `./gradlew runGameTestServer`
- In game: `/demo transloss`
- In game: `/transloss debug here`

## Current implementation notes

- Includes common config for coefficients and speed scaling modes (`NONE`, `LINEAR`, `QUADRATIC`).
- Adds cache primitives and dirty-marking hooks.
- Samples real Create network members at runtime via reflection and registry IDs.
- Adds in-world commands: `/transloss debug here`, `/transloss recalc here`, `/transloss profile`, `/demo transloss`.
- Belt counting strategy is defined as **pulley-to-pulley Manhattan span** for stability across Create 0.5.x internals.

## Next wiring steps

- Add broader block/tag coverage for decorative and edge-case transmission members.
- Append overlay line to Create goggles tooltip path.

## Gradle wrapper

This repository pins Gradle to **8.8** via `gradle/wrapper/gradle-wrapper.properties`.

> Note: `gradle-wrapper.jar` is intentionally not committed (binary artifacts are not supported in this environment), and is ignored by `.gitignore`.
> If needed locally/CI, regenerate it with:
>
> ```bash
> gradle wrapper --gradle-version 8.8
> ```
