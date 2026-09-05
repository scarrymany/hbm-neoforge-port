package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.HeaterHeatexBlockEntity;
import com.hbm.inventory.container.machine.dummyable.HeaterHeatexMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Exact CE {@code GUIHeaterHeatex} on existing {@code gui_heatex.png} 176×204.
 * Fields 73,31 / 73,49; tanks 44,88 + 116,88. Invented Button widgets and heat {@code fill()} removed.
 */
public class HeaterHeatexScreen extends GuiInfoContainer<HeaterHeatexMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/machine/gui_heatex.png");

    private EditBox fieldCycles;
    private EditBox fieldDelay;

    public HeaterHeatexScreen(HeaterHeatexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        HeaterHeatexBlockEntity be = this.getMenu().be;
        // CE GUIHeaterHeatex.java:42-48
        this.fieldCycles = new EditBox(this.font, this.leftPos + 73, this.topPos + 31, 30, 10, Component.empty());
        initText(this.fieldCycles);
        this.fieldCycles.setValue(String.valueOf(be.amountToCool));
        this.addRenderableWidget(this.fieldCycles);

        this.fieldDelay = new EditBox(this.font, this.leftPos + 73, this.topPos + 49, 30, 10, Component.empty());
        initText(this.fieldDelay);
        this.fieldDelay.setValue(String.valueOf(be.tickDelay));
        this.addRenderableWidget(this.fieldDelay);
    }

    private static void initText(EditBox field) {
        field.setTextColor(0x00ff00);
        field.setTextColorUneditable(0x00ff00);
        field.setBordered(false);
        field.setMaxLength(5);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        HeaterHeatexBlockEntity be = this.getMenu().be;
        // CE :89-90
        be.hot.renderTank(x + 44, y + 88, 0, 16, 52);
        be.cold.renderTank(x + 116, y + 88, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :77-79
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        HeaterHeatexBlockEntity be = this.getMenu().be;
        be.hot.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 44, topPos + 36, 16, 52);
        be.cold.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 116, topPos + 36, 16, 52);
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 70, topPos + 26, 36, 18,
                Component.literal(I18nUtil.resolveKey("gui.heatex.amount")));
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 70, topPos + 44, 36, 18,
                Component.literal(I18nUtil.resolveKey("gui.heatex.cycle")));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.fieldCycles.keyPressed(keyCode, scanCode, modifiers) || this.fieldCycles.canConsumeInput()) {
            pushCycles();
            return true;
        }
        if (this.fieldDelay.keyPressed(keyCode, scanCode, modifiers) || this.fieldDelay.canConsumeInput()) {
            pushDelay();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.fieldCycles.charTyped(codePoint, modifiers)) {
            pushCycles();
            return true;
        }
        if (this.fieldDelay.charTyped(codePoint, modifiers)) {
            pushDelay();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void pushCycles() {
        // CE :108-113 — NumberUtils.toInt, min 1
        int cyc = Math.max(NumberUtils.toInt(this.fieldCycles.getValue()), 1);
        CompoundTag data = new CompoundTag();
        data.putInt("toCool", cyc);
        PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
    }

    private void pushDelay() {
        // CE :115-119
        int delay = Math.max(NumberUtils.toInt(this.fieldDelay.getValue()), 1);
        CompoundTag data = new CompoundTag();
        data.putInt("delay", delay);
        PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
    }
}
