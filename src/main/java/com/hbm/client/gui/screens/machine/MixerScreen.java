package com.hbm.client.gui.screens.machine;

import com.hbm.blockentity.machine.MachineMixerBlockEntity;
import com.hbm.inventory.container.machine.MachineMixerMenu;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIMixer}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIMixer.java
 * <p>
 * GUI with power bar, progress bar, 3 tanks, and recipe toggle button.
 */
public class MixerScreen extends AbstractContainerScreen<MachineMixerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_mixer.png");

    public MixerScreen(MachineMixerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        MachineMixerBlockEntity be = menu.be;

        // CE GUIMixer.java:89-90: power bar (16x52px) at x=23, y=75-i
        long power = be.getPower();
        long maxPower = be.getMaxPower();
        int powerFill = maxPower > 0 ? (int) (power * 53 / maxPower) : 0;
        if (powerFill > 0) {
            graphics.blit(TEXTURE, leftPos + 23, topPos + 75 - powerFill, 176, 52 - powerFill, 16, powerFill);
        }

        // CE GUIMixer.java:92-95: progress bar (53x44px) at x=62, y=36
        if (be.processTime > 0 && be.progress > 0) {
            int progress = be.progress * 53 / be.processTime;
            graphics.blit(TEXTURE, leftPos + 62, topPos + 36, 192, 0, progress, 44);
        }

        // CE GUIMixer.java:96-98: tank overlays (7x52px, 7x52px, 16x52px)
        int tank0Fill = be.tanks.get(0).getMaxFill() > 0 ? (int) (be.tanks.get(0).getFill() * 52 / be.tanks.get(0).getMaxFill()) : 0;
        if (tank0Fill > 0) {
            graphics.blit(TEXTURE, leftPos + 43, topPos + 75 - tank0Fill, 200, 52 - tank0Fill, 7, tank0Fill);
        }
        int tank1Fill = be.tanks.get(1).getMaxFill() > 0 ? (int) (be.tanks.get(1).getFill() * 52 / be.tanks.get(1).getMaxFill()) : 0;
        if (tank1Fill > 0) {
            graphics.blit(TEXTURE, leftPos + 52, topPos + 75 - tank1Fill, 207, 52 - tank1Fill, 7, tank1Fill);
        }
        int tank2Fill = be.tanks.get(2).getMaxFill() > 0 ? (int) (be.tanks.get(2).getFill() * 52 / be.tanks.get(2).getMaxFill()) : 0;
        if (tank2Fill > 0) {
            graphics.blit(TEXTURE, leftPos + 117, topPos + 75 - tank2Fill, 214, 52 - tank2Fill, 16, tank2Fill);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIMixer.java:77-80: title centered + inventory label
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIMixer.java:64-71: recipe toggle button (12x12px) at x=62, y=22
        if (mouseX >= leftPos + 62 && mouseX < leftPos + 62 + 12 && mouseY >= topPos + 22 && mouseY < topPos + 22 + 12) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 1.0F);
            }
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUIMixer.java:40-58: tooltips for power, tanks, recipe button
        MachineMixerBlockEntity be = menu.be;
        if (mouseX >= leftPos + 23 && mouseX < leftPos + 23 + 16 && mouseY >= topPos + 22 && mouseY < topPos + 22 + 52) {
            graphics.renderTooltip(this.font, Component.literal(be.getPower() + " / " + be.getMaxPower() + " HE"), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 43 && mouseX < leftPos + 43 + 7 && mouseY >= topPos + 23 && mouseY < topPos + 23 + 52) {
            graphics.renderTooltip(this.font, Component.literal(be.tanks.get(0).getFill() + " / " + be.tanks.get(0).getMaxFill() + " mB"), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 52 && mouseX < leftPos + 52 + 7 && mouseY >= topPos + 23 && mouseY < topPos + 23 + 52) {
            graphics.renderTooltip(this.font, Component.literal(be.tanks.get(1).getFill() + " / " + be.tanks.get(1).getMaxFill() + " mB"), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 117 && mouseX < leftPos + 117 + 16 && mouseY >= topPos + 23 && mouseY < topPos + 23 + 52) {
            graphics.renderTooltip(this.font, Component.literal(be.tanks.get(2).getFill() + " / " + be.tanks.get(2).getMaxFill() + " mB"), mouseX, mouseY);
        }
        // CE GUIMixer.java:43-55: recipe selector tooltip
        if (mouseX >= leftPos + 62 && mouseX < leftPos + 62 + 12 && mouseY >= topPos + 22 && mouseY < topPos + 22 + 12) {
            // TODO(CE): Port MixerRecipes cycling display (requires recipe system, deferred)
            graphics.renderTooltip(this.font, Component.literal("Recipe selector\nClick to change!"), mouseX, mouseY);
        }
    }
}
