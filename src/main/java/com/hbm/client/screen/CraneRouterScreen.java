package com.hbm.client.screen;

import com.hbm.blockentity.network.CraneRouterBlockEntity;
import com.hbm.main.MainRegistry;
import com.hbm.menu.CraneRouterMenu;
import com.hbm.module.ModulePatternMatcher;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge port of CE's {@code GUICraneRouter} - 30 filter slots + 6 mode toggle buttons.
 * CE layout: 2 columns of filter slots (3 sides × 5 filters each), mode buttons left/right.
 * xSize=256, ySize=201 (CE dimensions).
 */
public class CraneRouterScreen extends AbstractContainerScreen<CraneRouterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/storage/gui_crane_router.png");

    public CraneRouterScreen(CraneRouterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 201;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Main background (CE: 256×93 top section)
        graphics.blit(TEXTURE, x, y, 0, 0, 256, 93);

        // Player inventory area (CE: 176×108 at offset 39,93)
        graphics.blit(TEXTURE, x + 39, y + 93, 39, 93, 176, 108);

        // Mode buttons (6 sides: 2 columns × 3 rows)
        // CE: buttons at (7, 16+k*26) for left column, (7+222, 16+k*26) for right
        // Texture: 18×18 icons at (238, 93 + mode*18)
        CraneRouterBlockEntity be = this.menu.getBlockEntity();
        if (be != null) {
            for (int j = 0; j < 2; j++) { // columns
                for (int k = 0; k < 3; k++) { // rows
                    int index = j * 3 + k;
                    int mode = be.modes[index];
                    int buttonX = x + 7 + j * 222;
                    int buttonY = y + 16 + k * 26;
                    // Blit mode icon from texture (238, 93+mode*18)
                    graphics.blit(TEXTURE, buttonX, buttonY, 238, 93 + mode * 18, 18, 18);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // Mode button tooltips (CE behavior)
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        CraneRouterBlockEntity be = this.menu.getBlockEntity();
        if (be != null) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 3; k++) {
                    int buttonX = x + 7 + j * 222;
                    int buttonY = y + 16 + k * 26;
                    if (mouseX >= buttonX && mouseX < buttonX + 18 && mouseY >= buttonY && mouseY < buttonY + 18) {
                        int index = j * 3 + k;
                        List<Component> tooltip = new ArrayList<>();
                        switch (be.modes[index]) {
                            case CraneRouterBlockEntity.MODE_NONE:
                                tooltip.add(Component.literal("OFF"));
                                break;
                            case CraneRouterBlockEntity.MODE_WHITELIST:
                                tooltip.add(Component.literal("WHITELIST"));
                                tooltip.add(Component.literal("Route if filter matches"));
                                break;
                            case CraneRouterBlockEntity.MODE_BLACKLIST:
                                tooltip.add(Component.literal("BLACKLIST"));
                                tooltip.add(Component.literal("Route if filter doesn't match"));
                                break;
                            case CraneRouterBlockEntity.MODE_WILDCARD:
                                tooltip.add(Component.literal("WILDCARD"));
                                tooltip.add(Component.literal("Route if no other route is valid"));
                                break;
                        }
                        graphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY);
                    }
                }
            }
        }

        // Filter slot tooltips (CE: show pattern mode on hover if not holding item)
        if (this.menu.getCarried().isEmpty() && be != null) {
            for (int i = 0; i < 30; i++) {
                Slot slot = this.menu.slots.get(i);
                if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    ModulePatternMatcher matcher = be.patterns[i / 5];
                    int patternIndex = i % 5;
                    if (matcher.modes[patternIndex] != null) {
                        List<Component> tooltip = new ArrayList<>();
                        tooltip.add(Component.literal("§cRight click to change"));
                        tooltip.add(Component.literal(ModulePatternMatcher.getLabel(matcher.modes[patternIndex])));
                        graphics.renderTooltip(this.font, tooltip, java.util.Optional.empty(), mouseX, mouseY - 30);
                    }
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = this.title;
        int titleX = (this.imageWidth - this.font.width(title)) / 2;
        graphics.drawString(this.font, title, titleX, 5, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8 + 39, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Mode toggle buttons
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        CraneRouterBlockEntity be = this.menu.getBlockEntity();

        if (be != null) {
            for (int j = 0; j < 2; j++) {
                for (int k = 0; k < 3; k++) {
                    int buttonX = x + 7 + j * 222;
                    int buttonY = y + 16 + k * 26;
                    if (mouseX >= buttonX && mouseX < buttonX + 18 && mouseY >= buttonY && mouseY < buttonY + 18) {
                        int index = j * 3 + k;
                        // Send clickMenuButton to server
                        if (this.minecraft != null && this.minecraft.gameMode != null) {
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                        }
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
