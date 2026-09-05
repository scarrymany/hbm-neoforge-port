package com.hbm.inventory.gui.machine;

import com.hbm.blockentity.machine.MachineCombustionEngineBlockEntity;
import com.hbm.inventory.container.machine.MachineCombustionEngineMenu;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.items.machine.ItemPistons;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

/**
 * Exact CE {@code GUICombustionEngine} on existing {@code gui_combustion.png} 176×203.
 * Ignition 79,13 / slider 79+(setting*32/30),38 / power 143,69-i / tank 35,69.
 * Invented Button widgets removed — NBTControlPacket like CE {@code :121-142}.
 */
public class MachineCombustionEngineScreen extends GuiInfoContainer<MachineCombustionEngineMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/generators/gui_combustion.png");

    private int setting;
    private boolean mouseLocked;

    public MachineCombustionEngineScreen(MachineCombustionEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 203;
        this.inventoryLabelY = this.imageHeight - 94;
        this.setting = menu.be.setting;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (this.mouseLocked) {
            int next = Mth.clamp((mouseX - this.leftPos - 81) * 30 / 32, 0, 30);
            if (this.setting != next) {
                this.setting = next;
                CompoundTag data = new CompoundTag();
                data.putInt("setting", next);
                PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            }
        }

        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineCombustionEngineBlockEntity be = this.getMenu().be;
        ItemStack piston = be.inventory.getStackInSlot(2);
        if (!piston.isEmpty() && piston.getItem() instanceof ItemPistons item) {
            int i = item.getType().ordinal();
            guiGraphics.blit(TEXTURE, x + 80, y + 51, 176, 52 + i * 12, 25, 12);
        }

        guiGraphics.blit(TEXTURE, x + 79 + (this.setting * 32 / 30), y + 38, 192, 15, 4, 8);
        if (be.isOn) {
            guiGraphics.blit(TEXTURE, x + 79, y + 13, 192, 0, 35, 15);
        }

        int p = (int) (be.getPower() * 53 / MachineCombustionEngineBlockEntity.MAX_POWER);
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 143, y + 69 - p, 176, 52 - p, 16, p);
        }
        be.tank.renderTank(x + 35, y + 69, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :167-169 — inventory only
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineCombustionEngineBlockEntity be = this.getMenu().be;
        if (!this.mouseLocked) {
            drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 143, topPos + 17, 16, 52,
                    be.getPower(), MachineCombustionEngineBlockEntity.MAX_POWER);
            be.tank.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 35, topPos + 17, 16, 52);
        }

        if (this.mouseLocked || isHovered(mouseX, mouseY, 80, 38, 34, 8)) {
            int tx = Mth.clamp(mouseX, leftPos + 80, leftPos + 114);
            int ty = Mth.clamp(mouseY, topPos + 38, topPos + 46);
            guiGraphics.renderTooltip(this.font, Component.literal(((this.setting * 2) / 10D) + "mB/t"), tx, ty);
        }

        ItemStack piston = be.inventory.getStackInSlot(2);
        if (!piston.isEmpty() && piston.getItem() instanceof ItemPistons item
                && be.tank.getTankType().hasTrait(FT_Combustible.class)) {
            FT_Combustible trait = be.tank.getTankType().getTrait(FT_Combustible.class);
            int grade = trait.getGrade().ordinal();
            double[] eff = item.getType().eff;
            double power = this.setting * 0.2 * trait.getCombustionEnergy() / 1_000D
                    * (grade < eff.length ? eff[grade] : 0);
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 50, 35, 14, mouseX, mouseY,
                    Component.literal(ChatFormatting.YELLOW + String.format(Locale.US, "%,d", (int) power) + " HE/t"),
                    Component.literal(ChatFormatting.YELLOW + String.format(Locale.US, "%,d", (int) (power * 20)) + " HE/s"));
        }

        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 13, 35, 15, mouseX, mouseY,
                Component.literal("Ignition"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUICombustionEngine.java:134-151
        if (isHovered(mouseX, mouseY, 89, 13, 16, 14)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("turnOn", true);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        if (isHovered(mouseX, mouseY, 79, 38, 36, 8)) {
            click();
            this.mouseLocked = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.mouseLocked && (button == 0 || button == 1)) {
            this.mouseLocked = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
