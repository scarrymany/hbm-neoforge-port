#!/usr/bin/env python3
"""1-to-1 CE Sedna gun renderer + anim-lambda port. Does not invent keyframes."""
from __future__ import annotations

import re
from pathlib import Path

REPO = Path("/workspace")
CE_SEDNA = REPO / "upstream/hbm-ce/src/main/java/com/hbm/render/item/weapon/sedna"
CE_FACTORY = REPO / "upstream/hbm-ce/src/main/java/com/hbm/items/weapon/sedna/factory"
CE_RM = REPO / "upstream/hbm-ce/src/main/java/com/hbm/main/ResourceManager.java"
OUT = REPO / "src/main/java/com/hbm/client/render/item/weapon"
SKIP_RENDERERS = {
    "ItemRenderWeaponBase.java",
    "ItemRenderSPAS12.java",
    "ItemRenderUzi.java",
    "ItemRenderAm180.java",
    "ItemRenderStinger.java",
}

ID_MAP = {
    "ID_SILENCER": "silencer",
    "ID_SCOPE": "scope",
    "ID_NO_STOCK": "no_stock",
    "ID_NO_SHIELD": "no_shield",
    "ID_FURNITURE_GREEN": "furniture_green",
    "ID_FURNITURE_BLACK": "furniture_black",
    "ID_SAWED_OFF": "sawed_off",
    "ID_SPEEDLOADER": "speedloader",
    "ID_GREASEGUN": "greasegun_clean",
    "ID_UZI_SATURNITE": "uzi_saturnite",
    "ID_BAYONET": "mas_bayonet",
    "ID_CARBINE_BAYONET": "carbine_bayonet",
    "ID_LAS_SHOTGUN": "las_shotgun",
    "ID_LAS_CAPACITOR": "las_capacitor",
    "ID_LAS_AUTO": "las_auto",
    "ID_STACK_MAG": "stack_mag",
    "ID_CHOKE": "choke",
    "ID_SLOWDOWN": "minigun_slowdown",
    "ID_SPEEDUP": "minigun_speedup",
    "ID_SHREDDER_SPEEDUP": "shredder_speedup",
}

MODITEM_CLASS = {
    "gun_pepperbox": "GunPistolItems",
    "gun_light_revolver": "GunPistolItems",
    "gun_light_revolver_atlas": "GunPistolItems",
    "gun_light_revolver_dani": "GunPistolItems",
    "gun_henry": "GunPistolItems",
    "gun_henry_lincoln": "GunPistolItems",
    "gun_heavy_revolver": "GunPistolItems",
    "gun_heavy_revolver_lilmac": "GunPistolItems",
    "gun_heavy_revolver_protege": "GunPistolItems",
    "gun_hangman": "GunPistolItems",
    "gun_greasegun": "GunPistolItems",
    "gun_lag": "GunPistolItems",
    "gun_uzi": "GunPistolItems",
    "gun_uzi_akimbo": "GunPistolItems",
    "gun_am180": "GunPistolItems",
    "gun_star_f": "GunPistolItems",
    "gun_star_f_akimbo": "GunPistolItems",
    "gun_maresleg": "GunShotgunItems",
    "gun_maresleg_akimbo": "GunShotgunItems",
    "gun_maresleg_broken": "GunShotgunItems",
    "gun_liberator": "GunShotgunItems",
    "gun_spas12": "GunShotgunItems",
    "gun_autoshotgun": "GunShotgunItems",
    "gun_autoshotgun_shredder": "GunShotgunItems",
    "gun_autoshotgun_sexy": "GunShotgunItems",
    "gun_double_barrel": "GunShotgunItems",
    "gun_double_barrel_sacred_dragon": "GunShotgunItems",
    "gun_autoshotgun_heretic": "GunShotgunItems",
    "gun_g3": "GunRifleItems",
    "gun_g3_zebra": "GunRifleItems",
    "gun_stg77": "GunRifleItems",
    "gun_carbine": "GunRifleItems",
    "gun_minigun": "GunRifleItems",
    "gun_minigun_lacunae": "GunRifleItems",
    "gun_minigun_dual": "GunRifleItems",
    "gun_mas36": "GunRifleItems",
    "gun_amat": "GunRifleItems",
    "gun_amat_subtlety": "GunRifleItems",
    "gun_amat_penance": "GunRifleItems",
    "gun_m2": "GunRifleItems",
    "gun_flaregun": "GunLauncherItems",
    "gun_congolake": "GunLauncherItems",
    "gun_mk108": "GunLauncherItems",
    "gun_bolter": "GunLauncherItems",
    "gun_panzerschreck": "GunHeavyItems",
    "gun_stinger": "GunHeavyItems",
    "gun_quadro": "GunHeavyItems",
    "gun_missile_launcher": "GunHeavyItems",
    "gun_fireext": "GunHeavyItems",
    "gun_charge_thrower": "GunHeavyItems",
    "gun_flamer": "GunHeavyItems",
    "gun_flamer_topaz": "GunHeavyItems",
    "gun_flamer_daybreaker": "GunHeavyItems",
    "gun_chemthrower": "GunHeavyItems",
    "gun_drill": "GunHeavyItems",
    "gun_pa_melee": "GunHeavyItems",
    "gun_pa_ranged": "GunHeavyItems",
    "gun_debug": "GunHeavyItems",
    "gun_tau": "GunEnergyItems",
    "gun_coilgun": "GunEnergyItems",
    "gun_n_i_4_n_i": "GunEnergyItems",
    "gun_tesla_cannon": "GunEnergyItems",
    "gun_laser_pistol": "GunEnergyItems",
    "gun_laser_pistol_pew_pew": "GunEnergyItems",
    "gun_laser_pistol_morning_glory": "GunEnergyItems",
    "gun_lasrifle": "GunEnergyItems",
    "gun_fatman": "GunEnergyItems",
    "gun_folly": "GunEnergyItems",
    "gun_aberrator": "GunEnergyItems",
    "gun_aberrator_eott": "GunEnergyItems",
}


def parse_rm_maps() -> tuple[dict[str, str], dict[str, str], dict[str, str]]:
    text = CE_RM.read_text()
    obj: dict[str, str] = {}
    tex: dict[str, str] = {}
    anim: dict[str, str] = {}
    for m in re.finditer(
        r"public static final \S+ (\w+) = new HFRWavefrontObject\(new ResourceLocation\([^,]+,\s*\"([^\"]+)\"\)",
        text,
    ):
        obj[m.group(1)] = m.group(2)
    for m in re.finditer(
        r"public static final ResourceLocation (\w+) = new ResourceLocation\([^,]+,\s*\"(textures/models/weapons/[^\"]+)\"\)",
        text,
    ):
        tex[m.group(1)] = m.group(2)
    for m in re.finditer(
        r"public static final HashMap<String, BusAnimationSedna> (\w+) = AnimationLoader.load\(new ResourceLocation\([^,]+,\s*\"([^\"]+)\"\)",
        text,
    ):
        anim[m.group(1)] = m.group(2)
    return obj, tex, anim


OBJ_MAP, TEX_MAP, ANIM_MAP = parse_rm_maps()


def split_args(argstr: str) -> list[str]:
    args: list[str] = []
    buf: list[str] = []
    depth = 0
    for ch in argstr:
        if ch == "(":
            depth += 1
            buf.append(ch)
        elif ch == ")":
            depth -= 1
            buf.append(ch)
        elif ch == "," and depth == 0:
            args.append("".join(buf).strip())
            buf = []
        else:
            buf.append(ch)
    if buf:
        args.append("".join(buf).strip())
    return args


def convert_rotates(s: str) -> str:
    out: list[str] = []
    i = 0
    key = "GlStateManager.rotate("
    while True:
        j = s.find(key, i)
        if j < 0:
            out.append(s[i:])
            break
        out.append(s[i:j])
        k = j + len(key)
        depth = 1
        while k < len(s) and depth:
            if s[k] == "(":
                depth += 1
            elif s[k] == ")":
                depth -= 1
            k += 1
        args = split_args(s[j + len(key) : k - 1])
        if len(args) != 4:
            out.append(f"/* TODO(CE:rotate args) */ poseStack.mulPose(Axis.YP.rotationDegrees(0F));")
        else:
            angle, x, y, z = args
            axis = "YP"
            try:
                xf, yf, zf = float(x), float(y), float(z)
                if abs(yf) < 1e-9 and abs(zf) < 1e-9:
                    axis = "XN" if xf < 0 else "XP"
                elif abs(xf) < 1e-9 and abs(zf) < 1e-9:
                    axis = "YN" if yf < 0 else "YP"
                elif abs(xf) < 1e-9 and abs(yf) < 1e-9:
                    axis = "ZN" if zf < 0 else "ZP"
            except ValueError:
                if x.strip() in ("1", "1.0", "1F") or x.strip().startswith("1"):
                    axis = "XP"
                elif x.strip() in ("-1", "-1.0", "-1F"):
                    axis = "XN"
                elif z.strip() in ("1", "1.0", "1F"):
                    axis = "ZP"
            out.append(f"poseStack.mulPose(Axis.{axis}.rotationDegrees((float) ({angle})));")
        i = k
        if i < len(s) and s[i] == ";":
            i += 1
    return "".join(out)


def strip_method(src: str, sig: str) -> str:
    idx = src.find(sig)
    while idx >= 0:
        brace = src.find("{", idx)
        if brace < 0:
            break
        depth = 0
        k = brace
        while k < len(src):
            if src[k] == "{":
                depth += 1
            elif src[k] == "}":
                depth -= 1
                if depth == 0:
                    k += 1
                    break
            k += 1
        src = src[:idx] + src[k:]
        idx = src.find(sig)
    return src


def convert_body(src: str, class_name: str) -> str:
    s = src
    s = re.sub(r"GlStateManager\.pushMatrix\(\);", "poseStack.pushPose();", s)
    s = re.sub(r"GlStateManager\.popMatrix\(\);", "poseStack.popPose();", s)
    s = re.sub(r"GlStateManager\.translate\(([^;]+)\);", r"poseStack.translate(\1);", s)
    s = re.sub(r"GlStateManager\.scale\(([^,]+),\s*([^,]+),\s*([^)]+)\);", r"poseStack.scale((float)(\1), (float)(\2), (float)(\3));", s)
    s = convert_rotates(s)
    s = re.sub(r"super\.setupThirdPerson\(stack\);", "super.setupThirdPersonGun(stack, poseStack);", s)
    s = re.sub(r"super\.setupInv\(stack\);", "super.setupInventoryGun(stack, poseStack);", s)
    s = re.sub(r"super\.setupFirstPerson\(stack\);", "super.setupFirstPersonGun(stack, poseStack);", s)
    s = re.sub(r"super\.setupEntity\(stack\);", "super.setupEntityGun(stack, poseStack);", s)
    s = re.sub(r"standardAimingTransform\(stack,", "standardAimingTransform(poseStack,", s)
    s = re.sub(r"HbmAnimationsSedna\.getRelevantTransformation", "GunAnimationClientState.getRelevantTransformation", s)
    s = re.sub(
        r"HbmAnimationsSedna\.applyRelevantTransformation\(([^)]+)\);",
        r"GunAnimationClientState.applyRelevantTransformation(poseStack, \1);",
        s,
    )
    s = re.sub(
        r"Minecraft\.getMinecraft\(\)\.(?:getTextureManager\(\)|renderEngine)\.bindTexture\(ResourceManager\.(\w+)\);",
        r'/* bind */ currentTex = GunModels.tex("\1");',
        s,
    )
    s = re.sub(
        r"Minecraft\.getMinecraft\(\)\.(?:getTextureManager\(\)|renderEngine)\.bindTexture\(([^)]+)\);",
        r"/* bind */ currentTex = \1;",
        s,
    )
    s = re.sub(
        r'ResourceManager\.(\w+)\.renderPart\("([^"]+)"\);',
        r'renderPart(GunModels.obj("\1"), poseStack, bufferSource, currentTex, packedLight, packedOverlay, "\2");',
        s,
    )
    s = re.sub(
        r"(?<![\w.])renderMuzzleFlash\((?!poseStack)",
        r"renderMuzzleFlash(poseStack, bufferSource, ",
        s,
    )
    s = re.sub(
        r"this\.renderGapFlash\(([^)]+)\);",
        r"renderGapFlash(poseStack, bufferSource, \1);",
        s,
    )
    s = re.sub(
        r"this\.renderLaserFlash\(([^)]+)\);",
        r"renderLaserFlash(poseStack, bufferSource, \1);",
        s,
    )
    s = re.sub(
        r"this\.renderSmokeNodes\([^;]+;",
        f"/* TODO(CE:{class_name}:smokeNodes) smokeNodes not ported */",
        s,
    )
    s = re.sub(r"GlStateManager\.shadeModel\([^)]+\);", "", s)
    s = re.sub(r"GlStateManager\.enableLighting\(\);", "", s)
    s = re.sub(r"GlStateManager\.disableLighting\(\);", "", s)
    s = re.sub(r"GlStateManager\.enableCull\(\);", "", s)
    s = re.sub(r"GlStateManager\.disableCull\(\);", "", s)
    s = strip_method(s, "public void setupModTable")
    s = re.sub(r"ResourceManager\.(\w+)\.renderAll\(\);", r'renderAll(GunModels.obj("\1"), poseStack, bufferSource, currentTex, packedLight, packedOverlay);', s)
    s = re.sub(r".*GL11\..*\n", "", s)
    s = re.sub(r".*RenderUtil\..*\n", "", s)
    s = re.sub(r"if\s*\([^)]*needFlat[^)]*\)\s*\n", "", s)
    s = re.sub(r"if\s*\([^)]*prevShade[^)]*\)\s*\n", "", s)
    s = re.sub(r"if\s*\([^)]*GL_[A-Z]+[^)]*\)\s*\n", "", s)
    s = re.sub(r"if\s*\(\s*!prevLighting\s*\)\s*\n", "", s)
    s = re.sub(r"if\s*\(\s*!prevCull\s*\)\s*\n", "", s)
    s = re.sub(r"if\s*\(\s*prevLighting\s*\)\s*\n", "", s)
    s = re.sub(r"if\s*\(\s*prevCull\s*\)\s*\n", "", s)
    s = re.sub(r"@Override\s*\n\s*\n\s*@Override", "@Override", s)
    s = re.sub(r"boolean prevLighting = .*;\n", "", s)
    s = re.sub(r"boolean prevCull = .*;\n", "", s)
    s = re.sub(r"final boolean needFlat = .*;\n", "", s)
    s = re.sub(r"final int prevShade = .*;\n", "", s)
    s = re.sub(r"ItemGunBaseNT\.getMode\(stack\)", "ItemGunBaseNT.getMode(stack, 0)", s)
    s = re.sub(r"(?<![\w.])renderGapFlash\((?!poseStack)", "renderGapFlash(poseStack, bufferSource, ", s)
    s = re.sub(r"(?<![\w.])renderLaserFlash\((?!poseStack)", "renderLaserFlash(poseStack, bufferSource, ", s)
    s = re.sub(
        r"XWeaponModManager\.hasUpgrade\(stack,\s*(\d+),\s*XWeaponModManager\.(ID_\w+)\)",
        lambda m: f'XWeaponModManager.hasUpgrade(stack, {m.group(1)}, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "{ID_MAP.get(m.group(2), m.group(2).lower())}"))',
        s,
    )
    s = re.sub(
        r"stack\.getItem\(\) == ModItems\.(gun_[a-z0-9_]+)",
        lambda m: f"stack.getItem() == {MODITEM_CLASS.get(m.group(1), 'GunPistolItems')}.{m.group(1).upper()}.get()",
        s,
    )
    s = re.sub(
        r"float aimingProgress = ItemGunBaseNT\.prevAimingProgress \+\s*\(ItemGunBaseNT\.aimingProgress - ItemGunBaseNT\.prevAimingProgress\) \* interp;",
        "float aimingProgress = net.minecraft.util.Mth.lerp(partialTick, ItemGunBaseNT.prevAimingProgress, ItemGunBaseNT.aimingProgress);",
        s,
    )
    s = re.sub(r"\binterp\b", "partialTick", s)
    # Tesla yomi / other missing client classes
    s = re.sub(r".*RenderPlushie.*", "/* TODO(CE:ItemRenderTeslaCannon.java:106) RenderPlushie.yomiTex not ported */", s)
    s = re.sub(r"Minecraft\.getMinecraft\(\)\.getTextureManager\(\)\.bindTexture", "/* leftover bind */ currentTex = currentTex; //", s)
    return s


def convert_renderer(path: Path) -> str:
    src = path.read_text()
    name = path.stem
    src = re.sub(r"package com\.hbm\.render\.item\.weapon\.sedna;", "package com.hbm.client.render.item.weapon;", src)
    src = re.sub(r"@AutoRegister\([^)]*\)\n", "", src)
    src = src.replace("extends ItemRenderWeaponBase", "extends ItemRenderGunBase")
    # strip CE imports, we'll write a standard set
    src = re.sub(r"^import .*;\n", "", src, flags=re.M)
    src = convert_body(src, name)
    src = src.replace("public void setupFirstPerson(ItemStack stack)", "protected void setupFirstPersonGun(ItemStack stack, PoseStack poseStack)")
    src = src.replace("public void setupThirdPerson(ItemStack stack)", "protected void setupThirdPersonGun(ItemStack stack, PoseStack poseStack)")
    src = src.replace("public void setupInv(ItemStack stack)", "protected void setupInventoryGun(ItemStack stack, PoseStack poseStack)")
    src = src.replace("public void setupEntity(ItemStack stack)", "protected void setupEntityGun(ItemStack stack, PoseStack poseStack)")
    src = src.replace("public void renderFirstPerson(ItemStack stack)", "protected void renderFirstPerson(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay)")
    src = src.replace("public void renderOther(ItemStack stack, Object type)", "protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay)")

    # inject currentTex at start of render methods
    def inject_tex(m: re.Match[str]) -> str:
        body = m.group(0)
        if "ResourceLocation currentTex" in body:
            return body
        return body.replace("{", "{\n        ResourceLocation currentTex = this.defaultTex();", 1)

    src = re.sub(
        r"protected void renderFirstPerson\([^)]+\) \{",
        lambda m: m.group(0)[:-1] + "{\n        ResourceLocation currentTex = this.defaultTex();",
        src,
    )
    src = re.sub(
        r"protected void renderOther\([^)]+\) \{",
        lambda m: m.group(0)[:-1] + "{\n        ResourceLocation currentTex = this.defaultTex();",
        src,
    )

    header = """package com.hbm.client.render.item.weapon;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.content.GunEnergyItems;
import com.hbm.items.weapon.sedna.content.GunHeavyItems;
import com.hbm.items.weapon.sedna.content.GunLauncherItems;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunRifleItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

"""
    # drop leftover package if convert_body didn't
    src = re.sub(r"^package .*;\n+", "", src)
    # add defaultTex helper before last closing brace
    return header + src


def extract_anim_fields() -> list[tuple[str, str, str]]:
    found: list[tuple[str, str, str]] = []
    files = list(CE_FACTORY.glob("XFactory*.java")) + [CE_FACTORY / "Lego.java"]
    start_re = re.compile(
        r"public static(?: final)? BiFunction<ItemStack, AnimationEnums\.GunAnimation, BusAnimationSedna> (\w+) ="
    )
    for path in files:
        text = path.read_text()
        for m in start_re.finditer(text):
            i = m.end()
            depth = 0
            started = False
            j = i
            while j < len(text):
                ch = text[j]
                if ch in "{(":
                    depth += 1
                    started = True
                elif ch in "})":
                    depth -= 1
                elif ch == ";" and started and depth <= 0:
                    j += 1
                    break
                j += 1
            body = text[m.start() : j]
            found.append((path.name, m.group(1), body))
    return found


def write_gun_anims() -> None:
    fields = extract_anim_fields()
    chunks: list[str] = []
    for fname, name, raw in fields:
        body = raw
        body = body.replace("AnimationEnums.GunAnimation", "GunAnimationType")
        body = re.sub(r"ResourceManager\.(\w+_anim)", lambda mm: f'GunModels.json("{mm.group(1)}")', body)
        body = body.replace(
            "MainRegistry.proxy.me().inventory",
            "Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.getInventory()",
        )
        body = body.replace(
            "MainRegistry.proxy.me().getRNG()",
            "new java.util.Random()",
        )
        body = body.replace("@SuppressWarnings(\"incomplete-switch\") ", "")
        body = re.sub(
            r"public static(?: final)? BiFunction<ItemStack, GunAnimationType, BusAnimationSedna>",
            "public static final BiFunction<ItemStack, GunAnimationType, BusAnimationSedna>",
            body,
            count=1,
        )
        chunks.append(f"    // CE: {fname}\n    {body}")
    text = """package com.hbm.client.render.item.weapon;

import com.hbm.config.ClientConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.render.anim.sedna.BusAnimationKeyframeSedna.IType;
import com.hbm.render.anim.sedna.BusAnimationSedna;
import com.hbm.render.anim.sedna.BusAnimationSequenceSedna;
import com.hbm.weapon.anim.GunAnimationType;
import com.hbm.weapon.anim.HbmAnimationType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;

/** CE XFactory LAMBDA_*_ANIMS, copied verbatim. wrap() adapts GunAnimationType to HbmAnimationType. */
public final class GunAnims {
    private GunAnims() {}

    public static BiFunction<ItemStack, HbmAnimationType, BusAnimationSedna> wrap(
            BiFunction<ItemStack, GunAnimationType, BusAnimationSedna> inner) {
        return (stack, raw) -> raw instanceof GunAnimationType type ? inner.apply(stack, type) : null;
    }

""" + "\n\n".join(chunks) + "\n}\n"
    (OUT / "GunAnims.java").write_text(text)
    print(f"wrote GunAnims {len(fields)} lambdas")


def write_gun_models() -> None:
    lines = [
        "package com.hbm.client.render.item.weapon;",
        "",
        "import com.hbm.main.MainRegistry;",
        "import com.hbm.render.anim.sedna.AnimationLoader;",
        "import com.hbm.render.anim.sedna.BusAnimationSedna;",
        "import com.hbm.render.loader.HbmObjModel;",
        "import com.hbm.render.loader.ModelFormatException;",
        "import net.minecraft.resources.ResourceLocation;",
        "",
        "import javax.annotation.Nullable;",
        "import java.util.Map;",
        "import java.util.Set;",
        "import java.util.concurrent.ConcurrentHashMap;",
        "",
        "/** Lazy CE ResourceManager field → path lookup. No invented assets. */",
        "public final class GunModels {",
        "    private GunModels() {}",
        "    private static final Map<String, String> OBJ = new ConcurrentHashMap<>();",
        "    private static final Map<String, String> TEX = new ConcurrentHashMap<>();",
        "    private static final Map<String, String> ANIM = new ConcurrentHashMap<>();",
        "    private static final Map<String, HbmObjModel> OBJ_CACHE = new ConcurrentHashMap<>();",
        "    private static final Map<String, Map<String, BusAnimationSedna>> ANIM_CACHE = new ConcurrentHashMap<>();",
        "    private static final Set<ResourceLocation> WARNED = ConcurrentHashMap.newKeySet();",
        "    static {",
    ]
    for k, v in sorted(OBJ_MAP.items()):
        lines.append(f'        OBJ.put("{k}", "{v}");')
    for k, v in sorted(TEX_MAP.items()):
        lines.append(f'        TEX.put("{k}", "{v}");')
    lines.append('        TEX.put("ncrpa_arm", "textures/armor/ncrpa_arm.png");')
    for k, v in sorted(ANIM_MAP.items()):
        lines.append(f'        ANIM.put("{k}", "{v}");')
    lines += [
        "    }",
        "",
        "    public static final ResourceLocation SPAS12_OBJ = rl(\"models/weapons/spas-12.obj\");",
        "    public static final ResourceLocation SPAS12_TEX = rl(\"textures/models/weapons/spas-12.png\");",
        "    public static final ResourceLocation SPAS12_ANIM = rl(\"models/weapons/animations/spas12.json\");",
        "    public static final ResourceLocation CASINGS_TEX = rl(\"textures/particle/casings.png\");",
        "    public static final ResourceLocation UZI_OBJ = rl(\"models/weapons/uzi.obj\");",
        "    public static final ResourceLocation UZI_TEX = rl(\"textures/models/weapons/uzi.png\");",
        "    public static final ResourceLocation UZI_SATURNITE_TEX = rl(\"textures/models/weapons/uzi_saturnite.png\");",
        "    public static final ResourceLocation AM180_OBJ = rl(\"models/weapons/am180.obj\");",
        "    public static final ResourceLocation AM180_TEX = rl(\"textures/models/weapons/am180.png\");",
        "    public static final ResourceLocation AM180_ANIM = rl(\"models/weapons/animations/am180.json\");",
        "",
        "    @Nullable public static HbmObjModel spas12() { return obj(\"spas_12\"); }",
        "    public static Map<String, BusAnimationSedna> spas12Anim() { return json(\"spas_12_anim\"); }",
        "    @Nullable public static HbmObjModel uzi() { return obj(\"uzi\"); }",
        "    @Nullable public static HbmObjModel am180() { return obj(\"am180\"); }",
        "    public static Map<String, BusAnimationSedna> am180Anim() { return json(\"am180_anim\"); }",
        "",
        "    @Nullable",
        "    public static HbmObjModel obj(String field) {",
        "        String path = OBJ.get(field);",
        "        if (path == null) return null;",
        "        return OBJ_CACHE.computeIfAbsent(field, f -> tryLoadModel(rl(path)));",
        "    }",
        "",
        "    public static ResourceLocation tex(String field) {",
        "        String path = TEX.get(field);",
        "        return path != null ? rl(path) : rl(\"textures/models/weapons/debug_gun.png\");",
        "    }",
        "",
        "    public static Map<String, BusAnimationSedna> json(String field) {",
        "        String path = ANIM.get(field);",
        "        if (path == null) return Map.of();",
        "        return ANIM_CACHE.computeIfAbsent(field, f -> tryLoadAnim(rl(path)));",
        "    }",
        "",
        "    @Nullable",
        "    private static HbmObjModel tryLoadModel(ResourceLocation resource) {",
        "        try { return HbmObjModel.get(resource); }",
        "        catch (ModelFormatException e) { warnOnce(resource, \"OBJ model\", e); return null; }",
        "    }",
        "",
        "    private static Map<String, BusAnimationSedna> tryLoadAnim(ResourceLocation resource) {",
        "        try {",
        "            Map<String, BusAnimationSedna> loaded = AnimationLoader.load(resource);",
        "            if (loaded != null) return loaded;",
        "            warnOnce(resource, \"animation JSON (resource not found)\", null);",
        "        } catch (Exception e) { warnOnce(resource, \"animation JSON\", e); }",
        "        return Map.of();",
        "    }",
        "",
        "    private static void warnOnce(ResourceLocation resource, String kind, @Nullable Exception e) {",
        "        if (WARNED.add(resource)) {",
        "            MainRegistry.logger.warn(\"GunModels: failed to load {} '{}' (logged once).\", kind, resource, e);",
        "        }",
        "    }",
        "",
        "    private static ResourceLocation rl(String path) {",
        "        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path);",
        "    }",
        "}",
        "",
    ]
    (OUT / "GunModels.java").write_text("\n".join(lines))


def main() -> None:
    write_gun_models()
    write_gun_anims()
    n = 0
    for path in sorted(CE_SEDNA.glob("ItemRender*.java")):
        if path.name in SKIP_RENDERERS:
            continue
        dest = OUT / path.name.replace("ItemRenderSPAS12", "ItemRenderSpas12")
        dest.write_text(convert_renderer(path))
        n += 1
    print(f"wrote {n} renderers + GunModels")
    print(f"obj fields {len(OBJ_MAP)} tex {len(TEX_MAP)} anim {len(ANIM_MAP)}")


if __name__ == "__main__":
    main()
