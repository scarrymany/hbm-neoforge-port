#!/usr/bin/env python3
"""Mechanical cleanup of auto-converted CE Sedna gun renderers. Does not invent anims."""
from __future__ import annotations

import re
from pathlib import Path

DIR = Path("/workspace/src/main/java/com/hbm/client/render/item/weapon")
SKIP = {
    "ItemRenderGunBase.java",
    "ItemRenderSpas12.java",
    "ItemRenderUzi.java",
    "ItemRenderAm180.java",
    "GunAnimationClientState.java",
    "GunAnimationRegistration.java",
    "GunAnims.java",
    "GunModels.java",
    "GunClientEvents.java",
    "Vec3NT.java",
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
    "ID_UZI_SATURN": "uzi_saturnite",
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

EXTRA_SIG = (
    "ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, "
    "int packedLight, int packedOverlay"
)


def fix_scale_calls(text: str) -> str:
    def repl(m: re.Match) -> str:
        inner = m.group(1)
        args: list[str] = []
        buf: list[str] = []
        depth = 0
        for ch in inner:
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
        if len(args) != 3:
            return m.group(0)
        cleaned = []
        for a in args:
            a = re.sub(r"^\(float\)\s*", "", a).strip()
            if a.startswith("(") and a.endswith(")"):
                cleaned.append(f"(float){a}")
            else:
                cleaned.append(f"(float)({a})")
        return "poseStack.scale(" + ", ".join(cleaned) + ")"

    return re.sub(r"poseStack\.scale\(\(float\)\((.*)\)\);", repl, text)


def ensure_import(text: str, stmt: str) -> str:
    if stmt in text:
        return text
    lines = text.splitlines(True)
    last = 0
    for i, line in enumerate(lines):
        if line.startswith("import "):
            last = i
    lines.insert(last + 1, stmt + "\n")
    return "".join(lines)


def rewrite_method_sigs(text: str) -> str:
    text = re.sub(
        r"public void setupThirdPersonAkimbo\(ItemStack stack\)",
        "public void setupThirdPersonAkimbo(ItemStack stack, PoseStack poseStack)",
        text,
    )
    text = re.sub(
        r"public void renderInv\(ItemStack stack\)",
        f"public void renderInv({EXTRA_SIG})",
        text,
    )
    text = re.sub(
        r"public void renderEquipped\(ItemStack stack\)",
        f"public void renderEquipped({EXTRA_SIG})",
        text,
    )
    text = re.sub(
        r"public void renderEquippedAkimbo\(ItemStack stack\)",
        f"public void renderEquippedAkimbo({EXTRA_SIG})",
        text,
    )
    text = re.sub(
        r"public void renderModTable\(ItemStack stack, int index\)",
        "public void renderModTable(ItemStack stack, int index, PoseStack poseStack, "
        "MultiBufferSource bufferSource, int packedLight, int packedOverlay)",
        text,
    )
    text = re.sub(
        r"public void renderOther\(ItemStack stack, Object data\)",
        "protected void renderOther(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, "
        "MultiBufferSource bufferSource, int packedLight, int packedOverlay)",
        text,
    )
    return text


def inject_current_tex(text: str) -> str:
    def inject(m: re.Match) -> str:
        body = m.group(0)
        if "ResourceLocation currentTex" in body:
            return body
        if "currentTex" not in body:
            return body
        return body.replace("{", "{\n        ResourceLocation currentTex = this.defaultTex();", 1)

    return re.sub(
        r"public void render(?:Inv|Equipped|EquippedAkimbo|ModTable)\([^)]*\)\s*\{",
        inject,
        text,
    )


def fix_ids(text: str) -> str:
    def repl(m: re.Match) -> str:
        name = m.group(1)
        path = ID_MAP.get(name)
        if not path:
            return m.group(0)
        return f'ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "{path}")'

    return re.sub(r"XWeaponModManager\.(ID_[A-Z0-9_]+)", repl, text)


def comment_block(text: str, start: str, end_pat: str) -> str:
    i = text.find(start)
    if i < 0:
        return text
    m = re.search(end_pat, text[i:])
    if not m:
        return text
    chunk = text[i : i + m.end()]
    commented = "\n".join("// " + line if line.strip() and not line.strip().startswith("//") else line for line in chunk.splitlines())
    return text[:i] + commented + text[i + m.end() :]


def process_file(path: Path) -> None:
    text = path.read_text()
    orig = text
    text = fix_scale_calls(text)
    text = text.replace("HbmAnimationsSedna.", "GunAnimationClientState.")
    text = rewrite_method_sigs(text)
    text = inject_current_tex(text)
    text = fix_ids(text)
    text = text.replace("this.renderMuzzleFlash(", "renderMuzzleFlash(poseStack, bufferSource, ")
    text = text.replace("this.renderFireball(", "renderFireball(poseStack, bufferSource, ")
    if "XFactoryTool." in text:
        text = ensure_import(text, "import com.hbm.items.weapon.sedna.content.XFactoryTool;")
    if "MagazineFullReload" in text:
        text = ensure_import(text, "import com.hbm.items.weapon.sedna.mags.MagazineFullReload;")
    if "IMagazine" in text:
        text = ensure_import(text, "import com.hbm.items.weapon.sedna.mags.IMagazine;")
    if "BobMathUtil" in text:
        text = ensure_import(text, "import com.hbm.util.BobMathUtil;")
    if "GunAnimationType" in text or "GunAnimation." in text:
        text = ensure_import(text, "import com.hbm.weapon.anim.GunAnimationType;")
        text = text.replace("GunAnimation.INSPECT", "GunAnimationType.INSPECT")
        text = text.replace("GunAnimation.CYCLE", "GunAnimationType.CYCLE")
    if path.name != "Vec3NT.java" and "new Vec3NT" in text:
        # package-private helper in same package
        pass
    if text != orig:
        path.write_text(text)


def main() -> None:
    for path in sorted(DIR.glob("ItemRender*.java")):
        if path.name in SKIP:
            continue
        process_file(path)
    anims = DIR / "GunAnims.java"
    text = anims.read_text()
    text = text.replace("HbmAnimationsSedna.", "GunAnimationClientState.")
    anims.write_text(text)
    print("cleaned")


if __name__ == "__main__":
    main()
