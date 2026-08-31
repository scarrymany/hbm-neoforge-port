package com.hbm.inventory.gui.cart;

import com.hbm.inventory.container.MinecartDestroyerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for {@link MinecartDestroyerMenu} - port of CE's {@code EntityMinecartDestroyer.GuiCartDestroyer}.
 * See {@link MinecartCrateScreen}'s class javadoc for why this class exists (review-pass fix: the menu
 * was fully wired and entity-reachable but had no bound client {@code Screen}) and
 * {@code com.hbm.main.VehicleCargoClientRegistry} for the registration.
 */
public class MinecartDestroyerScreen extends GuiInfoContainer<MinecartDestroyerMenu> {

    // CE's real filename is gui_destroyer.png, not gui_cart_destroyer.png - see the survey's
    // Headline finding 2 (right folder, wrong filename).
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/cart/gui_destroyer.png");

    public MinecartDestroyerScreen(MinecartDestroyerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // CE: two 3x3 filter-template banks (rows end at y=71), player inventory rows starting at
        // y=84, hotbar at y=142 - see MinecartDestroyerMenu's own slot layout.
        // ySize is CE's real 166, not 168 - see the survey's Headline finding 5.
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
