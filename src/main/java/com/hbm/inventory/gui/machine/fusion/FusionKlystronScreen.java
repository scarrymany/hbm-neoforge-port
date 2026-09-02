package com.hbm.inventory.gui.machine.fusion;

import com.hbm.inventory.container.machine.fusion.FusionKlystronMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.FusionControlPacket;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.apache.commons.lang3.math.NumberUtils;

public class FusionKlystronScreen extends GuiInfoContainer<FusionKlystronMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/reactors/gui_fusion_klystron.png");
    
    private EditBox field;

    public FusionKlystronScreen(FusionKlystronMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 194;
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(this.font, this.leftPos + 84, this.topPos + 22, 102, 12, Component.empty());
        this.field.setTextColor(0x00FF00);
        this.field.setTextColorUneditable(0x00FF00);
        this.field.setBordered(false);
        this.field.setValue(String.valueOf(this.getMenu().be.outputTarget));
        this.addRenderableWidget(this.field);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        this.getMenu().be.compair.renderTank(x + 76, y + 18, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 18, 16, 52, be.getPower(), be.getMaxPower());
        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 43, topPos + 71, 18, 18,
                Component.literal("<- " + BobMathUtil.getShortNumber(be.output) + "KyU / "
                        + BobMathUtil.getShortNumber(be.outputTarget) + "KyU"));
        guiGraphics.drawString(this.font, "= " + BobMathUtil.getShortNumber(be.outputTarget) + "KyU", 80, 40, 0x00FF00, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.field.keyPressed(keyCode, scanCode, modifiers) || this.field.canConsumeInput()) {
            pushTarget();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.field.charTyped(codePoint, modifiers)) {
            pushTarget();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void pushTarget() {
        String text = this.field.getValue();
        if (text.isEmpty() || !NumberUtils.isCreatable(text)) return;
        long amount = (long) Double.parseDouble(text);
        CompoundTag data = new CompoundTag();
        data.putLong("amount", amount);
        PacketDistributor.sendToServer(new FusionControlPacket(this.getMenu().be.getBlockPos(), data));
    }
}
