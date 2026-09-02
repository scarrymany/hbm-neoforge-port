#!/usr/bin/env python3
"""Close leftover misses that have real CE pngs. No invented art. No mat cubes CE never shipped."""
from __future__ import annotations

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from phase10_texture_miss_audit import (  # noqa: E402
    PORT,
    PORT_BLOCK_MODEL,
    PORT_BLOCKSTATE,
    PORT_ITEM_MODEL,
    block_playable,
    item_playable,
    registry_ids,
)

REWIRED: list[str] = []

PIPE_COLUMN = {
    "deco_pipe": ("pipe_top", "pipe_side"),
    "deco_pipe_red": ("pipe_top_red", "pipe_side_red"),
    "deco_pipe_green": ("pipe_top_green", "pipe_side_green"),
    "deco_pipe_rusted": ("pipe_top_rusty", "pipe_side_rusty"),
    "deco_pipe_marked": ("pipe_top_marked", "pipe_side_marked"),
    "deco_pipe_green_rusted": ("pipe_top_green_rusty", "pipe_side_green_rusty"),
}
# framed/quad/rim share the same CE color faces as the matching unframed pipe.
for kind in ("framed", "quad", "rim"):
    PIPE_COLUMN[f"deco_pipe_{kind}"] = ("pipe_top", "pipe_side")
    PIPE_COLUMN[f"deco_pipe_{kind}_red"] = ("pipe_top_red", "pipe_side_red")
    PIPE_COLUMN[f"deco_pipe_{kind}_green"] = ("pipe_top_green", "pipe_side_green")
    PIPE_COLUMN[f"deco_pipe_{kind}_rusted"] = ("pipe_top_rusty", "pipe_side_rusty")
    PIPE_COLUMN[f"deco_pipe_{kind}_marked"] = ("pipe_top_marked", "pipe_side_marked")
    PIPE_COLUMN[f"deco_pipe_{kind}_green_rusted"] = ("pipe_top_green_rusty", "pipe_side_green_rusty")
# framed uses CE pipe_frame as the extra identity when uncolored
PIPE_COLUMN["deco_pipe_framed"] = ("pipe_frame", "pipe_frame")


def dump(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def write_column(bid: str, end: str, side: str) -> None:
    dump(
        PORT_BLOCK_MODEL / f"{bid}.json",
        {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": f"hbm:block/{end}", "side": f"hbm:block/{side}"},
        },
    )
    dump(PORT_BLOCKSTATE / f"{bid}.json", {"variants": {"": {"model": f"hbm:block/{bid}"}}})
    dump(PORT_ITEM_MODEL / f"{bid}.json", {"parent": f"hbm:block/{bid}"})
    REWIRED.append(bid)


def write_cube(bid: str, tex: str) -> None:
    dump(
        PORT_BLOCK_MODEL / f"{bid}.json",
        {"parent": "minecraft:block/cube_all", "textures": {"all": tex}},
    )
    dump(PORT_BLOCKSTATE / f"{bid}.json", {"variants": {"": {"model": f"hbm:block/{bid}"}}})
    dump(PORT_ITEM_MODEL / f"{bid}.json", {"parent": f"hbm:block/{bid}"})
    REWIRED.append(bid)


def write_item(iid: str, layer0: str, layer1: str | None = None) -> None:
    tex = {"layer0": layer0}
    if layer1:
        tex["layer1"] = layer1
    dump(
        PORT_ITEM_MODEL / f"{iid}.json",
        {"parent": "minecraft:item/generated", "textures": tex},
    )
    REWIRED.append(f"item:{iid}")


def write_cross(bid: str, tex: str) -> None:
    dump(
        PORT_BLOCK_MODEL / f"{bid}.json",
        {
            "parent": "minecraft:block/cross",
            "render_type": "minecraft:cutout",
            "textures": {"cross": tex},
        },
    )
    dump(PORT_BLOCKSTATE / f"{bid}.json", {"variants": {"": {"model": f"hbm:block/{bid}"}}})
    write_item(bid, tex)
    REWIRED.append(bid)


def write_carpet(bid: str, tex: str) -> None:
    dump(
        PORT_BLOCK_MODEL / f"{bid}.json",
        {
            "parent": "minecraft:block/carpet",
            "textures": {"wool": tex, "particle": tex},
        },
    )
    dump(PORT_BLOCKSTATE / f"{bid}.json", {"variants": {"": {"model": f"hbm:block/{bid}"}}})
    dump(PORT_ITEM_MODEL / f"{bid}.json", {"parent": f"hbm:block/{bid}"})
    REWIRED.append(bid)


def main() -> int:
    items, blocks = registry_ids()
    before_i = sorted(i for i in items if not item_playable(i))
    before_b = sorted(b for b in blocks if not block_playable(b))
    print(f"BEFORE item={len(before_i)}/{len(items)} block={len(before_b)}/{len(blocks)}")

    for bid, (end, side) in PIPE_COLUMN.items():
        write_column(bid, end, side)

    for rid in (
        "railing_normal",
        "railing_bend",
        "railing_end_floor",
        "railing_end_self",
        "railing_end_flipped_floor",
        "railing_end_flipped_self",
    ):
        write_cube(rid, "hbm:block/pipe_side")  # TODO(CE:models/block/railing.mtl map_Kd)

    write_cube("rbmk_inlet", "hbm:block/rbmk_steam_inlet")  # CE rbmk_steam_inlet
    write_cube("rbmk_outlet", "hbm:block/rbmk_steam_outlet")
    write_cube("red_cable_box", "hbm:block/red_cable_icon")  # TODO(CE:models/block/red_cable.json)
    write_cube("pole_satellite_receiver", "hbm:block/deco_satellite_receiver")

    for st in ("statue_elb", "statue_elb_f", "statue_elb_g", "statue_elb_w"):
        write_cube(st, "hbm:models/misc/modelstatue")

    write_cube("launch_pad", "hbm:models/launchpad/silo")
    write_cube("launch_pad_rusted", "hbm:models/launchpad/silo_rusted")
    write_cube("launch_pad_large", "hbm:models/launchpad/pad")

    write_cube("filing_cabinet", "hbm:models/file_cabinet_steel")

    write_cross("plant_reeds", "hbm:block/reeds_top")
    write_carpet("leaves_layer", "hbm:block/waste_leaves")  # CE BlockLayering(..., "waste_leaves")

    # inventory: same-object CE 3D skins / layered sprites
    write_item("mold", "hbm:item/mold_base")
    write_item("cart_ntm_crate", "hbm:item/cart.vanilla", "hbm:item/cart_overlay.crate")
    write_item("cart_ntm_destroyer", "hbm:item/cart.steel", "hbm:item/cart_overlay.destroyer")
    write_item("cart_ntm_powder", "hbm:item/cart.steel", "hbm:item/cart_overlay.powder")
    write_item("cart_ntm_semtex", "hbm:item/cart.wood", "hbm:item/cart_overlay.semtex")
    write_item("cart_ntm_ore", "hbm:item/cart.wood", "hbm:item/cart_overlay.empty")  # CE EMPTY cart

    write_item("crashed_bomb_nuke", "hbm:models/bombs/dud_nuke")
    write_item("crashed_bomb_balefire", "hbm:models/bombs/dud_balefire")
    write_item("crashed_bomb_conventional", "hbm:models/bombs/dud_conventional")
    write_item("crashed_bomb_salted", "hbm:models/bombs/dud_salted")
    for bid in (
        "crashed_bomb_nuke",
        "crashed_bomb_balefire",
        "crashed_bomb_conventional",
        "crashed_bomb_salted",
    ):
        tex = {
            "crashed_bomb_nuke": "hbm:models/bombs/dud_nuke",
            "crashed_bomb_balefire": "hbm:models/bombs/dud_balefire",
            "crashed_bomb_conventional": "hbm:models/bombs/dud_conventional",
            "crashed_bomb_salted": "hbm:models/bombs/dud_salted",
        }[bid]
        write_cube(bid, tex)

    write_item("machine_gascent", "hbm:models/machines/centrifuge_gas")
    write_item("machine_icf_reactor", "hbm:models/machines/icf")
    write_item("machine_large_turbine", "hbm:models/machines/turbine")
    write_item("machine_turbine_gas", "hbm:models/machines/turbinegas")
    write_item("machine_watz_reactor", "hbm:models/machines/watz")
    write_item("machine_minirtg", "hbm:models/machines/rtg_cell_flipped")
    write_item("machine_powerrtg", "hbm:models/machines/rtg_polonium")

    # CE aliases / same-class textures (not invented)
    write_item("ammo_debug", "hbm:item/ammo_45")  # TODO(CE:GunFactory.java ItemBakedBase ammo_45)
    write_item("fmn", "hbm:item/tablet")  # TODO(CE:ModItems.java:143 ItemPill tablet)
    write_item("fext_water", "hbm:item/ammo_fireext")  # TODO(CE:XFactoryTool.java ammo_fireext meta 0)
    write_item("fext_sand", "hbm:item/ammo_fireext_sand")  # TODO(CE:XFactoryTool.java ammo_fireext meta 2)
    write_cube("concrete_light_gray", "hbm:block/concrete_silver")  # CE 1.12 silver
    write_cube("sellafield_bedrock", "hbm:block/sellafield_slaked")  # TODO(CE:BlockSellafieldSlaked.java:51-56)
    write_cube("skeleton_holder", "hbm:block/dirt_dead")  # TODO(CE:ModBlocks.java:519 cubeAll dirt_dead)
    write_cube("rbmk_display_blank", "hbm:block/rbmk/rbmk_display")  # TODO(CE:RBMKMiniPanelBase.java:145)

    after_i = sorted(i for i in items if not item_playable(i))
    after_b = sorted(b for b in blocks if not block_playable(b))
    print(f"AFTER  item={len(after_i)}/{len(items)} block={len(after_b)}/{len(blocks)}")
    print(f"rewired={len(REWIRED)}")
    print("item leftover", after_i)
    print("block leftover", after_b)

    report = {
        "census_items": len(items),
        "census_blocks": len(blocks),
        "before_item_miss": len(before_i),
        "before_block_miss": len(before_b),
        "after_item_miss": len(after_i),
        "after_block_miss": len(after_b),
        "after_item_ids": after_i,
        "after_block_ids": after_b,
        "rewired": len(REWIRED),
    }
    (ROOT / "docs" / "phase10" / "TEXTURE_MISS_AUDIT.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
