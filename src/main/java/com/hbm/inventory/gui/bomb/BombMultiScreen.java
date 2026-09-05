package com.hbm.inventory.gui.bomb;

import com.hbm.blockentity.bomb.BombMultiBlockEntity;
import com.hbm.inventory.container.bomb.BombMultiMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported from CE's {@code GUIBombMulti} (68 lines). Texture {@code bombGeneric.png} 176×166,
 * displays modifier result icons (slots 2+5) on right-side preview overlay.
 */
public class BombMultiScreen extends GuiInfoContainer<BombMultiMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/bomb_generic.png");

    public BombMultiScreen(BombMultiMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        BombMultiBlockEntity be = this.getMenu().be;
        int type2 = be.return2type();
        int type5 = be.return5type();

        if (type2 == type5 && type2 > 0) {
            int iconIndex = type2 - 1;
            guiGraphics.blit(TEXTURE, x + 124, y + 34, 176, iconIndex * 18, 18, 18);
        }

        if (type2 != type5 && (type2 > 0 || type5 > 0)) {
            guiGraphics.blit(TEXTURE, x + 124, y + 34, 176, 7 * 18, 18, 18);
        }
    }
}
