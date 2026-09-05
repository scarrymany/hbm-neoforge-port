package com.hbm.inventory.gui.turret;

import com.hbm.blockentity.turret.TurretFritzBlockEntity;
import com.hbm.inventory.container.turret.TurretMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.TurretControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

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
 * <b>Review-pass addition</b> (per {@code docs/phase5/gui_screens_survey_weapons_storage_special.md}
 * Headline finding 1): CE's {@code GUITurretBase} embeds a name-based biometric whitelist editor
 * (a text field plus cycle/add/delete controls reading/writing an {@link
 * com.hbm.items.machine.ItemTurretBiometry} chip in slot 0) that this screen never exposed, even
 * though the entire server-side mechanic ({@code TurretBaseBlockEntity#getWhitelist}/{@code addName}/
 * {@code removeName}, dispatched via {@code receiveControl}'s {@code "name"}/{@code "del"} NBT keys)
 * was already fully wired. {@link #cycleWhitelist}/{@link #addWhitelistName}/
 * {@link #removeWhitelistName} close that gap, at CE's exact hitbox coordinates
 * ({@code GUITurretBase.java:52-58,168-200}: field at (10,65), cycle buttons at (7,80)/(43,80),
 * add/delete at (7,98)/(43,98) - none of which collide with the ammo-grid/chip/battery slots CE
 * itself lays out at x&gt;=80, per {@code TurretMenu}'s own slot coordinates).
 * <p>
 * <b>Review-pass addition</b>: {@link TurretFritzBlockEntity}'s 16,000&nbsp;mB diesel tank (a real,
 * already-implemented field, see that class's own javadoc) was previously invisible in this shared
 * screen - {@link #renderBg}/{@link #render} now branch on {@code be instanceof TurretFritzBlockEntity}
 * and call {@link com.hbm.inventory.fluid.tank.FluidTankNTM#renderTank}/{@code renderTankTooltip} at
 * CE's exact {@code GUITurretFritz} coordinates (tank bottom at {@code guiLeft+134, guiTop+115},
 * width 7, height 52).
 * <p>
 * <b>Review-pass fix</b>: the pre-existing power/on-off/blacklist hover tooltips lived in
 * {@link #renderLabels}, which (per real {@code AbstractContainerScreen} behavior, confirmed against
 * this port's own {@code BatteryScreen}/{@code FluidTankScreen}/{@code LaunchPadScreen}, all of which
 * do their mouse-tracked tooltips from an overridden {@link #render}, never {@code renderLabels})
 * receives the raw, <i>un-translated</i> {@code mouseX}/{@code mouseY} even though its
 * {@link GuiGraphics} pose is already shifted by {@code leftPos}/{@code topPos} - comparing that
 * absolute mouse position against the local {@code x=8}-style hitboxes those calls used meant the
 * tooltips could never actually trigger. Moved to an overridden {@link #render}, matching the
 * established convention exactly (absolute {@code leftPos+x} hitboxes compared against the absolute
 * {@code mouseX}/{@code mouseY} {@code render} receives). {@link #renderLabels} now only draws the
 * title/inventory-label text CE's own {@code drawGuiContainerForegroundLayer} always drew and this
 * screen previously omitted entirely.
 * <p>
 * <b>Not ported</b>: CE's ammo-cycling hover tooltip ({@code GUITurretBase.drawAmmo}) - CE's own
 * ammo list is empty until the gun/ammo content package lands (see
 * {@code TurretBaseBlockEntity#getAmmoList()}), and {@code GuiInfoContainer} itself has no
 * equivalent helper yet (a shared, non-turret-specific addition the report explicitly defers to
 * whoever builds that helper once ammo lists exist to feed it - see the report's decision 5).
 * <b>Not ported</b>: the {@code stattrak} kill-tally bar and per-category lit/unlit icon overlays
 * (survey Headline 1) - both are texture-dependent cosmetic details with no functional gap, left for
 * the asset-copy pass. Arty/HIMARS mode toggle lives on {@link TurretArtilleryScreen}.
 */
public class TurretScreen extends GuiInfoContainer<TurretMenu> {

    public TurretScreen(TurretMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private EditBox nameField;
    private int whitelistIndex = 0;

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

        // Biometric whitelist editor - CE's GUITurretBase.java:52-58 exact coordinates.
        this.nameField = new EditBox(this.font, x + 10, y + 65, 50, 14, Component.literal("Name"));
        this.nameField.setMaxLength(25);
        this.nameField.setBordered(false);
        this.nameField.setTextColor(0x00FF00);
        this.addRenderableWidget(this.nameField);

        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> cycleWhitelist(-1))
                .bounds(x + 7, y + 80, 18, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> cycleWhitelist(1))
                .bounds(x + 43, y + 80, 18, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("+"), b -> addWhitelistName())
                .bounds(x + 7, y + 98, 18, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("-"), b -> removeWhitelistName())
                .bounds(x + 43, y + 98, 18, 18).build());
    }

    private void click(int buttonId) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, buttonId);
    }

    /** @return the biometric chip's name list, or an empty list if no chip/no names (never null - simplifies every caller below). */
    private List<String> whitelist() {
        List<String> wl = this.getMenu().be.getWhitelist();
        return wl != null ? wl : List.of();
    }

    private void cycleWhitelist(int delta) {
        int size = whitelist().size();
        this.whitelistIndex = size == 0 ? 0 : Math.floorMod(whitelistIndex + delta, size);
    }

    private void addWhitelistName() {
        String name = this.nameField.getValue();
        if (name.isEmpty()) return;

        CompoundTag data = new CompoundTag();
        data.putString("name", name);
        PacketDistributor.sendToServer(new TurretControlPacket(this.getMenu().be.getBlockPos(), data));
        this.nameField.setValue("");
    }

    private void removeWhitelistName() {
        if (whitelist().isEmpty()) return;

        CompoundTag data = new CompoundTag();
        data.putInt("del", this.whitelistIndex);
        PacketDistributor.sendToServer(new TurretControlPacket(this.getMenu().be.getBlockPos(), data));
        this.whitelistIndex = 0;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/weapon/gui_turret_base.png"), 
                x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (this.getMenu().be instanceof TurretFritzBlockEntity fritz) {
            fritz.tank.renderTank(x + 134, y + 115, 0, 7, 52);
        }

        List<String> wl = whitelist();
        String current = wl.isEmpty() ? ChatFormatting.ITALIC + "None" : wl.get(Math.min(whitelistIndex, wl.size() - 1));
        guiGraphics.drawString(this.font, current, x + 8, y + 51, 0x00FF00, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 6, 160, 12, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 108, 50, 16,
                Component.literal(be.isOn() ? "On" : "Off"));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 120, topPos + 126, 50, 16,
                Component.literal(be.isBlacklistMobFilter ? "Blacklist" : "Whitelist"));

        if (be instanceof TurretFritzBlockEntity fritz) {
            fritz.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 134, topPos + 115 - 52, 7, 52);
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }
}
