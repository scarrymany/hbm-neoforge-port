#!/usr/bin/env python3
"""Rewrite RegisterMenuScreensEvent handlers to SafeMenuScreens.bind."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/java"
IMPORT = "import com.hbm.inventory.gui.SafeMenuScreens;\n"
CALL = re.compile(
    r"event\.register\(\s*([A-Za-z0-9_.]+)\.get\(\)\s*,\s*([A-Za-z0-9_]+)::new\s*\)"
)


def patch(path: Path) -> bool:
    text = path.read_text()
    if "event.register(" not in text or "RegisterMenuScreensEvent" not in text:
        return False
    new, n = CALL.subn(r"SafeMenuScreens.bind(event, \1, \2::new)", text)
    if n == 0:
        return False
    if "import com.hbm.inventory.gui.SafeMenuScreens;" not in new:
        # insert after package + first import block start
        new = new.replace("import com.hbm.main.MainRegistry;\n", "import com.hbm.inventory.gui.SafeMenuScreens;\nimport com.hbm.main.MainRegistry;\n", 1)
        if "import com.hbm.inventory.gui.SafeMenuScreens;" not in new:
            new = new.replace("import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;\n",
                              IMPORT + "import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;\n", 1)
    path.write_text(new)
    print(f"patched {path.relative_to(ROOT)} ({n})")
    return True


def main() -> None:
    for p in ROOT.rglob("*ClientRegistry.java"):
        patch(p)
    patch(ROOT / "com/hbm/main/ClientModRegistry.java")


if __name__ == "__main__":
    main()
