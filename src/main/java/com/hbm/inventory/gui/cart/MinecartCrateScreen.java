package com.hbm.inventory.gui.cart;

import com.hbm.inventory.container.MinecartCrateMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for {@link MinecartCrateMenu} - port of CE's {@code EntityMinecartCrate.GuiCartCrate}.
 * Review-pass fix: this menu was fully wired and entity-reachable
 * ({@code EntityMinecartCrate#interact} calls {@code player.openMenu(this, ...)}) but had no bound
 * client {@code Screen} anywhere, so right-clicking a placed ore/crate minecart did nothing visible on
 * the client despite the server-side container opening correctly - see
 * {@link com.hbm.inventory.gui.train.TrainCargoTramScreen}'s class javadoc for the same finding on its
 * sibling menu, and {@code com.hbm.main.VehicleCargoClientRegistry} for the registration this class is
 * bound through.
 */
public class MinecartCrateScreen extends GuiInfoContainer<MinecartCrateMenu> {

    // CE never drew a dedicated cart-crate texture - its real GuiCartCrate inner class reuses the
    // steel storage crate's own PNG directly (confirmed by reading EntityMinecartCrate.java:176-201).
    // This port's previous "gui/cart/gui_cart_crate.png" path was invented and will never resolve,
    // even after the asset-copy pass - see the survey's Headline finding 2. CrateScreen's own STEEL
    // CrateType needs this exact same file (CrateBlockEntity.java:59,97), so no new art is needed.
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/storage/gui_crate_steel.png");

    public MinecartCrateScreen(MinecartCrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // CE: 6x9 cargo grid (rows end at y=126), player inventory rows starting at y=140, hotbar at
        // y=198 - see MinecartCrateMenu's own slot layout.
        // ySize is CE's real 222, not 224 - see the survey's Headline finding 5.
        this.imageWidth = 176;
        this.imageHeight = 222;
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
