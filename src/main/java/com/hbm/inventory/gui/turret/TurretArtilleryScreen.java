package com.hbm.inventory.gui.turret;

import com.hbm.blockentity.turret.TurretArtyBlockEntity;
import com.hbm.blockentity.turret.TurretBaseBlockEntity;
import com.hbm.blockentity.turret.TurretHIMARSBlockEntity;
import com.hbm.inventory.container.turret.TurretMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.TurretControlPacket;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;
import java.util.List;

/**
 * CE {@code GUITurretArty}/{@code GUITurretHIMARS} on shared {@code GUITurretBase} chrome.
 * Textures already in {@code textures/gui/weapon/}.
 */
public class TurretArtilleryScreen extends GuiInfoContainer<TurretMenu> {

    private static final ResourceLocation ARTY =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/weapon/gui_turret_arty.png");
    private static final ResourceLocation HIMARS =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/weapon/gui_turret_himars.png");

    private final ResourceLocation texture;
    private EditBox nameField;
    private int whitelistIndex;

    public TurretArtilleryScreen(TurretMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.texture = menu.be instanceof TurretHIMARSBlockEntity ? HIMARS : ARTY;
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.nameField = new EditBox(this.font, leftPos + 10, topPos + 65, 50, 14, Component.literal("Name"));
        this.nameField.setMaxLength(25);
        this.nameField.setBordered(false);
        this.nameField.setTextColor(0x00FF00);
        this.addRenderableWidget(this.nameField);
    }

    private TurretBaseBlockEntity be() {
        return this.getMenu().be;
    }

    private List<String> whitelist() {
        List<String> wl = be().getWhitelist();
        return wl != null ? wl : List.of();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight);

        if (isHovered(mouseX, mouseY, 7, 80, 18, 18)) guiGraphics.blit(texture, x + 7, y + 80, 176, 58, 18, 18);
        if (isHovered(mouseX, mouseY, 43, 80, 18, 18)) guiGraphics.blit(texture, x + 43, y + 80, 194, 58, 18, 18);
        if (isHovered(mouseX, mouseY, 7, 98, 18, 18)) guiGraphics.blit(texture, x + 7, y + 98, 176, 76, 18, 18);
        if (isHovered(mouseX, mouseY, 43, 98, 18, 18)) guiGraphics.blit(texture, x + 43, y + 98, 194, 76, 18, 18);

        int i = be().getPowerScaled(53);
        if (i > 0) guiGraphics.blit(texture, x + 152, y + 97 - i, 194, 52 - i, 16, i);
        if (be().isOn) guiGraphics.blit(texture, x + 115, y + 26, 176, 40, 18, 18);
        if (be().targetPlayers) guiGraphics.blit(texture, x + 8, y + 30, 176, 0, 10, 10);
        if (be().targetAnimals) guiGraphics.blit(texture, x + 22, y + 30, 176, 10, 10, 10);
        if (be().targetMobs) guiGraphics.blit(texture, x + 36, y + 30, 176, 20, 10, 10);
        if (be().targetMachines) guiGraphics.blit(texture, x + 50, y + 30, 176, 30, 10, 10);

        if (be() instanceof TurretArtyBlockEntity arty) {
            if (arty.mode == TurretArtyBlockEntity.MODE_CANNON) {
                guiGraphics.blit(texture, x + 151, y + 16, 210, 0, 18, 18);
            } else if (arty.mode == TurretArtyBlockEntity.MODE_MANUAL) {
                guiGraphics.blit(texture, x + 151, y + 16, 210, 18, 18, 18);
            }
        } else if (be() instanceof TurretHIMARSBlockEntity himars
                && himars.mode == TurretHIMARSBlockEntity.FiringMode.MANUAL) {
            guiGraphics.blit(texture, x + 151, y + 16, 210, 0, 18, 18);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 45, 16, 52, be().getPower(), be().getMaxPower());
        String on = "§a" + I18nUtil.resolveKey("turret.on");
        String off = "§c" + I18nUtil.resolveKey("turret.off");
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 29, 10, 10,
                Component.literal(I18nUtil.resolveKey("turret.players", be().targetPlayers ? on : off)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 22, topPos + 29, 10, 10,
                Component.literal(I18nUtil.resolveKey("turret.animals", be().targetAnimals ? on : off)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 36, topPos + 29, 10, 10,
                Component.literal(I18nUtil.resolveKey("turret.mobs", be().targetMobs ? on : off)));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 50, topPos + 29, 10, 10,
                Component.literal(I18nUtil.resolveKey("turret.machines", be().targetMachines ? on : off)));

        String modeKey = "turret.arty.artillery";
        if (be() instanceof TurretArtyBlockEntity arty) {
            modeKey = arty.mode == TurretArtyBlockEntity.MODE_CANNON ? "turret.arty.cannon"
                    : arty.mode == TurretArtyBlockEntity.MODE_MANUAL ? "turret.arty.manual" : "turret.arty.artillery";
        } else if (be() instanceof TurretHIMARSBlockEntity himars) {
            modeKey = himars.mode == TurretHIMARSBlockEntity.FiringMode.MANUAL ? "turret.arty.manual" : "turret.arty.artillery";
        }
        Component[] modeLines = Arrays.stream(I18nUtil.resolveKeyArray(modeKey)).map(Component::literal).toArray(Component[]::new);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 151, topPos + 16, 18, 18, modeLines);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0x808080, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x808080, false);
        List<String> wl = whitelist();
        String current = wl.isEmpty() ? ChatFormatting.ITALIC + "None" : wl.get(Math.min(whitelistIndex, wl.size() - 1));
        guiGraphics.drawString(this.font, current, 12, 51, 0x00FF00, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 115, 25, 18, 18)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, TurretMenu.BUTTON_TOGGLE_ON);
            return true;
        }
        if (isHovered(mouseX, mouseY, 8, 29, 10, 10)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, TurretMenu.BUTTON_TARGET_PLAYERS);
            return true;
        }
        if (isHovered(mouseX, mouseY, 22, 29, 10, 10)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, TurretMenu.BUTTON_TARGET_ANIMALS);
            return true;
        }
        if (isHovered(mouseX, mouseY, 36, 29, 10, 10)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, TurretMenu.BUTTON_TARGET_MOBS);
            return true;
        }
        if (isHovered(mouseX, mouseY, 50, 29, 10, 10)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, TurretMenu.BUTTON_TARGET_MACHINES);
            return true;
        }
        if (isHovered(mouseX, mouseY, 151, 16, 18, 18)) {
            click();
            this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, TurretMenu.BUTTON_MODE);
            return true;
        }
        int count = whitelist().size();
        if (count > 0 && isHovered(mouseX, mouseY, 7, 80, 18, 18)) {
            whitelistIndex = Math.floorMod(whitelistIndex - 1, count);
            click();
            return true;
        }
        if (count > 0 && isHovered(mouseX, mouseY, 43, 80, 18, 18)) {
            whitelistIndex = (whitelistIndex + 1) % count;
            click();
            return true;
        }
        if (isHovered(mouseX, mouseY, 7, 98, 18, 18)) {
            click();
            String name = nameField.getValue();
            if (!name.isEmpty()) {
                CompoundTag data = new CompoundTag();
                data.putString("name", name);
                PacketDistributor.sendToServer(new TurretControlPacket(be().getBlockPos(), data));
                nameField.setValue("");
            }
            return true;
        }
        if (isHovered(mouseX, mouseY, 43, 98, 18, 18)) {
            click();
            if (!whitelist().isEmpty()) {
                CompoundTag data = new CompoundTag();
                data.putInt("del", whitelistIndex);
                PacketDistributor.sendToServer(new TurretControlPacket(be().getBlockPos(), data));
                whitelistIndex = 0;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
