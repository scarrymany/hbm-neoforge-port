package com.hbm.inventory.gui.machine;

import com.hbm.inventory.container.machine.MachineCombustionEngineMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (visually, from CE's {@code GUICombustionEngine}) as a plain panel - see
 * {@link MachineRTGScreen}'s javadoc for the no-texture-yet rationale. Three vanilla
 * {@link Button}s (on/off, throttle down/up) route through vanilla's
 * {@code MultiPlayerGameMode#handleInventoryButtonClick}, the same client-&gt;server path
 * {@code EnchantmentScreen}/{@code BeaconScreen} use, landing on
 * {@link MachineCombustionEngineMenu#clickMenuButton} server-side. <b>Not independently confirmed
 * against a NeoForge jar in this sandbox</b> (no decompiled artifact reachable) - this is the same
 * long-standing vanilla button-click plumbing those two screens have used since 1.13-era
 * {@code Container} buttons, carried forward unchanged through every Forge/NeoForge version since;
 * double-check the exact {@code Minecraft.gameMode} field/method names on first build.
 */
public class MachineCombustionEngineScreen extends GuiInfoContainer<MachineCombustionEngineMenu> {

    public MachineCombustionEngineScreen(MachineCombustionEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int containerId = this.getMenu().containerId;
        // CE ignition 79,13 / slider 80,38 — keep off Exact CE canister column 17,17.
        this.addRenderableWidget(Button.builder(Component.literal("On/Off"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, MachineCombustionEngineMenu.BUTTON_TOGGLE_ON)
        ).bounds(leftPos + 79, topPos + 13, 35, 15).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, MachineCombustionEngineMenu.BUTTON_THROTTLE_DOWN)
        ).bounds(leftPos + 79, topPos + 36, 16, 12).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), b ->
                this.minecraft.gameMode.handleInventoryButtonClick(containerId, MachineCombustionEngineMenu.BUTTON_THROTTLE_UP)
        ).bounds(leftPos + 97, topPos + 36, 16, 12).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        // Exact CE GUICombustionEngine.java:199 — tank at 35,69 (bottom origin, 16×52).
        this.getMenu().be.tank.renderTank(x + 35, y + 69, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 143, 17, 16, 52, this.getMenu().be.getPower(), this.getMenu().be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, 79, 50, 35, 14, Component.literal("Throttle: " + this.getMenu().be.setting + "/30"));
    }
}
