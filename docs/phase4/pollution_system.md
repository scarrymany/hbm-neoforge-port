# Pollution system (`PollutionHandler`) — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/handler/pollution/PollutionHandler.java` (396 lines — the
  entire package; there are no sibling classes, `PollutionData`/`PollutionPerWorld`/`PollutionType`
  are nested inside this one file)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityMachinePolluting.java` (105
  lines — the smoke-tank-buffered producer base class)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/fluid/trait/FT_Polluting.java` (104 lines — the
  per-fluid pollution-profile trait) and this port's own already-ported copy at
  `src/main/java/com/hbm/inventory/fluid/trait/FT_Polluting.java` (107 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityChimneyBase.java` (143 lines)
  + its two concrete subclasses `TileEntityChimneyBrick.java` (58 lines) and
  `TileEntityChimneyIndustrial.java` (63 lines) — the player-buildable pollution-filtering chimneys
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/ItemPollutionDetector.java` (41 lines) and this
  port's own stub at `src/main/java/com/hbm/items/tool/ItemPollutionDetector.java` (34 lines, already
  registered — see Headline finding)
- `upstream/hbm-ce/src/main/java/com/hbm/packet/PermaSyncHandler.java` (122 lines — the per-tick
  player-position pollution sync used for client smog rendering)
- `upstream/hbm-ce/src/main/java/com/hbm/saveddata/satellites/SatelliteMapper.java` (85 lines) and
  this port's own already-ported copy (read in full, ~90 lines, `CMD_GETSMOG` already a documented
  stub — see Headline finding)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityAshpit.java` (215 lines, read
  for the ash-byproduct side-mechanic chimneys feed into — confirmed out of this report's own scope,
  see Deferred scope)
- Partial reads (relevant methods/sections, not the whole file) of CE's `main/ModEventHandler.java`
  (mob-gear decoration around `LivingSpawnEvent`, lead-poisoning-on-block-break around
  `HarvestDropsEvent`), `main/ModEventHandlerRenderer.java` (client-side smog fog density/color,
  lines 1-140), `handler/EntityEffectHandler.java` (`handlePollution`, lines 572-607, out of ~759
  total), `blocks/generic/BlockGlyphidSpawner.java` (`TileEntityGlyphidSpawner.update`/`createSwarm`,
  lines 100-206), `entity/mob/glyphid/EntityGlyphid.java` (`useExtendedTargeting`, line ~329) and
  `EntityGlyphidScout.java` (rampant-guidance branch, lines ~75-105), `items/weapon/ItemAmmoArty.java`
  (chemical-warhead shell impacts, lines 270-368), `tileentity/machine/oil/TileEntityMachineRefinery.java`
  (its two pollution call sites, lines 150-190 and 300-325), `tileentity/machine/TileEntityCustomMachine.java`
  (JSON-recipe-driven `pollution()` method, lines 280-310), and `config/{RadiationConfig,MobConfig}.java`
  (field declarations + `loadFromConfig`, ~100 lines each)
- Repo-wide grep of CE for every `PollutionHandler.*` / `FT_Polluting.*` call site (~90 matches across
  ~35 files) to build the complete producer/consumer list below; files not read in full were grepped
  only for their one-line call and are named as such, not claimed as fully surveyed (blast furnace,
  fluid tank, drain, matter annihilator, gas flare, and the `CustomMachineRecipes`/`CustomMachine`
  Groovy-script JSON recipe plumbing)
- `upstream/neo-edition/src/main/java/com/hbm/handler/pollution/PollutionHandler.java` (342 lines,
  read in full) and `.../commands/PollutionCommand.java` (66 lines, read in full) — **cross-referenced
  for confirmed real NeoForge 1.21.1 API shape only**, per this task's ground rules; every behavioral
  number/formula below is sourced from CE, never from Neo Edition
- This port's own already-committed, already-compiling code, read to confirm real API shapes and
  real forward-reference contracts: `src/main/java/com/hbm/saveddata/{TomSaveData.java (full, 86
  lines — the `SavedData` pattern this report recommends), satellites/SatelliteSavedData.java
  (`computeIfAbsent` lines), satellites/Satellite.java (`setDirty()` call sites)}`,
  `src/main/java/com/hbm/handler/{ArmorDamageHandler.java (`@EventBusSubscriber` pattern),
  neutron/NeutronHandler.java (`ServerTickEvent.Pre` per-tick-simulation pattern), ArmorUtil.java}`,
  `src/main/java/com/hbm/util/ArmorRegistry.java` (`hasProtection(LivingEntity, EquipmentSlot,
  HazardClass)`, confirmed real signature), `src/main/java/com/hbm/config/{RadiationConfig.java,
  MobConfig.java}` (full `ModConfigSpec` field/getter surface), `src/main/java/com/hbm/blockentity/
  machine/{MachineCombustionEngineBlockEntity.java, oil/MachineRefineryBlockEntity.java,
  MachineTurbineGasBlockEntity.java, MachineDieselBlockEntity.java, FluidTankBlockEntity.java}`
  (javadoc + grep, confirming their documented pollution forward-references), `src/main/java/com/hbm/
  inventory/fluid/Fluids.java` (grep of all `FT_Polluting`/`PollutionHandler` constant usages, ~15
  fluid definitions), `docs/phase2/machines_power_generation.md` and `PORT_SPEC.md`/`HANDOFF.md`
  (grepped for prior framing)

## Headline finding

The task frames this as "confirm the storage/spread shape independently, don't assume it matches
chunk radiation" and treats the system as a clean, not-yet-touched green-field build. Both are true,
but two things the framing doesn't anticipate matter more for how this phase should actually proceed:

1. **This is a live, currently-unresolved compile break in the port today, not a purely forward-looking
   dependency.** `grep -rn "PollutionHandler" src/main/java` turns up **eight** already-committed port
   files, and two of them do not merely mention `PollutionHandler` in a comment — they `import` and
   directly call it: `src/main/java/com/hbm/inventory/fluid/Fluids.java` (constructs `FT_Polluting`
   trait instances against `PollutionHandler.SOOT_PER_SECOND`/`HEAVY_METAL_PER_SECOND`/
   `POISON_PER_SECOND` and `PollutionHandler.PollutionType.SOOT`/`HEAVYMETAL`/`POISON` at ~15 fluid
   definitions) and `src/main/java/com/hbm/inventory/fluid/trait/FT_Polluting.java` itself (calls
   `PollutionHandler.incrementPollution(Level, BlockPos, PollutionType, float)` from
   `onFluidRelease`/the static `pollute()` helper). **`com.hbm.handler.pollution.PollutionHandler` does
   not exist anywhere in this port's source tree** — confirmed by an exhaustive `grep -rn "class
   PollutionHandler"` returning zero results. Every other already-ported file that touches pollution
   (`MachineCombustionEngineBlockEntity`, `MachineRefineryBlockEntity`, `MachineTurbineGasBlockEntity`,
   `SatelliteMapper`, `ItemPollutionDetector`, `ToolItems`) was written *defensively* — each carries an
   explicit "stubbed pending `PollutionHandler`" javadoc/TODO and contains no reference the compiler
   would choke on. `Fluids.java`/`FT_Polluting.java` were not written defensively; they were ported as
   straight 1:1 translations of CE, on the (reasonable, since both are Phase-0/2-adjacent low-level
   data files with no obvious owner at the time) assumption that the class would exist by the time
   anyone needed to compile the tree. **This means this report's Package A is not optional
   infrastructure for later phases — it is required to make the existing `main` source set compile at
   all**, and should be sequenced accordingly (this sandbox cannot run `gradlew` to independently
   verify a full build failure, but an unresolvable import in a `.java` file is a guaranteed compile
   error in any Java toolchain — the grep evidence is conclusive on its own).
2. **Pollution is not a per-Minecraft-chunk system despite living next to "chunk radiation" in
   PORT_SPEC's phase description and despite reusing Minecraft's `ChunkPos` class.** CE's
   `incrementPollution`/`getPollutionData` compute their storage key as
   `new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6)` — **shift by 6, not 4**. This abuses `ChunkPos`
   purely for its `equals`/`hashCode`; the resulting "cell" is a 64×64-block square (4×4 real chunks),
   entirely decoupled from which real chunks are loaded, generated, or even exist yet. The whole
   per-dimension map (`HashMap<ChunkPos, PollutionData>`) lives in one `HashMap<World, PollutionPerWorld>`
   kept fully in memory for the dimension's entire lifetime, populated once on `WorldEvent.Load`,
   dumped to one flat NBT file (`hbmpollution.dat`, one per dimension's `data/` folder) on
   `WorldEvent.Save`, and dropped on `WorldEvent.Unload` — never touched at real per-chunk
   granularity, and its decay/spread pass (see below) runs over every stored cell regardless of
   whether the corresponding real chunks are currently loaded (only the separate "poison kills nearby
   plants" sweep checks `chunkExists`, and only so it doesn't call `getBlockState` on an unloaded
   chunk). This is architecturally closer to `TomSaveData`/`SatelliteSavedData` (a small, whole-map,
   per-dimension blob) than to whatever `ChunkRadiationManager`/`RadiationSystemNT` actually do — a
   quick check of `ChunkRadiationManager.java`'s own file confirms it is a thin proxy with no
   `ChunkPos`/bit-shift math of its own at all, i.e. it is a *materially different* storage shape, not
   a sibling implementation of the same pattern. Concretely: **PORT_SPEC.md's own two-bucket framing**
   ("Data Attachments on chunks/entities + `SavedData` for world-level systems") **already has the
   right bucket for this** — pollution is the "world-level system" case, not the "per-chunk state"
   case, and should be built as one `PollutionSavedData extends SavedData` per `ServerLevel`
   (`level.getDataStorage().computeIfAbsent(factory(), KEY)`), exactly matching this port's own
   already-committed, already-compiling `TomSaveData`/`SatelliteSavedData` precedent — **not** a
   `BlockCapability`/chunk-attached Data Attachment, and **not** Neo Edition's own choice (see Key
   design decisions) of hand-rolled `FileInputStream`/`FileOutputStream` I/O against a manually-resolved
   dimension folder path. The mechanism differs from CE/Neo Edition; the on-disk *behavior* (one blob
   per dimension, saved with the world, gone on unload) is unchanged.

## Suggested Phase 4 work-package split

This system is far smaller than the RBMK/gun-framework precedents (one 396-line core file, no deep
per-tick physics), so it does not need a 3-4-way package split — one package covers it:

### Package A — `PollutionHandler` core (do this first; unblocks the existing compile break)

The engine itself: `PollutionType` enum, `PollutionData`/`PollutionSavedData` storage, the
`increment`/`decrement`/`set`/`get`/`getPollutionData` static API (must match the exact signatures
`FT_Polluting.java` and `Fluids.java` already call — see Phase-4-safe scope), the 60-tick decay/spread
loop, the every-tick "poison kills nearby plants" world-destruction sweep, and load/save wiring. This
is the only piece that must land before the tree compiles again.

### Package B — Consumer retrofits (sequence after Package A; each is a small, independent edit to an
already-shipped file, not new content)

- `SatelliteMapper.onCommandImpl`'s `CMD_GETSMOG` branch (currently a hard-coded `this.tx = ""` stub
  with a TODO naming this exact system) — a 4-line change once Package A exists.
- `ItemPollutionDetector.inventoryTick` (currently an empty TODO body) — port the 4-value readout,
  using this port's already-established `Player#sendSystemMessage` pattern (confirmed by
  `ItemDosimeter`) instead of CE's `PlayerInformPacketLegacy` toast packet.
- `FT_Polluting`/`Fluids.java` need no code changes at all once Package A exists — they already call
  the exact contract; they are today's compile break, and Package A alone fixes them.

### Not part of this package (real dependents/producers that belong to other phases or other Phase 4
sub-areas — see Deferred scope for the full list with exact ownership)

`TileEntityMachinePolluting`, the chimney blocks, `TileEntityAshpit`, the furnace/refinery/turbine/
coker/wood-burner producer tile entities, the fluid-spill producer tile entities (blast furnace,
fluid tank, drain, annihilator, gas flare), `ItemAmmoArty`'s chemical shells, the JSON-driven Custom
Machine recipe hook, the Glyphid mob family (spawner, `EntityGlyphid`/`Scout`/`Digger`), the mob-gear
soot-based decoration hook, the lead-poisoning-on-ore-break hook, `EntityEffectHandler.handlePollution`
(poison/lead potion effects), and the client-side smog fog rendering. All of these are real CE call
sites into `PollutionHandler`, confirmed by reading them, but **none of them exist in this port yet**
(confirmed by exhaustive `find`/`grep` — see each item's exact ownership below) — building the engine
those absent files will eventually call into is this package's job; building the callers themselves is
not.

## Phase-4-safe scope (Package A detail)

All line counts are from CE files actually read in full or `wc -l`'d.

| Concern | CE shape (confirmed) | Recommended port shape | Confirmed real NeoForge 1.21.1 API |
|---|---|---|---|
| **`PollutionType` enum** | `SOOT`, `POISON`, `HEAVYMETAL`, `FALLOUT` (that exact ordinal order — the ordinal is used as the NBT/array/network index everywhere, must be preserved), each carrying a `name` field that is actually a translation key (`"trait.ptype.soot"` etc.) used only by `FT_Polluting.addInfoHidden`'s tooltip. **`FALLOUT` is dead weight in CE**: a repo-wide grep for `PollutionType.FALLOUT` finds exactly one hit, in `ItemPollutionDetector`'s printout — no producer ever increments it, and the decay/spread loop's `S`/`H`/`P` index variables never touch it either. It is declared, networked (generically, as part of the 4-length float array), and displayed, but always reads `0`. | Port the enum with the translation-key field intact (already-ported `FT_Polluting.java` references `entry.getKey().name` and expects it to resolve). Recommend replicating the dead `FALLOUT` slot exactly as CE has it (always-zero, no producer) rather than either wiring it to something new or removing it — CE itself never gave it behavior, and inventing behavior here would be adding content the source mod doesn't have. | N/A (plain enum) |
| **Storage key** | `new ChunkPos(pos.getX() >> 6, pos.getZ() >> 6)` — a 64-block coarse cell, **not** a real chunk (see Headline finding #2). | A small `record PollutionCellPos(int x, int z)` (or keep reusing `ChunkPos` as CE does, purely for its hashing) computed the same way, `>> 6` on both axes. | `net.minecraft.world.level.ChunkPos` still exists and is freely constructible from arbitrary ints in 1.21.1 if reuse is preferred (confirmed — Neo Edition's port does exactly this, `new ChunkPos(toChange)` via the `BlockPos`-taking constructor for the real lookup key, `new ChunkPos(x, z)` for neighbor cells). |
| **Per-cell data** | `PollutionData { float[4] pollution; }`, NBT round-trip via 4 named float tags (`soot`/`poison`/`heavymetal`/`fallout`, lower-cased enum names). | Same shape; straightforward `CompoundTag` read/write, one float per `PollutionType`. | N/A |
| **Per-dimension container + persistence** | `PollutionPerWorld { HashMap<ChunkPos,PollutionData> pollution; }`, one per `World`, held in a static `HashMap<World, PollutionPerWorld>`; serialized as an NBT list of `{chunkX, chunkZ, ...4 floats}` entries under one `"entries"` tag, written to a flat file `hbmpollution.dat` inside `<world save dir>/DIM<id>/data/` (CE's own dimension-folder-suffix logic, `WorldServer.getSaveHandler().getWorldDirectory()` + manual `DIM<n>` suffix check) via raw `FileInputStream`/`FileOutputStream` + `CompressedStreamTools`, on `WorldEvent.Load`/`Save`/`Unload`. | **Recommend departing from CE/Neo-Edition's raw-file I/O**: a `PollutionSavedData extends SavedData` per `ServerLevel`, exactly mirroring this port's own already-committed `TomSaveData`/`SatelliteSavedData` (`SavedData.Factory<PollutionSavedData>(ctor, load)`, `serverLevel.getDataStorage().computeIfAbsent(factory(), KEY)`, `load(CompoundTag, HolderLookup.Provider)`/`save(CompoundTag, HolderLookup.Provider)`). This is automatically one-per-dimension (no manual `DIM<n>` folder math needed at all — `DimensionDataStorage` already resolves the right per-dimension file), rides Minecraft's own save/autosave/backup path, and needs no `WorldEvent.Load`/`Unload` handling — `computeIfAbsent` lazily creates-or-loads on first touch per dimension per server run. **Must call `.setDirty()`** after every mutation (increment/decrement/set, and once per decay pass) — unlike CE's unconditional whole-file rewrite on every `WorldEvent.Save`, `SavedData` only persists when marked dirty; this is the one real behavioral seam a naive line-for-line port would miss. | `net.minecraft.world.level.saveddata.SavedData` + `SavedData.Factory<T>`, `ServerLevel#getDataStorage()`, `DimensionDataStorage#computeIfAbsent(Factory<T>, String)`, `SavedData#setDirty()` — all confirmed real by this port's own already-compiling `TomSaveData.java` (read in full) and `Satellite.java`'s `setDirty()` call sites, not merely by Neo Edition. |
| **Static read/write API** (the exact contract `FT_Polluting.java`/`Fluids.java` already call) | `incrementPollution(World, BlockPos, PollutionType, float)`, `decrementPollution(...)` (= increment with negated amount), `setPollution(...)`, `getPollution(World, BlockPos, PollutionType) → float` (0 if disabled/no data), `getPollutionData(World, BlockPos) → PollutionData` (**nullable** — `SatelliteMapper` and `ItemPollutionDetector` both null-check it). All five gate on `RadiationConfig.enablePollution` first (Neo Edition: `NtmConfig.COMMON.ENABLE_POLLUTION.get()`; this port: `RadiationConfig.ENABLE_POLLUTION.get()`, already a real `BooleanValue`). **`incrementPollution` multiplies the incoming `amount` by `MobConfig.pollutionMult` before adding** — applied to every `PollutionType`, not just soot, despite the config's own comment saying "for soot emitted" (a real CE naming/doc mismatch, not a port error to fix — replicate it). Each type's value is independently clamped to `[0, 10_000]` per cell (4 independent ceilings, not a combined one). | Signature must be `incrementPollution(Level, BlockPos, PollutionType, float)` etc. (`Level`/`BlockPos` already the 1.21.1 types both existing callers use) — **this exact signature is not a design choice, it is dictated by the two files that already call it**. Read `RadiationConfig.ENABLE_POLLUTION.get()` and `MobConfig.effectivePollutionMult()` (**not** the raw `POLLUTION_MULT` field — `effectivePollutionMult()` already folds in CE's Rampant-Mode `pollutionMult==1 ? 3 : pollutionMult` override as a derived getter, confirmed present in this port's own `MobConfig.java`, so PollutionHandler should call the derived getter, not reimplement that override itself). | Both config accessors already exist, confirmed by reading `MobConfig.java`/`RadiationConfig.java` in full. |
| **60-tick decay/diffusion pass** (`updateSystem`) | Runs once every 60 ticks (`eggTimer`), over every stored cell in every registered dimension: **SOOT** — if `> 10`, siphon `5%` of its value to each of the 4 orthogonal neighbor cells, then multiply the source by `0.8`; unconditionally multiply by `0.99` afterward (net multiplier while draining: `0.8 * 0.99 = 0.792` per pass, i.e. ~3-4 passes/~10-14s to visibly clear a spike). **HEAVYMETAL** — multiply by `0.9995` only; **it is never written into the neighbor-diffusion array at all** (the `pollutionForNeightbors[H]` slot is simply never assigned, staying `0f`) — heavy metal pollution **never spreads geographically in CE**, only decays in place. At `0.9995`/pass (60 ticks = 3s real-time), halving takes `ln(0.5)/ln(0.9995) ≈ 1387` passes ≈ **~69 real-world minutes** with no further input — heavy metal is designed as a near-permanent, spatially-fixed hazard, unlike soot/poison. **POISON** — if `>10`, siphon `2.5%` to neighbors then `×0.9`; else `×0.995`. Diffusion is applied to a *fresh* `newPollution` map built entirely from this pass (self's decayed remainder + all incoming neighbor contributions), then swapped in — i.e. it is not in-place mutation during iteration. | Replicate all three formulas and the "heavy metal never diffuses" asymmetry exactly — this is real, verified CE game-balance behavior, not an oversight to "fix." Call `.setDirty()` once per per-dimension pass if anything changed (mirroring CE's unconditional-rewrite cadence closely enough without persisting every single tick). | `ServerTickEvent.Post` (Neo Edition, confirmed real) or `ServerTickEvent.Pre` (this port's own precedent in `NeutronHandler.java`, confirmed real) — either fires once/tick; recommend `.Pre` only for this-port-internal consistency with `NeutronHandler`, no functional difference for a 60-tick-gated pass. |
| **Every-tick "poison kills nearby plants" sweep** (`handleWorldDestruction`) | Runs **every single server tick**, unconditionally (not gated by the 60-tick timer) for every stored cell whose `POISON > 15`: picks 5 random surface points inside that 64-block cell per tick, and if the topmost block is `GRASS`/plain `DIRT` converts it to `DIRT`; if it's `TALLGRASS`/`Material.LEAVES`/`Material.PLANTS` it is destroyed to air. Checks `chunkExists` before touching a block (the **only** place in the whole system that cares whether the real chunk is loaded). | Same 5-attempts/tick/cell budget and 15-poison threshold. **`Material.LEAVES`/`Material.PLANTS` have no 1.21.1 equivalent** (the `Material` system was removed after 1.13) — Neo Edition's port replaces this with `state.is(BlockTags.LEAVES)` (a real vanilla tag, safe to reuse) plus a **custom, Neo-Edition-invented** `NtmTags.Blocks.PLANTS` tag for the "any plant" half — that tag is Neo Edition's own content choice, not a confirmed vanilla or CE-parity source, so this port should define its own equivalent block-family check (either its own custom tag populated to match CE's `Material.PLANTS` membership — saplings, crops, flowers, tall grass, ferns, vines, etc. — or an `instanceof` check against the relevant vanilla block classes) rather than copy Neo Edition's tag verbatim. | `BlockState#is(TagKey<Block>)` / `is(Block)`, confirmed real by Neo Edition's port; `ServerLevel#hasChunk(int,int)` replaces `ChunkProviderServer.chunkExists`. |
| **Registration** | `MinecraftForge.EVENT_BUS.register(new PollutionHandler())` (instance-based, called once from `MainRegistry.preInit`-equivalent) — unconditional; `RadiationConfig.enablePollution` is checked inside each handler method, not at registration time, so toggling the config **after** a world has already loaded does not retroactively populate/depopulate the in-memory map (a world reload is needed) even though every read/write call re-checks the flag live. | This port's confirmed idiom is a static `@EventBusSubscriber(modid = MainRegistry.MODID)` class with `static` `@SubscribeEvent` methods (matches `ArmorDamageHandler`/`NeutronHandler`, and matches Neo Edition's own `PollutionHandler` exactly) rather than an instance registered imperatively — no explicit registration call needed anywhere. Preserve the "config only takes full effect on world (re)load" quirk rather than fixing it, unless the user-facing team decides otherwise (flagged in Open questions). | `net.neoforged.fml.common.EventBusSubscriber`, `net.neoforged.bus.api.SubscribeEvent`, both confirmed already in use in this exact port (`ArmorDamageHandler.java`, `NeutronHandler.java`). |
| **`PermaSyncHandler` slice** | CE syncs the *local* `PollutionData` (all 4 floats) for the block position under each connected player, every tick, as part of one larger multi-purpose sync packet (also carries Tom-impact data, "boykisser" potion-effect ids, satellite positions, and a riding-desync fix) — purely for the client to render smog fog (see below); it is not itself a gameplay effect. | Out of this report's own scope to redesign (no `PermaSyncHandler`-equivalent packet exists yet in this port), but the exact 4-float local-position payload this system needs to expose is worth naming for whichever phase builds the client sync: read `PollutionHandler.getPollutionData(level, player.blockPosition())` server-side, write 4 floats (or all-zero if `null`), every tick, per player. | N/A — payload/packet infrastructure is this port's already-confirmed `CustomPacketPayload`/`StreamCodec` pattern (see `docs/phase3/gun_framework.md`'s Key design decisions for the exact template), not re-derived here. |

## Deferred scope

Real CE call sites into `PollutionHandler` that this report's Package A must support the API contract
for, but that this report does **not** build, matching the "which package/phase" format the other
Phase reports use. None of the classes named below exist anywhere in this port yet (confirmed by
`find`/`grep` against `src/`) — Package A is buildable and independently valuable without them, since
its two live callers today are `Fluids.java`/`FT_Polluting.java`.

- **`TileEntityMachinePolluting`** (105 lines, read in full) and its 6 CE subclasses
  (`TileEntityFireboxBase`, `TileEntityDiFurnace`, `TileEntityFurnaceCombination`,
  `TileEntityMachineRotaryFurnace`, `TileEntityMachinePyroOven`, plus the already-documented
  combustion engine/diesel generator from `docs/phase2/machines_power_generation.md`) — these machines
  buffer their pollution byproduct in three internal `smoke`/`smoke_leaded`/`smoke_poison`
  `FluidTankNTM`s and only call `PollutionHandler.incrementPollution` on **tank overflow** (i.e. they
  are filterable: piping the smoke tanks into a chimney below/beside them drains the tanks before they
  overflow, so a correctly-plumbed setup with these machines never pollutes at all). This is a Phase 2
  "machines" concern (each of these tile entities' own retrofit is already tracked, for the two power-
  generation ones, in `docs/phase2/machines_power_generation.md`'s own deferred-scope section) — this
  report only needs to guarantee the `pollute(PollutionType, float)`/tank-overflow contract those
  files already describe is satisfiable once Package A exists.
- **`TileEntityChimneyBase`/`TileEntityChimneyBrick`/`TileEntityChimneyIndustrial`** (read in full,
  143+58+63 lines) — the player-buildable filters. Brick chimney passes through 25% of piped-in smoke
  as real pollution (`getPollutionMod() = 0.25`); Industrial chimney passes through only 10% and also
  captures the intercepted soot as physical "ash" fed into a `TileEntityAshpit` below it (a separate,
  Phase-2-owned item-byproduct machine, read in full at 215 lines to confirm it has no
  `PollutionHandler` dependency of its own — it only receives `ashLevelFly`/`ashLevelSoot` int
  counters from the chimney above it). Both chimneys' `getPollutionMod()` have a commented-out
  `MobConfig.rampantMode ? MobConfig.rampantSmokeStackOverride : ...` branch **that CE itself left
  disabled** (dead code, not a hidden feature to port) — the config field `rampantSmokeStackOverride`
  and `RadiationConfig.smokeStackSootMult` are consequently **both unused by any live code path in CE**
  (confirmed by grep: `smokeStackSootMult` has zero non-config references anywhere in the codebase).
  Phase 2's machine-block work owns building these; this report only needs `incrementPollution` to
  exist for their overflow path to call into.
- **The direct-`incrementPollution` producer family** (call `PollutionHandler` unconditionally every
  N ticks, with **no** smoke-tank buffering and therefore **not** filterable by any chimney — a real,
  confirmed CE asymmetry between this family and the `TileEntityMachinePolluting` family above, worth
  flagging so a future implementer doesn't assume all polluting machines are equally mitigable):
  `TileEntityFurnaceSteel`, `TileEntityFurnaceIron`, `TileEntityMachineElectricFurnace`,
  `TileEntityMachineArcFurnaceLarge` (10F flat, the single largest fixed per-event soot dump found in
  this survey), `TileEntityCrucible`, `TileEntityMachineTurbineGas` (imports `PollutionHandler`
  directly rather than through the `TileEntityMachinePolluting` base, per
  `docs/phase2/machines_power_generation.md`'s own note), `TileEntityMachineWoodBurner`,
  `TileEntityMachineCoker`, and `TileEntityMachineRefinery` (two independent call sites read in full:
  a `SOOT_PER_SECOND*5` passive-operation trickle, and a `SOOT_PER_SECOND*70` burst while its
  `onFire`/gas-flare-vent branch is active — the single largest sustained-rate producer surveyed).
  These belong with whichever Phase 2 (or later cleanup) package finishes each of those tile entities.
- **The fluid-spill/burn producer family** (`FT_Polluting.pollute(...)`/`onFluidRelease` called
  directly, grep-confirmed call sites only, not read in full):
  `TileEntityMachineBlastFurnace` (exhaust vent spill), `TileEntityMachineFluidTank` (overpressure
  spill/burn), `TileEntityMachineDrain` (manual spill), `TileEntityMachineAnnihilator` (matter-
  annihilation burn), `TileEntityMachineGasFlare` (flare spill/burn) — none of these tile entities
  exist in this port yet (confirmed: only a simplified `FluidTankBlockEntity` exists, and it has no
  pollution call at all, meaning `FT_Polluting.onFluidRelease`/the static `pollute()` helper are
  currently unreachable dead code in this port even after Package A lands — they only stop being dead
  once one of these five machines is ported). Phase 2 territory.
- **The JSON-driven "Custom Machine" recipe pollution hook** (`TileEntityCustomMachine.pollution(...)`,
  read in full, 280-310 — routes a recipe's string `pollutionType`/float `pollutionAmount` fields
  through `PollutionHandler.PollutionType.valueOf(...)` + `increment`/`decrementPollution`; the
  recipe-loading side lives in `CustomMachineRecipes.java`/the Groovy-script `CustomMachine.java`,
  grep-confirmed only). `TileEntityCustomMachine` does not exist in this port yet — this is the
  data-driven "moddable machine" system, its own Phase 2 (or later) research/implementation target.
- **`ItemAmmoArty`'s chemical-warfare shells** (read in full, lines 270-368) — `CHLORINE`
  (`+5 HEAVYMETAL` on impact), `PHOSGENE` (`+10 HEAVYMETAL`, `+15 POISON`), `MUSTARD` (`+15
  HEAVYMETAL`, `+30 POISON`) — the single largest one-shot pollution dumps found in this survey.
  `docs/phase3/guns_and_ammo.md` already read `ItemAmmoArty` in full and explicitly deferred it to
  "whichever Phase 3/4 sub-area owns artillery/turret block entities and vehicles" — not built in
  this port yet (confirmed). This report only needs `incrementPollution` to be callable once that
  area lands.
- **The Glyphid mob family** (`BlockGlyphidSpawner`/`TileEntityGlyphidSpawner`, `EntityGlyphid`,
  `EntityGlyphidScout`, `EntityGlyphidDigger`) — the other side of pollution's actual gameplay payoff:
  `createSwarm(soot, meta)`'s formula `swarmAmount = min(baseSwarmSize * max(swarmScalingMult *
  soot/sootStep, 1), 10)` and the per-species spawn-chance table (`adjustedChance = base + (modifier -
  modifier / max((soot+1)/3, 1))`, gated by each species' own minimum-soot threshold — both read in
  full from `BlockGlyphidSpawner.java`), `EntityGlyphid.useExtendedTargeting()` (soot ≥
  `MobConfig.targetingThreshold` unlocks a longer aggro range), and `EntityGlyphidScout`'s
  rampant-mode player-base-seeking waypoint AI keyed off `PollutionHandler.targetCoords` (a public
  static field **set from inside `PollutionHandler.java` itself**, by a `PlayerSleepInBedEvent`
  listener also living in that same file — see Key design decisions for the exact contract this
  implies). Confirmed not ported anywhere in this port (only a decorative `BlockGlyphid` block and an
  unrelated `BlockAllocatorGlyphidDig` explosion helper exist) — this is squarely Phase 4's own "custom
  entities" sub-area per `PORT_SPEC.md`'s phase description, a distinct research/implementation target
  from this report, not resolved here. **`PollutionHandler.targetCoords` and its two Glyphid-specific
  `@SubscribeEvent` methods (`rampantTargetSetter`/`rampantScoutPopulator`) should still be built as
  part of Package A**, even though nothing consumes them yet — they are physically part of
  `PollutionHandler.java` in CE (confirmed: Neo Edition's own port, which also has no Glyphid mobs
  yet, correctly dropped both methods from its 342-line translation for exactly this reason; this port
  should include them from the start since Glyphids are in scope for this same phase, just a later
  sub-area).
- **The mob-gear soot-based decoration hook** (`ModEventHandler.decorateMob`, read in full) — soot
  `> 2` gives a small chance (`0.5%`) of a fully-hazmat-suited "zombine," and feeds a soot-scaled
  "skelegun" replacement roll (`getSkelegun(soot, rand)`, not read in this survey — signature only)
  for skeleton bow slots. `ModEventHandler`'s equivalent does not exist yet in this port. Whichever
  Phase 1/3/4 sub-area ports mob equipment decoration owns this.
- **The lead-poisoning-on-ore-break hook** (`ModEventHandler`'s `HarvestDropsEvent` listener, read in
  full, lines ~1270-1310) — breaking a block in a `HEAVYMETAL`-polluted area applies `HbmPotion.lead`
  (amplifier scaling with pollution: `0`@5-10, `1`@10-25, `2`@25+) unless the player has
  `HazardClass.PARTICLE_FINE` head protection (`ArmorRegistry.hasProtection`, **already a real,
  confirmed API in this port** — `LivingEntity, EquipmentSlot, HazardClass`, read in full at
  `src/main/java/com/hbm/util/ArmorRegistry.java`). Blocked on `HbmPotion` (see next item), not on
  anything in this report.
- **`EntityEffectHandler.handlePollution`** (read in full, lines 572-607) — the ambient-exposure half:
  every 60 ticks, a player standing in `POISON > 10` gets vanilla `MobEffects.POISON`/`WITHER` (tiered
  by amount, gated by `RadiationConfig.enablePoison` + `HazardClass.GAS_BLISTERING` head protection);
  a player standing in `HEAVYMETAL > 25` gets `HbmPotion.lead` (tiered, gated by
  `RadiationConfig.enableLeadPoisoning` + `HazardClass.PARTICLE_FINE`). **`EntityEffectHandler.java`
  itself (759 lines total) does not exist in this port yet**, and is already partially claimed:
  `docs/phase3/fsb_armor_and_jetpacks.md` (read for cross-reference) already researched this exact
  file's shield-regen/dash-stamina chunk (lines outside 572-607) and confirmed this port's
  `HbmPlayerAttachment` already has the fields that chunk needs. This report claims the
  `handlePollution` chunk specifically as Phase 4 scope (it is squarely a pollution-consumer effect,
  not a shield/armor one) — whichever phase actually assembles the `EntityEffectHandler` class shell
  should pull both already-researched chunks in rather than have either phase re-derive the other's
  half.
- **`HbmPotion.lead`** (and, transitively, the whole `com.hbm.potion.HbmPotion` registration class) —
  referenced by both consumer hooks above but **not a cross-phase dependency**: `HbmPotion`'s
  registration is `PORT_SPEC.md`'s own "contamination effects (`potion` port)" line item, explicitly
  named as part of *this same* Phase 4, just a different sub-area. Confirmed not built yet (`find`
  for `HbmPotion.java` in this port returns nothing). This report only needs to name the exact 2
  effect names (`lead`, plus vanilla `MobEffects.POISON`/`WITHER`) and the exact amplifier tiers CE
  uses, not build the registration itself.
- **Client-side smog fog rendering** (`ModEventHandlerRenderer`'s `worldTick`/`thickenFog`/`tintFog`,
  read in full, lines 80-132) — a smoothed (`renderSoot`, `±0.05`/tick lerp toward the synced local
  soot value) fog-density (`FogDensity` event, `farPlaneDistance / (1 + soot*5/sootFogDivisor)`) and
  fog-color-tint (`FogColors` event, lerp toward a dark grey `0.15/0.15/0.15`) effect once local soot
  exceeds `RadiationConfig.sootFogThreshold` (already confirmed real:
  `RadiationConfig.sootFogThreshold()`, a derived getter already folding in the Rampant-Mode
  multiplier, present in this port's own `RadiationConfig.java`). This is Phase 5 "Client & UX" per
  `PORT_SPEC.md`'s own phase split (fog/rendering hooks, not world-simulation logic) — this report
  only needs the local-soot value to be queryable (already covered by Package A's `getPollution` API)
  and the config field to exist (already does).
- **`PermaSyncHandler`'s pollution slice specifically** — CE bundles it into one large multi-purpose
  per-tick sync packet alongside unrelated Tom-impact/satellite/riding-desync data; no equivalent
  packet exists in this port yet. Whichever phase builds the client HUD/fog sync should read
  `PollutionHandler.getPollutionData(level, player.blockPosition())` — this report names the exact call,
  not the packet.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own committed code and Neo
Edition's parallel pollution port for NeoForge API shape — no NeoForge API is invented below):

- **Use `SavedData`, not raw file I/O — this port already has the precedent, Neo Edition does not use
  it.** Neo Edition's `PollutionHandler` (read in full) is a near-1:1 behavioral translation of CE
  that also carries over CE's manual `FileInputStream`/`FileOutputStream` + `getServer().getWorldPath
  (LevelResource.ROOT)` + `DimensionType.getStorageFolder(...)` dimension-folder resolution — a real,
  confirmed-compiling NeoForge 1.21.1 shape, but not the idiomatic one, and not what this port's own
  two prior world-level-data systems (`TomSaveData`, `SatelliteSavedData`, both read in full) already
  chose. Recommend this port's `PollutionSavedData` follow those two, not Neo Edition, for the
  persistence *mechanism* (see Phase-4-safe-scope table) while keeping every *number* (the decay
  formulas, thresholds, multipliers) sourced from CE.
- **`@EventBusSubscriber(modid = MainRegistry.MODID)` with static methods, confirmed real and already
  this port's own idiom** (`ArmorDamageHandler.java`, `NeutronHandler.java`, both read/grepped) —
  matches Neo Edition's own `PollutionHandler` shape (`@EventBusSubscriber` + static
  `@SubscribeEvent` methods) almost exactly, just with this port's own convention of specifying
  `modid` explicitly. No instance-registration call (CE's `MinecraftForge.EVENT_BUS.register(new
  PollutionHandler())`) is needed.
- **`ServerTickEvent.Pre`/`.Post` and `LevelEvent.Load`/`Save`/`Unload` are both confirmed real**
  (Neo Edition uses `ServerTickEvent.Post` + all three `LevelEvent` variants; this port's
  `NeutronHandler` independently confirms `ServerTickEvent.Pre` for an analogous per-tick world
  simulation) — but if `PollutionSavedData` is adopted per the recommendation above, **no
  `LevelEvent.Load`/`Save`/`Unload` handling is needed at all**: `computeIfAbsent` lazily creates the
  per-dimension instance on first access (e.g. the first `incrementPollution` call, or the first tick
  pass touching that dimension), and NeoForge's own `DimensionDataStorage` handles save-on-world-save
  and eviction-on-unload transparently. Only the tick-driven decay/destruction loop needs a
  `ServerTickEvent` subscriber.
- **`ArmorRegistry.hasProtection(LivingEntity, EquipmentSlot, HazardClass)` is already real and
  already correctly ported**, confirmed by reading `src/main/java/com/hbm/util/ArmorRegistry.java` in
  full — this is the exact call CE's `handlePollution`/`HarvestDropsEvent` lead-poisoning hooks route
  through (`ArmorRegistry.hasProtection(entity, EntityEquipmentSlot.HEAD,
  ArmorRegistry.HazardClass.GAS_BLISTERING/PARTICLE_FINE)` in CE), so whichever phase eventually wires
  those two consumer hooks has zero new armor-protection API to build — it already exists, ready to
  call.
- **`MobConfig.effectivePollutionMult()`/`RadiationConfig.sootFogThreshold()` are already real derived
  getters that fold in CE's Rampant-Mode config-mutation side effects** (confirmed by reading both
  files in full) — `PollutionHandler`'s own `incrementPollution` should call
  `MobConfig.effectivePollutionMult()`, not a raw config field, to inherit that behavior for free
  rather than re-implementing CE's `if(pollutionMult==1) pollutionMult=3` load-time mutation.
- **No `com.hbm.config.WorldConfig`/`GeneralConfig` field is pollution-related** — confirmed by
  grepping both files for "pollution"/"soot" and finding zero matches; every pollution-adjacent config
  field lives in `RadiationConfig` (`CATEGORY_POLLUTION`: `enablePollution`, `enableLeadFromBlocks`,
  `enableLeadPoisoning`, `enableSootFog`, `enablePoison`, `buffMobThreshold` [confirmed **dead** — see
  below], `sootFogThreshold`, `sootFogDivisor`, `smokeStackSootMult` [confirmed **dead**, see Deferred
  scope]) and `MobConfig` (the Glyphid/Rampant-Mode soot fields). All of these already exist,
  correctly named and typed, in this port's already-ported `RadiationConfig.java`/`MobConfig.java`.
- **`RadiationConfig.buffMobThreshold` is dead code in CE** — declared, config-loaded (comment: "The
  amount of soot required to buff naturally spawning mobs"), and never read anywhere else in the
  codebase (confirmed by an exhaustive case-insensitive grep for `buffMob`). No mob-buffing-by-soot
  mechanic exists in CE despite the field's presence and doc comment. Recommend leaving it unused in
  the port too, exactly matching CE, rather than inventing the buff mechanic the comment implies but
  the code never delivers.

## Open questions / risks

- **Should the port fix the "config toggle needs a world reload to take full effect" quirk, or
  preserve it?** CE's `perWorld` map is only populated at `WorldEvent.Load` time when
  `enablePollution` was true; flipping the config mid-session doesn't retroactively add/remove the
  dimension's entry even though every read/write call re-checks the live flag. A `SavedData`-based
  redesign (per this report's recommendation) naturally changes this: `computeIfAbsent` would create
  the instance lazily on first *use* rather than at world-load time, meaning toggling the config back
  on mid-session would start working again as soon as anything calls `incrementPollution`, whereas CE
  never does. This is a small, probably-desirable behavior improvement that falls out of the
  recommended persistence mechanism rather than a deliberate design choice — worth flagging to
  whichever human/agent reviews the implementation so it's a known, intentional deviation rather than
  an unnoticed one.
- **Exact `Material.LEAVES`/`Material.PLANTS` replacement for the world-destruction sweep** — flagged
  in the Phase-4-safe-scope table: Neo Edition invented its own `NtmTags.Blocks.PLANTS` tag rather
  than finding a confirmed vanilla or CE-sourced equivalent. This report did not attempt to enumerate
  CE 1.12.2's actual `Material.PLANTS` block membership (it's a per-block property in a removed API,
  not something groppable from a single source), so the exact block list a faithful replacement tag
  should include is not nailed down here — recommend whoever implements Package A cross-check a
  handful of CE's own `Material.PLANTS`-tagged blocks (crops, flowers, saplings, vines, tall grass,
  mushrooms — the conventional 1.12-era membership) against 1.21.1's vanilla tags/block classes rather
  than assume Neo Edition's invented tag is complete or correct.
- **`FALLOUT` pollution type: replicate as permanently dead, or take the opportunity to wire it up?**
  This report recommends parity (leave it always-zero, exactly as CE does) since inventing behavior
  CE itself never shipped would be adding content, not porting it — but this is a judgment call worth
  surfacing explicitly rather than silently deciding, since a player-facing "Pollution Detector" tool
  printing a permanently-zero "Fallout: 0.0" line reads as a bug even though it's faithful to CE.
- **Compile-break urgency**: this report's Headline finding #1 (the live `Fluids.java`/
  `FT_Polluting.java` import break) means Package A is not merely "next in the phase queue" — if any
  other Phase 4 (or later) work needs to compile the `com.hbm.inventory.fluid` package at all, Package
  A is a hard prerequisite, not a nice-to-have. Recommend sequencing it first within Phase 4 rather
  than after e.g. chunk radiation, even though "radiation" is listed first in `PORT_SPEC.md`'s Phase 4
  description.
- **This sandbox could not run `gradlew`** to confirm the exact current compile-error text or rule out
  some other file silently already stubbing around the missing import (e.g. a build-exclusion). The
  `grep`-based evidence (a real `import com.hbm.handler.pollution.PollutionHandler;` statement in two
  committed, non-test `.java` files, with no such class anywhere in the tree) is about as conclusive
  as static analysis gets, but an actual build log would be worth capturing once this phase's
  implementation wave has network/toolchain access, to make sure no other latent surprise is hiding
  behind this one.
