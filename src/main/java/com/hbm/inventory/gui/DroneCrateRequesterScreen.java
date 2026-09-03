package com.hbm.inventory.gui;

import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Minimal CE {@code GUIDroneRequester} - requester inventory screen (filter + stock slots).
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIDroneRequester.java
 * <p>
 * Partial port: displays filter and stock slots, but no ModulePatternMatcher mode indicators.
 * TODO(CE): Render filter mode overlays (EXACT/WILDCARD/OreDict icons) on filter slots.
 */
public class DroneCrateRequesterScreen extends AbstractContainerScreen<DroneCrateRequesterMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/storage/gui_drone_requester.png");

    public DroneCrateRequesterScreen(DroneCrateRequesterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
