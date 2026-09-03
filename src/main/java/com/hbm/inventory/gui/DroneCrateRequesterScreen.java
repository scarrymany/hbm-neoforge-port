package com.hbm.inventory.gui;

import com.hbm.main.MainRegistry;
import com.hbm.module.ModulePatternMatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.Arrays;

/**
 * CE {@code GUIDroneRequester} - requester inventory screen (filter + stock slots).
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIDroneRequester.java
 * <p>
 * Port includes filter mode overlay tooltips (CE :38-40).
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

        if (this.menu.getCarried().isEmpty()) {
            for (int i = 0; i < 9; i++) {
                Slot slot = this.menu.slots.get(i);
                if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    String mode = this.menu.getFilterMode(i);
                    if (mode != null) {
                        graphics.renderComponentTooltip(this.font, Arrays.asList(
                                Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                                Component.literal(ModulePatternMatcher.getLabel(mode))
                        ), mouseX, mouseY - 30);
                    }
                }
            }
        }
    }
}
