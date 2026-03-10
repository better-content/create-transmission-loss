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

- `gradle test`
- `gradle runGameTestServer`

## Current implementation notes

- Includes common config for coefficients and speed scaling modes (`NONE`, `LINEAR`, `QUADRATIC`).
- Adds cache primitives and debounced dirty-marking hooks.
- Adds command tree stubs: `/transloss debug here`, `/transloss recalc here`, `/transloss profile`.
- Belt counting strategy is defined as **pulley-to-pulley Manhattan span** for stability across Create 0.5.x internals.

## Next wiring steps

- Replace placeholder mixin targets/methods with exact deobfuscated names for Create 0.5.1.j.
- Connect `NetworkScanner` to actual network member traversal and block/type discrimination.
- Append overlay line to Create goggles tooltip path.
