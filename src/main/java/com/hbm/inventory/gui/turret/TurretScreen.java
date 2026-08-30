package com.hbm.inventory.gui.turret;

import com.hbm.inventory.container.turret.TurretMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Ported (visually, from CE's {@code GUITurretBase} - the 11 remaining concrete {@code GUITurret*}
 * classes were surveyed by grep and are cosmetic texture/position overrides only, no new behavior,
 * so this one shared {@code TurretScreen} covers all of them, same as CE's shared TE-agnostic
 * layout parts) as a plain panel, matching {@code MachineRTGScreen}'s no-texture-yet convention.
 * Power gauge via {@link #drawElectricityInfo}; the 4 targeting-toggle + on/off + blacklist/
 * whitelist controls are plain vanilla {@link Button}s routed through
 * {@link TurretMenu#clickMenuButton} (see that class's javadoc) rather than CE's own hand-rolled
 * icon-hitbox-plus-tooltip approach.
 * <p>
 * <b>Not ported</b>: CE's ammo-cycling hover tooltip ({@code GUITurretBase.drawAmmo}) - CE's own
 * ammo list is empty until the gun/ammo content package lands (see
 * {@code TurretBaseBlockEntity#getAmmoList()}), and {@code GuiInfoContainer} itself has no
 * equivalent helper yet (a shared, non-turret-specific addition the report explicitly defers to
 * whoever builds that helper once ammo lists exist to feed it - see the report's decision 5).
 */
public class TurretScreen extends GuiInfoContainer<TurretMenu> {

    public TurretScreen(TurretMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos;
        int y = topPos;

        this.addRenderableWidget(Button.builder(Component.literal("On/Off"), b ->
                click(TurretMenu.BUTTON_TOGGLE_ON)).bounds(x + 8, y + 108, 50, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Players"), b ->
                click(TurretMenu.BUTTON_TARGET_PLAYERS)).bounds(x + 60, y + 108, 54, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Animals"), b ->
                click(TurretMenu.BUTTON_TARGET_ANIMALS)).bounds(x + 116, y + 108, 54, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Mobs"), b ->
                click(TurretMenu.BUTTON_TARGET_MOBS)).bounds(x + 8, y + 126, 54, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Machines"), b ->
                click(TurretMenu.BUTTON_TARGET_MACHINES)).bounds(x + 64, y + 126, 54, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal("Black/Whitelist"), b ->
                click(TurretMenu.BUTTON_TOGGLE_BLACKLIST)).bounds(x + 120, y + 126, 50, 16).build());
    }

    private void click(int buttonId) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, buttonId);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, 6, 160, 12, be.getPower(), be.getMaxPower());

        drawCustomInfo(guiGraphics, mouseX, mouseY, 8, 108, 50, 16,
                Component.literal(be.isOn() ? "On" : "Off"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, 120, 126, 50, 16,
                Component.literal(be.isBlacklistMobFilter ? "Blacklist" : "Whitelist"));
    }
}
