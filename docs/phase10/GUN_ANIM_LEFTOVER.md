# CE gun held/render leftover

Sedna bus engine, CE `ItemRender*`, and factory lambdas were already wired.
This wave ports the CE held/render pipeline: `builtin/entity` item models,
`RenderHandEvent` cancel + `setPerspectiveAndRender` viewmodel, CE
`ItemRenderFrames17` + `setupThirdPerson`/`setupInv`/`setupEntity` numbers.

No invented keyframes or hold poses.

## Now using CE FP viewmodel + TP pose

Every gun bound in `GunAnimationRegistration` (68): 3D OBJ via BEWLR, CE first-person
projection/sway/turn, CE third-person/GUI/ground frames.

## Still vanilla / no Sedna held renderer

- `gun_fireext` — CE legacy `ItemRenderFireExt` (`TEISRBase`). TODO(CE:ItemRenderFireExt.java:20)
- `gun_pa_ranged` — CE `ItemGunPA`, no dedicated Sedna FP renderer. TODO(CE:XFactoryPA.java:36)
- `gun_b92` — legacy `ItemRenderGunAnim`, not Sedna bus. TODO(CE:ClientProxy.java:323)
- Orphan CE JSON never referenced in CE Java either: `python.json`, `cursed.json`, `novac.json`, `ks23.json`, `supershotty.json`, `benelli.json`. TODO(CE:ResourceManager.java:537-543)

## True blockers (not invented around)

- ShaderHelper hand-depth / skip-hand: TODO(CE:ItemRenderWeaponBase.java:124)
- smokeNodes trail: TODO(CE:ItemGunBaseNT.java:329)
- CYCLE recoil callback: TODO(CE:GunAnimationPacketSedna.java:96-102)
- Trenchmaster reload speed / equip-progress reset: TODO(CE:GunAnimationPacketSedna.java:116-119)
- Spent-casing tint: TODO(CE:ItemRenderSPAS12.java:64-73) / TODO(CE:ItemRenderCongoLake.java:86)
- Tesla yomi plushie: TODO(CE:ItemRenderTeslaCannon.java:106) (`RenderPlushie` not ported)
- Stinger “Not accurate” font: TODO(CE:ItemRenderStinger.java:81-100)
- Folly/Bolter/Shredder/Quadro/MissileLauncher ADS FontRenderer overlays: 1.12 immediate-mode
- Aberrator golden-sword orbit quads: TODO(CE:ItemRenderAberrator.java:135)
- FatMan balefire glint: TODO(CE:ItemRenderFatMan.java:143) (`RenderMiscEffects`)
- NI4NI dye/coin NBT: TODO(CE:ItemGunNI4NI)
- PA melee IPAWeaponsProvider: TODO(CE:XFactoryPA.java:36) — only CE NCR-arm OBJ is drawn
- MAS36 clip-plane: TODO(CE:ItemRenderMAS36.java:49)
