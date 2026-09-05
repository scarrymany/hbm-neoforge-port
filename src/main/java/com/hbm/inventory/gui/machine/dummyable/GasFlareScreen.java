package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineGasFlareBlockEntity;
import com.hbm.inventory.container.machine.dummyable.GasFlareMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.FluidCombustionRecipes;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;

/**
 * Exact CE {@code GUIMachineGasFlare} on existing {@code gui_flare_stack.png} 176×203.
 * Power 143,{@code 69-j} from 176,{@code 94-j}; valve 79,15; ignition 79,49; flame 88,29.
 * Invented Button widgets + {@code fill()} status bars removed.
 */
public class GasFlareScreen extends GuiInfoContainer<GasFlareMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/generators/gui_flare_stack.png");

    public GasFlareScreen(GasFlareMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineGasFlareBlockEntity be = this.getMenu().be;
        // CE GUIMachineGasFlare.java:76-87
        int j = (int) (be.getPower() * 52 / Math.max(1L, be.getMaxPower()));
        if (j > 0) {
            guiGraphics.blit(TEXTURE, x + 143, y + 69 - j, 176, 94 - j, 16, j);
        }
        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 79, y + 15, 176, 0, 35, 10);
        }
        if (be.doesBurn) {
            guiGraphics.blit(TEXTURE, x + 79, y + 49, 176, 10, 35, 14);
        }
        if (be.isOn && be.doesBurn && be.tank.getFill() > 0 && FluidCombustionRecipes.hasFuelRecipe(be.tank.getTankType())) {
            guiGraphics.blit(TEXTURE, x + 88, y + 29, 176, 24, 18, 18);
        }
        be.tank.renderTank(x + 35, y + 69, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :66 — inventory only
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineGasFlareBlockEntity be = this.getMenu().be;
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 16, 35, 10, mouseX, mouseY,
                Arrays.stream(I18nUtil.resolveKeyArray("flare.valve")).map(Component::literal).toArray(Component[]::new));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 50, 35, 14, mouseX, mouseY,
                Arrays.stream(I18nUtil.resolveKeyArray("flare.ignition")).map(Component::literal).toArray(Component[]::new));
        be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 69 - 52, 16, 52);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 69 - 52, 16, 52, be.getPower(), be.getMaxPower());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIMachineGasFlare.java:50-60
        if (isHovered(mouseX, mouseY, 89, 16, 16, 10)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("valve", true);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        if (isHovered(mouseX, mouseY, 89, 50, 16, 14)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("dial", true);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
