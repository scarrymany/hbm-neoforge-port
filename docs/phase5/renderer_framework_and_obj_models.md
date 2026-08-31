# Phase 5 research: Renderer framework & OBJ models

**Area:** `renderer_framework_and_obj_models` — CE's item/block renderer architecture end to end:
vanilla-shaped JSON models vs. custom Java renderers (TESR-era) vs. raw OBJ models; the real NeoForge
1.21.1 API shapes that replace each; which of this port's already-built block entities need a genuine
`BlockEntityRenderer` (BER) vs. a static baked model.

**Scope boundary (per task instructions, not duplicated here):** weapon/gun rendering (`ItemRenderBase`
subclasses under `com.hbm.render.item.weapon.*`, `ModelGun`/`ModelBullet`/`ModelGrenade`/animation
hooks), armor/`HumanoidModel` rendering (`ModelArmor*`, the `IClientItemExtensions`-based armor-model
hooks Phase 3 already wired), and boss/vehicle/mob/projectile entity rendering (`com.hbm.render.entity.*`
— `RenderBlackHole`, `RenderWormBody/Head`, `RenderMirv`, `RenderBoat`, plane/rocket entities) are all
covered by sibling Phase 5 research areas in this wave and are only referenced here to draw the
boundary, never analyzed in depth.

## Method

Read directly, with exact paths/line counts cited inline:
- `upstream/hbm-ce` (CE 1.12.2 source, the sole source of truth for behavior/asset names/visual intent)
- `upstream/neo-edition` (a parallel, partial, **already-compiling-against-the-same-`neo_version`**
  1.21.1 port — used *only* to confirm real NeoForge 1.21.1 client-rendering API shapes; every
  design/behavior/layout choice in this report comes from CE, never from neo-edition)
- This port's own `src/main/java` tree (grepped whole-tree for `BER`/`TESR`/`OBJ`/`BlockEntityRenderer`
  forward-reference comments, and for existing NeoForge client-model API usage)
- This port's own `docs/phase{0,1,2,3,4}/*.md` (grepped for "Phase 5" cross-references naming exact
  classes/mechanics that land in this area)

This port's `gradle.properties` pins `neo_version=21.1.228`; `upstream/neo-edition/gradle.properties`
pins the identical `neo_version=21.1.228` — so every neo-edition class cited below is confirmed against
the *exact* NeoForge build this port itself targets, not merely "some 1.21 version." This sandbox
cannot run `./gradlew` (network policy blocks `maven.neoforged.net`) and cannot launch a client, so no
claim here is a screenshot-verified visual confirmation — every API-shape claim is either (a) read
directly from neo-edition source at the cited line, or (b) explicitly flagged **"well-established
knowledge, NOT verified against a real jar."**

## Headline findings

1. **CE has three distinct rendering technologies, not two.** Beyond "vanilla JSON model" vs. "custom
   Java renderer," CE also ships a bespoke **Collada skeletal-animation system**
   (`com.hbm.animloader`, 6 files / 822 lines) used by blast-door TESRs specifically — a materially
   harder problem than the OBJ pipeline, already flagged Phase 5 by `docs/phase3/weapon_animation_hooks.md`,
   and this port has ported **none** of the underlying door blocks yet (see Blocked scope).
2. **This port's Phase 5 renderer work is 100% greenfield today**: exactly one file exists under any
   `com.hbm.render*` package in this port's `src/` (`render/misc/RenderScreenOverlay.java`, an HUD
   stub, not a block/item renderer — see Directory survey). Zero `BlockEntityRenderer`,
   `BlockEntityWithoutLevelRenderer`, or custom geometry-loader classes exist yet.
3. **CE ships 634 real `.obj` mesh files** (57 `.mtl`) under `assets/hbm/models/`, overwhelmingly
   block/machine-adjacent (this area) rather than weapon/vehicle-adjacent (sibling areas) — full
   breakdown below.
4. **The real NeoForge 1.21.1 API shape for every piece of this puzzle is independently confirmed**
   by neo-edition's own compiling code, at the exact same NeoForge version this port targets:
   `IGeometryLoader`/`IUnbakedGeometry`/`ModelEvent.RegisterGeometryLoaders` (static OBJ models),
   `BlockEntityRenderer`/`BlockEntityRendererProvider`/`BlockEntityRenderers.register` (dynamic
   in-world rendering), and `BlockEntityWithoutLevelRenderer` via `IClientItemExtensions.getCustomRenderer()`
   registered through `RegisterClientExtensionsEvent` (item-in-hand/GUI rendering — the modern
   replacement for CE's `TileEntityItemStackRenderer`).
5. **Neo-edition did *not* use NeoForge's built-in `obj` geometry loader.** It re-implemented CE's own
   `HFRWavefrontObject` text-format parser almost verbatim (431 lines vs. CE's 681) and wrote a thin
   custom `IGeometryLoader` wrapper around it. This is a deliberate, well-motivated choice (see
   "OBJ loading" section) that this port should also follow rather than gambling on the stock loader.
6. **A large, concrete slice of "safe to build now" work exists with zero new server-side plumbing**:
   this port's own already-committed block entities (Phases 0–4) already carry the exact live fields
   CE's renderers read every frame — e.g. `MachineLargeTurbineBlockEntity.rotor` (turbine rotation),
   `PylonBaseBlockEntity.connected`/`color`/`getMountPos()` (already networked every tick, per its own
   sync-packet comment) for the long-distance wire-mesh renderer. The renderer classes themselves are
   the only missing piece for several headline mechanics.

## Directory survey: what actually exists today

### CE (`upstream/hbm-ce/src/main/java/com/hbm/render/`)

| Package | File count | What it is |
|---|---|---|
| `tileentity/` | 194 (179 top-level `Render*.java` + 13 in `tileentity/door/` [12 door TESRs + 1 shared `IRenderDoors` interface] + `IItemRendererProvider.java`/`ItemRendererProviderRegistry.java` infra) | **TESR** (`TileEntitySpecialRenderer<T>`) classes — CE's per-tick, per-block-entity dynamic renderers. Whole-tree grep: 164 files `extends TileEntitySpecialRenderer` (163 of them inside `render/tileentity/` incl. `door/`, 1 elsewhere); 159 files whole-tree `implements IItemRendererProvider` (140 inside `render/tileentity/` incl. `door/`, the rest in `render/item`/`render/entity` for plain-item or entity-attached OBJ renders — see below). |
| `model/` | 112 | Mixed bag: ~36 are genuine custom `IBakedModel`/static-geometry classes for blocks/items (`*BakedModel.java` — `BlockDecoBakedModel`, `BlockScaffoldBakedModel`, `BlockCableBakedModel`, `RBMKColumnBakedModel`, `DuctNeoBakedModel`, `AbstractWavefrontBakedModel`, etc., this area's scope); the remaining ~76 are legacy `ModelBase`/`ModelBiped` classes for **armor/weapon/entity** models (`ModelArmor*`, `ModelGun`, `ModelBullet`, `ModelGrenade`, `ModelJetPack`, `ModelSword`, `ModelCrab`, `ModelWormHead`, …) — out of this area's scope per the task boundary. |
| `item/` | 20 | `ItemRenderBase` and weapon-specific item renderers (`com.hbm.render.item.weapon.*`) — out of scope (weapon rendering area), except `ItemRenderBase` itself as the shared abstract base CE's `IItemRendererProvider.getRenderer(Item)` returns. |
| `entity/` | 49 | Entity renderers (bosses, projectiles, clouds, the boat) — out of scope (entity/boss/vehicle rendering area). |
| `loader/` | 12 | `HFRWavefrontObject` (681 lines, the OBJ text parser), `HFRModelReloader`, `WaveFrontObjectVAO` — this area's scope. |
| `block/` | 4 | `BlockBakeFrame` (1.12 runtime model-baker, already flagged dead-weight by `docs/phase0/base_blocks.md`) + 3 `StateMapperBase` subclasses (`LayeredStateMapper`/`RotatableStateMapper`/`SimpleStateMapper`) — this whole package is superseded outright by real NeoForge blockstate-JSON datagen (this port's `ModBlockStateProvider`/`ICustomBlockModelRegister` already does this correctly; nothing here needs porting). |
| `com.hbm.animloader/` | 6 (822 lines) | Collada skeletal animation (`ColladaLoader`/`AnimatedModel`/`AnimationController`/`AnimationWrapper`/`Transform`/`Animation`) — consumed by `RenderDoorGeneric`, `RenderSlidingBlastDoorLegacy`, `RenderSiloHatch` (this area) and `ItemRenderJetpackGlider`/`WorldSpaceFPRender` (weapon area). Already named Phase 5 by `docs/phase3/weapon_animation_hooks.md:197-212`. |

### This port (`src/main/java/com/hbm/`)

- **Renderer code**: exactly **1 file** anywhere under a `render*` package —
  `src/main/java/com/hbm/render/misc/RenderScreenOverlay.java`, a narrow forward-reference stub for a
  HUD crosshair enum (not a block/item renderer; explicitly says so in its own javadoc, lines 4-19).
  **Zero** `BlockEntityRenderer`, `BlockEntityWithoutLevelRenderer`, or custom `IGeometryLoader` classes
  exist yet anywhere in this port.
- **Model-generation infra that already exists and is the correct integration point**:
  `com.hbm.blocks.ICustomBlockModelRegister` (`registerModel(BlockStateProvider, ResourceLocation)`,
  1 method) and `com.hbm.blocks.datagen.ModBlockStateProvider` (48 lines: iterates
  `ModBlocks.BLOCKS`, calls `custom.registerModel(...)` for anything implementing the interface, else
  falls back to `simpleCubeAllBlock`). Its own javadoc (lines 3-11) says it is "ported from the Neo
  Edition reference's `com.hbm.blocks.ICustomBlockModelRegister`" — i.e. this port already deliberately
  copied neo-edition's *hook shape*, confirming it's the right place for a future OBJ-model
  `CustomLoaderBuilder` to plug in. A parallel `com.hbm.items.ICustomItemModelRegister` +
  `ModItemModelProvider` (42 lines) exists for items.
- **Block entities already built (Phases 0-4) that are exactly this area's future BER targets**: 137
  files under `src/main/java/com/hbm/blockentity/` (`machine/`, `machine/rbmk/` [16 files],
  `machine/fusion/`, `machine/oil/`, `machine/chem/`, `network/`, `network/energy/`, `bomb/` [19 files
  including 11 nuke-casing/launch-pad BEs — see "Boundary note" below], `turret/`).

## The three CE render technologies, and their real NeoForge 1.21.1 replacements

### 1. Vanilla-shaped JSON models (the majority)

CE ships **1,085 block-model JSON files** and **2,999 item-model JSON files** under
`assets/hbm/models/block/` and `assets/hbm/models/item/` respectively (counted directly:
`find models/block -iname '*.json' | wc -l` → 1085, `find models/item -iname '*.json'` → 2999).
These are ordinary `cube_all`/`cube_column`/`item/generated`-parented JSON models — the CE-era
equivalent of what this port's `ModBlockStateProvider`'s `simpleCubeAllBlock` fallback and
`ModItemModelProvider` already generate correctly at datagen time. **No renderer-framework work is
needed for this bucket** — it is already the established, working default path in this port.

### 2. Static OBJ meshes baked once into a model (no per-frame Java logic)

CE parses a subset of its `.obj` files through `HFRWavefrontObject`
(`upstream/hbm-ce/src/main/java/com/hbm/render/loader/HFRWavefrontObject.java`, 681 lines — regex-based
Wavefront OBJ text parser, groups faces by `g`/`o` group name) and wraps the result in a custom
`net.minecraft.client.renderer.block.model.IBakedModel` implementation
(`com.hbm.render.model.*BakedModel` — `BlockDecoBakedModel`, `BlockScaffoldBakedModel`,
`BlockCableBakedModel`, `StaticMetaWavefrontBakedModel`, `RBMKColumnBakedModel`,
`DuctNeoBakedModel`, `AbstractWavefrontBakedModel`, ~36 classes total in `render/model/`), registered
at 1.12-era model-load time via CE's own `IModel`/`ICustomModelLoader` (1.12 Forge-only, dead — no
1.21 equivalent in this shape, confirmed already by `docs/phase0/base_blocks.md:108`).

**Real NeoForge 1.21.1 replacement, confirmed via neo-edition's own compiling code at the same
`neo_version`:**

- `net.neoforged.neoforge.client.model.geometry.IGeometryLoader<G>` — one method,
  `G read(JsonObject, JsonDeserializationContext)`
  (`upstream/neo-edition/src/main/java/com/hbm/render/model/loader/NtmGeometryLoader.java:11-23`).
- `net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry<G>` — one method,
  `BakedModel bake(IGeometryBakingContext, ModelBaker, Function<Material,TextureAtlasSprite>, ModelState, ItemOverrides)`
  (`.../render/model/loader/NtmGeometry.java:28-64`).
- Registered via `net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders`:
  `event.register(NtmGeometryLoader.ID, NtmGeometryLoader.INSTANCE)`
  (`.../main/NuclearTechModClient.java:206-208`, an `@SubscribeEvent`-annotated static method on the
  `@EventBusSubscriber(value = Dist.CLIENT)` client-setup class).
- Datagen side: `net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder<T>` — subclassed
  per geometry "flavor" (`AnvilLoaderBuilder`, `CableBlockLoaderBuilder`, `DetCordBlockLoaderBuilder`,
  `DuctBlockLoaderBuilder`, `BarbedWireBlockLoaderBuilder`, `SpikesLoaderBuilder`,
  `BarrelBlockModelBuilder` in `.../datagen/NtmBlockStateProvider.java:745-808`), each overriding
  `toJson(JsonObject)` to emit `"loader": "<id>"` + a `"type"` string the `IGeometryLoader` reads back
  (`object.get("type").getAsString()`), wired into `BlockStateProvider` calls like
  `this.models().getBuilder(path).customLoader(CableBlockLoaderBuilder::new).texture(...).end()`
  (`.../datagen/NtmBlockStateProvider.java:413,420,434-436,523-528,540,545`).

This is a complete, confirmed, end-to-end pattern this port can copy directly (hook shape, not
design/behavior — the actual mesh geometry and texture choices still come from CE).

**Important nuance neo-edition itself has not fully resolved (worth knowing before committing to a
plan)**: as of the code read, neo-edition's `NtmGeometryLoader`/`NtmGeometry` pipeline only covers **7
simple, single-mesh, no-named-part-animation shapes** (`BARBED_WIRE`, `SPIKES`, `BARREL`, `CABLE`,
`DET_CORD`, `PIPE`, `ANVIL` — see the `BakedModelType` enum,
`.../render/model/loader/NtmGeometry.java:30-38`). Every other OBJ-driven visual in neo-edition
(turbines, tanks, presses, RBMK, etc. — the ~180 TESR-equivalent mechanics) still goes through a
**separate, `@Deprecated`-annotated class**, `HFRWavefrontObjectVBO`
(`.../render/loader/HFRWavefrontObjectVBO.java`), which uploads each named OBJ group as its own
`com.mojang.blaze3d.vertex.VertexBuffer` at load time and exposes CE's *exact* original API —
`renderAll()`, `renderPart(String partName)`, `renderOnly(String... groupNames)`,
`renderAllExcept(String... excludedGroupNames)` (lines 105-141; compare to CE's identical method names
in `HFRWavefrontObject.java:200,261,287`) — backed by modern `Tesselator`/`BufferBuilder`/`VertexBuffer`
retained-mode calls instead of 1.12 immediate-mode `Tessellator`. `RenderZirnox.java:26` (neo-edition)
calls `ResourceManager.zirnox.renderAll()` on exactly this type. **Conclusion: the
`IGeometryLoader`/baked-model path is only a real fit for static, un-animated, single-piece OBJ blocks.
Anything needing per-frame named-part transforms (rotor blades, press head depth, RBMK lid segments)
needs the `HFRWavefrontObjectVBO`-style "parse once, upload named VBOs, draw named group from inside a
live `BlockEntityRenderer`" approach instead** — a real architectural fork this port will need to make
per-mechanic, not a single uniform pipeline. Recommend porting CE's own regex-based OBJ parser (as
neo-edition did, 431 lines) rather than attempting to shoehorn everything through the stock loader.

**Well-established, NOT verified against a real jar**: NeoForge also ships a *built-in* Wavefront OBJ
geometry loader (`net.neoforged.neoforge.client.model.obj.ObjLoader`, historically registered under an
id like `neoforge:obj`, descending from Forge's `forge:obj`). Neo-edition chose not to use it anywhere
in the code read — consistent with the named-group-animation requirement above, since the stock loader
is designed to bake one static mesh, not expose per-group VBO handles to a `BlockEntityRenderer`. This
port should make the same choice (custom loader for the few static shapes, hand-rolled parser+VBO
approach for animated ones) rather than spend time evaluating the stock loader's fitness.

### 3. Dynamic per-tick renderers (TESR → BlockEntityRenderer)

This is the bulk of this area's real work: **164 CE classes `extends TileEntitySpecialRenderer`**
whole-tree (194 files total in `render/tileentity/` once you count the `door/` subpackage and the two
shared infra classes — see Directory survey table). Confirmed real NeoForge 1.21.1 replacement shape,
again from neo-edition's own compiling code at the same `neo_version`:

- `net.minecraft.client.renderer.blockentity.BlockEntityRenderer<T>` — one method:
  `void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay)`
  (confirmed exact signature from neo-edition's own shared base class,
  `.../render/blockentity/BlockEntityRendererNT.java:15-16`).
- `net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T>` — `create(Context)`
  factory, implemented alongside `BlockEntityRenderer<T>` on the same class
  (`BlockEntityRendererNT.java:13`).
- Registered via the plain vanilla static registry call
  `net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(BlockEntityType<T>, BlockEntityRendererProvider<T>)`
  — **not** an event-based registration; called directly from inside `FMLClientSetupEvent.enqueueWork`
  (`.../main/NuclearTechModClient.java:144` calls `NuclearTechMod.proxy.registerBlockEntityRenderers()`,
  whose body is 60+ consecutive `BlockEntityRenderers.register(...)` calls,
  `.../main/ClientProxy.java:167-197+`).
- Neo-edition already has **47 files** under `render/blockentity/` implementing this shape (grep count),
  covering machines, nukes, and deco props — direct 1:1 architectural evidence for how this port's
  version should look, though (per the ground rule) none of neo-edition's actual visual output/behavior
  should be copied.

**Item-in-hand/GUI rendering parity (CE's `TileEntityItemStackRenderer`/`IItemRendererProvider`
mechanism)**: CE's TESR classes very often (**159 of 164 whole-tree**, i.e. the overwhelming majority) also
`implements IItemRendererProvider` — a CE-only glue interface (`getItemForRenderer()`/
`getItemsForRenderer()`/`getRenderer(Item): ItemRenderBase`) that lets the *same* OBJ-driven Java
renderer draw both (a) the placed-in-world block entity via TESR, and (b) the item in hand/inventory
via `item.setTileEntityItemStackRenderer(renderer)` — CE's own binding call,
`upstream/hbm-ce/src/main/java/com/hbm/main/client/NTMClientRegistry.java:93-107`.

The real NeoForge 1.21.1 replacement for item-in-hand/GUI rendering is
`net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer` (BEWLR), exposed through
`net.neoforged.neoforge.client.extensions.common.IClientItemExtensions#getCustomRenderer()`, itself
registered per-item via `net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent#registerItem(IClientItemExtensions, Item...)`.
Neo-edition already **ported forward CE's exact `IItemRendererProvider` design intent** under a new
name, `com.hbm.render.blockentity.IBEWLRProvider` (`getItemForRenderer()`/`getItemsForRenderer()`/
`getRenderer(): BlockEntityWithoutLevelRenderer`, 4 lines,
`.../render/blockentity/IBEWLRProvider.java`), and wires it up generically in
`ClientProxy.registerClientExtensions` (lines 74-78): it loops every registered BER provider and, if it
`instanceof IBEWLRProvider`, auto-registers the paired item renderer — i.e. one BER class implementing
one extra interface gets both behaviors, exactly mirroring CE's own one-class-two-jobs pattern. This
port should port this exact glue-interface *shape* (not neo-edition's actual per-mechanic bindings)
when it lands its own BER framework — it is a faithful, minimal 1:1 translation of CE's own design,
not an invented replacement.

**This port's own gap already flagged, matching this exactly**: `com.hbm.items.machine.ItemGear` and
`ItemBatteryPack` (Phase 1) both carry a javadoc noting "the CE `IMetaItemTesr` TESR-on-item render
binding is a client rendering detail out of this area's scope" — `IMetaItemTesr` is a sibling/variant
of `IItemRendererProvider` for plain items (not TE-backed) that also want an OBJ-mesh render in hand;
whoever builds the BEWLR framework here should also give it an item-only equivalent for these two
classes plus any other plain OBJ-modeled item.

## This port's own forward-reference comments (full grep result)

Every source-tree comment mentioning `BER`/`TESR`/`BlockEntityRenderer`/`.obj`/OBJ found by a
whole-tree grep, i.e. every place a previous phase already flagged work belonging to this area:

| File | What it says |
|---|---|
| `blocks/generic/DecoTapeRecorder.java:11-15` | CE's `TileEntityDecoTapeRecorder` tracked only a cosmetic reel-rotation counter for a TESR this port doesn't implement yet; TE dropped entirely (no consumer) rather than carried dead. |
| `blocks/generic/BlockLoot.java:26-31` | CE stores a per-item render offset for a bespoke static-multi-item baked model; dropped, plain default model stands in, "no confirmed NeoForge geometry path" (written before this research pass confirmed one exists — see above, now resolved). |
| `blocks/generic/BlockDecoModel.java:19-27` | Explicit: `models/blocks/puter.obj` baked via `HFRWavefrontObject`/`BlockDecoBakedModel`; registers with no `ICustomBlockModelRegister` override, default cube-all fallback stands in. |
| `blocks/generic/BlockDecoCRT.java:16-20` | Same OBJ/`BlockDecoBakedModel` gap, plain default model in the meantime. |
| `blocks/generic/HEVBattery.java:24-27` | Same OBJ gap. |
| `blocks/generic/BlockSkeletonHolder.java:26-29` | Same OBJ gap (`StaticMetaWavefrontBakedModel`), also notes "no held-item display" as a consequence. |
| `blocks/generic/BlockScaffold.java:14-21` | Same OBJ gap (`BlockScaffoldBakedModel`). |
| `blocks/network/energy/PylonBaseBlock.java:32-37` | CE's pylon block is invisible/non-solid, rendered as "an animated TESR wire mesh" (`EnumBlockRenderType.ENTITYBLOCK_ANIMATED`); "No wire renderer exists in this port yet (Phase 5 client work)"; empty collision shape stands in so players aren't blocked by an invisible hitbox in the meantime. **Confirmed this pass: the server-side data this wire renderer needs (`connected: List<BlockPos>`, `color: int`, `getMountPos(): Vec3[]`) already exists and is already networked every tick** (`PylonBaseBlockEntity.java:48-49,127,169-192`) — this is pure client rendering work with **zero remaining server-side blocker**. |
| `items/machine/ItemGear.java:6-8`, `items/machine/ItemBatteryPack.java:18-21` | CE's `IMetaItemTesr` TESR-on-item binding is "a client rendering detail out of this area's scope" — see BEWLR/item-render-parity note above. |
| `docs/phase2/machines_shredder_assembler_crystallizer_mixer.md:280-284` | Assembler's arm-animation state (`AssemblerArm[2]`, ring rotation, striker sound timing) is "purely cosmetic client-side flourish... belongs with whichever Phase 2 or Phase 5 package handles block-entity renderers (`BlockEntityRenderer`), since CE's version is tied to a custom OBJ-model rig." |
| `docs/phase2/blocks_network_conveyor_crane.md:140-145` | `BlockCraneBase`'s `CraneBakedModel`/`StateMapperBase`/`ExtendedBlockState`-unlisted-property rig is "1.12-era manual model-baking machinery with no direct NeoForge 1.21.1 equivalent... a datagen/blockstate-JSON + `BakedModel`/`RenderType` concern for whichever Phase 5 area owns custom block models." |
| `docs/phase1/blocks_generic.md:45` | `RebarFillRenderer` (fluid-filled chunk-mesh renderer, companion to `BlockRebar`) is "an obsolete rendering approach, needs a `BlockEntityRenderer` rewrite." **`BlockRebar` itself does not exist in this port yet** (confirmed this pass — grepped the whole tree for any `Rebar` block class; only `ItemRebarPlacer`/`CouplingToolItems` tool-item references exist) — this is blocked on the Phase-1 fluid-network TE group landing first, not purely a Phase 5 gap. |
| `docs/phase1/blocks_generic.md:149-154` | Full list of CE's custom-`.obj`-model blocks needing a NeoForge geometry-loader equivalent: `HEVBattery`, `BlockSkeletonHolder`, `BlockDecoModel`/`BlockDecoCRT`/`BlockDecoToaster`, `BlockScaffold`; plus `BlockReeds`/`BlockSandbags` (bespoke non-OBJ `IBakedModel`s, same "needs hand-authored NeoForge baked model" note). |
| `docs/phase3/weapon_animation_hooks.md:197-234` | Full Collada-animation deferral (see Headline finding #1). |

## Which block entities genuinely need a live BER vs. a static model

The dividing line, confirmed by reading representative CE TESR source directly (not inferred): **a
block entity needs a real per-frame `BlockEntityRenderer` if and only if its visual state changes
continuously from live TE data that a static baked/blockstate model cannot represent** (placement
facing and simple two-state on/off *can* be baked as ordinary blockstate variants — CE only used a TESR
for those cases because 1.12 required a TESR to render *any* OBJ mesh at all, a constraint that does
not carry over to 1.21's OBJ-via-baked-model path). Three concrete, directly-read examples, each
cross-checked against this port's already-built server-side BE fields:

| Mechanic | CE renderer (read in full) | Live per-frame read | This port's BE field already exists? |
|---|---|---|---|
| Turbine rotor spin | `RenderBigTurbine.java:29-59` — interpolates `turbine.lastRotor + (turbine.rotor - turbine.lastRotor) * partialTicks`, rotates only the `"Blades"` OBJ group (`ResourceManager.turbine.renderPart("Blades")`), body stays static. | Continuous rotation angle, unbounded | `MachineLargeTurbineBlockEntity.rotor` (float) exists and accumulates (`+ (shouldTurn ? 6F : 0F)) % 360F`, `.../blockentity/machine/MachineLargeTurbineBlockEntity.java:50,132`. **Gap**: no `lastRotor` field yet for partial-tick interpolation — a small, well-understood addition (either a synced previous-value field, or client-side `Mth.lerp` against two synced snapshots) needed when the BER is written. |
| Fluid tank contents | `RenderFluidTank.java:26-67` — picks the fluid-type texture and hazard-symbol overlay (`DiamondPronter.pront(...)`) from `tank.tank.getTankType()` read fresh every frame; swaps to a different OBJ ("exploded") variant if `tank.hasExploded`. | Fluid identity + explosion flag, both mutate at runtime | `FluidTankBlockEntity` already has a live `tank.getTankType()`-equivalent (`.../blockentity/machine/FluidTankBlockEntity.java:104` calls `tank.getTankType()` for its own subscribe logic) — the data this renderer needs already exists server-side. |
| Machine progress overlay | `RenderPress.java:29-56` — computes `f1 = press.progress * (1 - 0.125F) / TileEntityMachinePress.maxProgress` every frame and translates the press-head OBJ group down by `f1` (a literal physical "progress bar," not a 2D GUI one). | Continuous 0..1 progress fraction | 7 port BE classes already carry a `progress` field today (`LaunchpadSoyuzBlockEntity`, `MachineAssemblyMachineBlockEntity`, `MachineCrystallizerBlockEntity`, `MachineMixerBlockEntity`, `MachineReactorBreedingBlockEntity`, `MachineShredderBlockEntity`, `PWRControllerBlockEntity` — grepped directly), the same shape a press/compressor/etc. renderer would consume. |
| RBMK column height / rod state | `RenderRBMKLid.java:16-38` — loops `for (i = 0; i <= RBMKDials.getColumnHeight(world); i++)` stacking a repeated mesh segment, colors it from `te.rodColor`, gates a Cherenkov-glow effect on `te.fluxQuantity > 5`; `RenderRBMKControlRod.java` similarly reads `RBMKDials.getColumnHeight(world)` and live light data every frame. | Reactor column height is a **runtime server-config value** (`RBMKDials`), can never be baked once. | This port already has all 16 RBMK block-entity classes built (Phase 2, `.../blockentity/machine/rbmk/`), including `RBMKColumn.java`'s `ColumnType` enum and per-rod state — server side is ready, purely waiting on the renderer. |
| Long-distance pylon wire | `RenderPylonBase.java:19-90` — for each entry in `pyl.connected`, looks up the remote `TileEntityPylonBase`, reads both TEs' `getMountPos()` arrays, and tessellates a sagging half-line per pair every frame (1.12 `Tessellator`/`BufferBuilder` immediate mode). Endpoints are **arbitrary stored world positions**, not adjacent-block offsets — structurally impossible to bake into any static or blockstate-driven model. | Two arbitrary `BlockPos` endpoints + a color, read live | Already fully built and already networked (see forward-reference table above) — this is the cleanest "server done, purely a BER left" item in this entire report. |

**By contrast**, the large majority of CE's other ~159 TESR classes render a placement-facing-only OBJ
mesh with no other runtime-varying visual (most `machines/` and `block/` OBJ files: static multiblock
shells, decorative props, simple two-state machines). Those are candidates to skip a BER entirely in
this port and instead ship as a facing-driven blockstate model through the confirmed
`IGeometryLoader`/`CustomLoaderBuilder` datagen path above — CE only used a TESR for them because 1.12
had no other way to render an OBJ mesh at all. This is a **real simplification opportunity** relative
to CE's own architecture, not a shortcut that loses fidelity — the visual output is identical for a
block whose only variation is which way it's facing.

**Boundary note — nuke/bomb-casing block entities**: CE's `bombs/` OBJ folder (20 files:
`fatman.obj`, `lilboy.obj`, `gadget.obj`, `ivymike.obj`, `tsar.obj`, `prototype.obj`, `fleija.obj`,
`n2.obj`, `fstbmb.obj`, `ap_mine.obj`, etc.) and the corresponding CE TESRs
(`RenderNukeFstbmb`, `RenderCrashedBomb`, `RenderLandmine`, plus neo-edition's own confirmed
`RenderNukeFatMan`/`RenderNukeLittleBoy`/`RenderNukeGadget`/`RenderNukeIvyMike`/`RenderNukeTsarBomba`/
`RenderNukePrototype`/`RenderNukeFleija`/`RenderNukeSolinium`/`RenderNukeN2` — 12 BER classes total
under `render/blockentity/`) are, mechanically, ordinary block-entity renderers exactly like the
machine renderers above — they belong in this area's scope by mechanism. This port already has 19
matching block-entity classes under `blockentity/bomb/` from earlier phases (`NukeBoyBlockEntity`,
`NukeManBlockEntity`, `NukeGadgetBlockEntity`, `NukeMikeBlockEntity`, `NukeTsarBlockEntity`,
`NukePrototypeBlockEntity`, `NukeFleijaBlockEntity`, `NukeN2BlockEntity`, `NukeCasingBlockEntity`,
`NukeCustomBlockEntity`, `LaunchPadBlockEntity`/`LaunchPadLargeBlockEntity`/`LaunchPadRustedBlockEntity`,
`CrashedBombBlockEntity`, `LandmineBlockEntity`, `ChargeBlockEntity`, `NukeBalefireBlockEntity`, etc.).
Flagging rather than deep-diving per the task's own instruction — this content is thematically
"weapons & destruction," and whichever sibling Phase 5 research area covers nuke/explosion visuals
should own the actual per-nuke renderer design; this report only confirms the *mechanism* (BER, same
as everything else here) and that the server-side BE prerequisites already exist.

## OBJ asset inventory (634 `.obj` + 57 `.mtl`, full breakdown)

Counted directly under `upstream/hbm-ce/src/main/resources/assets/hbm/models/`:

| Directory | `.obj` count | Block/machine-adjacent (this area) or weapon/vehicle/armor-adjacent (sibling area)? |
|---|---|---|
| `machines/` | 102 | Block-adjacent (machines) |
| `block/` | 98 | Block-adjacent |
| `weapons/` (+ `weapons/animations/`) | 72 | **Weapon area** |
| `missile_parts/` | 45 | **Weapon/vehicle area** |
| `blocks/` | 37 | Block-adjacent (appears to be a newer/parallel naming convention to `block/` for a subset — e.g. `blocks/puter.obj` is the exact path this port's own `BlockDecoModel.java` javadoc cites) |
| `bombs/` | 20 | Block-adjacent by mechanism, weapon-themed content — see Boundary note above |
| `armor/` | 20 | **Armor area** |
| `launch_table/` | 15 | **Weapon/vehicle area** (missile launch infrastructure) |
| `rbmk/` | 14 | Block-adjacent (reactor) |
| `control_panel/` | 13 | Block-adjacent |
| `mobs/` | 12 | **Entity area** |
| `turrets/` | 11 | **Weapon area** |
| `pheodoors/` | 11 | Block-adjacent (doors — see Collada blocker above) |
| `doors/` | 11 | Block-adjacent (doors — see Collada blocker above) |
| `trinkets/` | 8 | Mixed/small; not investigated further (low file count, likely item-adjacent) |
| `network/` | 8 | Block-adjacent |
| `fusion/` | 8 | Block-adjacent (machines) |
| `projectiles/` | 7 | **Weapon area** |
| `particleaccelerator/` | 6 | Block-adjacent (machine) |
| `zirnox/` | 5 | Block-adjacent (reactor) |
| `vehicles/` | 5 | **Vehicle area** |
| `reactors/` | 5 | Block-adjacent |
| `pile/` | 3 | Block-adjacent |
| `lights/` | 3 | Block-adjacent |
| `effect/` | 3 | Likely **entity/VFX area** (not investigated further) |
| *(root, no subfolder)* | 92 | Mixed — sampled filenames include machine parts (`fluidtank.obj`, `centrifuge.obj`, `derrick.obj`, `press_head.obj`/`press_body.obj`, `keypad.obj`, `refinery.obj`, `soldering_station.obj`, `radgen.obj` → block-adjacent) alongside vehicle/weapon parts (`b29.obj`, `dornier.obj`, `boxcar.obj`, `duchessgambit.obj`, `soyuz*.obj`, `railgun_*.obj`, `mirv.obj`, `missile*.obj` → sibling areas) and misc test/utility meshes (`error.obj`, `testobj.obj`, `sphere*.obj`, `diffractionspikechecker.obj`). Not fully triaged file-by-file — flagging the mix rather than guessing at each one's consumer. |
| **Total** | **634** | **57 `.mtl` files also present** (CE's OBJ loader parses these for base-color hints, but `HFRWavefrontObject`/`HFRWavefrontObjectVBO` in both CE and neo-edition primarily drive texture selection through Java-side texture binding per render call, not through `.mtl`-driven material assignment — confirmed by every renderer example read above always calling `bindTexture(...)` explicitly rather than trusting `.mtl` data) |

`assets/hbm/models/item/` (2,999 files) and `assets/hbm/models/block/` (1,085 files) contain **zero**
`.obj` files between them — they are the vanilla-JSON bucket from technology #1 above, kept separate
from this table since they're not OBJ assets.

## Safe to build now (no external blocker)

1. **The `IGeometryLoader`/`IUnbakedGeometry`/`CustomLoaderBuilder` datagen pipeline itself** — the
   entire hook shape is confirmed against real, same-version NeoForge classes via neo-edition; nothing
   about it depends on any other unfinished port area. Recommend porting CE's own `HFRWavefrontObject`
   parser (following neo-edition's precedent, not the stock `neoforge:obj` loader) as the foundation.
2. **The `BlockEntityRenderer`/`BlockEntityRendererProvider`/`BlockEntityRenderers.register` framework**
   — same, fully confirmed, zero external blocker.
3. **The `IBEWLRProvider`-style glue interface** (BER class doubles as its own item renderer via
   `IClientItemExtensions.getCustomRenderer()`/`RegisterClientExtensionsEvent`) — confirmed shape,
   directly mirrors CE's own `IItemRendererProvider` design, no blocker.
4. **Pylon/substation long-distance wire BER** — server data (`connected`/`color`/`getMountPos()`)
   already exists and is already networked every tick; this is purely a client rendering task.
5. **Turbine rotor BER** (`MachineLargeTurbineBlockEntity`/`MachineTurbineBlockEntity`/
   `MachineTurbineGasBlockEntity`/`MachineIndustrialTurbineBlockEntity`/`TurbineBaseBlockEntity`) — the
   `rotor` field exists; only needs a small `lastRotor`-style interpolation addition alongside the BER.
6. **RBMK column/lid/control-rod BERs** — all 16 server-side RBMK block-entity classes already exist
   from Phase 2; purely a rendering task, though the column-height loop and Cherenkov effect are
   nontrivial (see table above) and should be written by someone reading CE's `RenderRBMKLid`/
   `RenderRBMKControlRod`/`RenderRBMKGraph` in full, not summarized further here.
7. **Fluid tank / progress-driven machine BERs** (press, compressor, the 7 BEs with a `progress`
   field, etc.) — server fields already exist; purely a rendering task per mechanic.
8. **The "static-facing-only OBJ block, no BER needed" simplification** identified above — for the
   large majority of CE's ~155 non-dynamic TESR-driven blocks, a plain facing-driven blockstate model
   via the geometry-loader path (item 1) is sufficient and simpler than porting a TESR that CE only
   needed because of a 1.12 API limitation.
9. **`BlockCable`'s connection rendering may not need OBJ/BER at all**: this port's own
   `BlockCable.java` (already built, Phase-1-or-later) already replaced CE's render-time
   `IExtendedBlockState`/unlisted-property connection mask with **6 ordinary listed `BooleanProperty`s**
   (`POS_X`/`NEG_X`/etc., confirmed by direct read of its javadoc and imports). Since connection state
   is already a real, listed blockstate property, a standard NeoForge **multipart blockstate JSON**
   (`"multipart": [{"when": {"pos_x": "true"}, "apply": {...}}, ...]`) can render per-direction wire
   geometry with **zero custom Java model code** — simpler than CE's own OBJ-baked approach and worth
   flagging as a deliberate simplification, not a fidelity loss, since the connection mask is already
   server-authoritative and synced via blockstate.

## Blocked / deferred (named blocker, not guessed)

1. **The 12 door TESRs (`RenderDoorGeneric`, `RenderSlidingBlastDoorLegacy`, `RenderSiloHatch`, +9 in
   `render/tileentity/door/`)** are double-blocked: (a) they depend on CE's Collada skeletal-animation
   system (`com.hbm.animloader`, 822 lines, GL-display-list based, **no confirmed NeoForge 1.21
   equivalent at all** — already named Phase 5 by `docs/phase3/weapon_animation_hooks.md`), and (b)
   **none of the underlying blast/vault/airlock/containment/sliding/secure/fire/water/cargo/seal door
   blocks exist in this port yet at all** (confirmed this pass: whole-tree grep for any block class
   named `*BlastDoor*`/`*VaultDoor*`/`*AirlockDoor*`/`*SlidingDoor*`/etc. returns zero matches; only
   plain vanilla-shaped `BlockModDoor`/`BlockNTMTrapdoor` exist). Owner: whoever ports the blast-door
   block family first (not previously assigned to any completed phase's STATUS.md — this is a genuine,
   newly-surfaced gap, not a re-statement of an existing one) **and**, separately, whoever picks a
   modern replacement for Collada skeletal animation (candidate per
   `docs/phase3/weapon_animation_hooks.md:212`: Sedna's own `AnimationLoader`, a custom JSON-keyframe
   system CE already uses elsewhere as a non-Collada alternative — a decision, not a research finding,
   for that owner to make).
2. **`BlockRebar`/`RebarFillRenderer`** — blocked on the Phase-1 fluid-network TE group landing first
   (`docs/phase1/blocks_generic.md:45`); the block itself doesn't exist in this port yet, so there is
   nothing to attach a renderer to.
3. **`CraneBakedModel`/`BlockCraneBase`'s connection-aware baked model** — blocked on
   `MultiblockHandlerXR` (Phase 2 multiblock framework, itself already a named gap per
   `docs/phase0/STATUS.md`) landing first; `docs/phase2/blocks_network_conveyor_crane.md:140-145`
   already named this exact dependency chain.
4. **Assembler arm-animation** (`docs/phase2/machines_shredder_assembler_crystallizer_mixer.md:280-284`)
   — purely cosmetic, zero gameplay effect, safe to implement whenever this area's BER framework
   lands; not blocked on anything else, just deferred by priority.
5. **Nuke/bomb-casing BERs** — mechanically unblocked (server BEs exist), but flagged as
   thematically belonging to a "weapons & destruction visuals" sibling area per the Boundary note
   above; whoever owns that area should confirm ownership rather than this report claiming it
   unilaterally.

## Key risks

- **The animated-vs-static OBJ fork is a real architectural decision, not just extra work.** Building
  everything through the simple `IGeometryLoader` path (like neo-edition's 7-shape `NtmGeometry`) will
  silently fail to animate anything (turbines, presses, RBMK). Building everything through the
  `HFRWavefrontObjectVBO`-style named-part-VBO path works for both cases but is more code per block.
  Whoever starts this area's implementation should decide this fork explicitly per-mechanic (static
  facing-only → geometry loader; anything with a live per-frame transform → BER + named-part VBOs) and
  document the choice, rather than defaulting to one approach for everything.
- **Partial-tick interpolation fields are not yet present** on several BEs whose CE renderer expects
  them (confirmed gap: `lastRotor` missing on `MachineLargeTurbineBlockEntity`). Whoever writes each
  BER needs to check for and add these small fields rather than assuming the existing BE is
  render-ready as-is.
- **Collada replacement is an open design question**, not merely unported code — no drop-in NeoForge
  equivalent exists, and the two CE-internal alternatives (port Collada loading as-is with a modern GL
  backend, vs. adopt Sedna's simpler custom JSON-keyframe system) have different fidelity/effort
  tradeoffs that this report does not resolve.
- **The stock NeoForge `obj` geometry loader's exact registration id and class location are
  well-established knowledge, not verified in this sandbox** (no NeoForge jar is cached locally, and
  `maven.neoforged.net` is network-blocked here) — low risk since this report recommends not using it
  anyway, but worth a real-jar check by whoever implements this area, in case its availability changes
  the static-shape recommendation.
- **The 92 root-level `.obj` files (no subfolder) and the 8-file `trinkets/`/3-file `effect/`
  directories were not fully triaged file-by-file** — flagged as a mix rather than guessed at
  individually; whoever picks up a specific mechanic should re-check its OBJ file's actual directory
  location rather than trusting this report's coarse grouping for edge cases.

## Open questions

1. Who owns the nuke/bomb-casing BERs (12 confirmed CE TESR classes, 19 already-built port BEs) — this
   area (mechanism match) or a "weapons & destruction visuals" sibling area (content match)? This
   report flags the ambiguity rather than resolving it.
2. Does this port want to port CE's Collada system at all, or standardize on Sedna's JSON-keyframe
   animation for every future need (doors included)? This blocks 12 door TESRs and 2 weapon-area
   renderers; a single project-wide decision would unblock all of them at once rather than
   per-mechanic.
3. Should the `IBEWLRProvider`-equivalent glue interface be named/shaped exactly like neo-edition's
   (a reasonable default, since it's a faithful CE translation), or does this port's existing
   `ICustomBlockModelRegister`/`ICustomItemModelRegister` naming convention suggest a different name
   for consistency with this port's own established interface-naming pattern?
