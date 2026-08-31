package com.hbm.inventory.gui;

import com.hbm.blockentity.bomb.LaunchPadRustedBlockEntity;
import com.hbm.inventory.container.LaunchPadRustedMenu;
import com.hbm.items.tool.LaunchInfraItems;
import com.hbm.packet.toserver.LaunchPadRustedControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Random;

/**
 * Screen for {@link LaunchPadRustedMenu}, ported from CE's {@code GUILaunchPadRusted} (126 lines,
 * read in full for this review pass - see
 * {@code docs/phase5/gui_screens_survey_weapons_storage_special.md} Headline finding 4). No power/
 * fuel gauges - the rusted pad's unlock condition is item-presence-based
 * ({@code launch_code}/{@code launch_key}), not power/fuel, matching
 * {@link LaunchPadRustedBlockEntity}'s own scope.
 * <p>
 * <b>Review-pass additions</b>, all against CE's exact pixel numbers:
 * <ul>
 *   <li>{@code imageHeight} corrected from 222 to CE's real {@code ySize} of 236 (matches
 *   {@link LaunchPadScreen}'s sibling fix - not previously flagged by name in the survey's Headline
 *   finding 4 table, which only explicitly checked {@code LaunchPadScreen}'s own {@code ySize}, but
 *   confirmed here by reading {@code GUILaunchPadRusted.java} directly).</li>
 *   <li>Added the {@code hasCodes}/{@code hasKey} 6x8 status icons (slots 1/2 holding
 *   {@link LaunchInfraItems#LAUNCH_CODE}/{@link LaunchInfraItems#LAUNCH_KEY}) - CE draws the exact
 *   same source icon at both positions ({@code drawTexturedModalRect(...,192,0,6,8)} twice), which
 *   this port replicates faithfully rather than "fixing" what may look like a copy-paste artifact in
 *   CE, per this report's own convention of matching CE byte-for-byte rather than normalizing it.</li>
 *   <li>Added the 8-digit pseudo-random "launch codes" readout, keyed off the block's own position
 *   ({@code new Random(pos.getX()*131071L + pos.getZ())}) exactly like CE - deterministic per block,
 *   not randomized per render.</li>
 *   <li>Added the "Release Missile" click zone (16x16 at (26,36)) with CE's exact 5-line warning
 *   tooltip, wired through the new {@link LaunchPadRustedControlPacket} (see that class's own
 *   javadoc for why a new payload was needed rather than reusing an existing one).</li>
 * </ul>
 * <p>
 * <b>Not ported</b> (cross-report dependency, named in the survey): the live 3D missile-preview
 * render fixed to the doomsday-rusted missile item ({@code ItemRenderMissileGeneric.renderers}) -
 * see {@link LaunchPadScreen}'s own javadoc for the identical, shared gap.
 */
public class LaunchPadRustedScreen extends GuiInfoContainer<LaunchPadRustedMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/weapon/gui_launch_pad_rusted.png");

    private final LaunchPadRustedBlockEntity pad;

    public LaunchPadRustedScreen(LaunchPadRustedMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.pad = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 236;
    }

    private static final int RELEASE_X = 26;
    private static final int RELEASE_Y = 36;
    private static final int RELEASE_SIZE = 16;

    private boolean hasCodes() {
        ItemStack codes = pad.inventory.getStackInSlot(1);
        return !codes.isEmpty() && codes.getItem() == LaunchInfraItems.LAUNCH_CODE.get();
    }

    private boolean hasKey() {
        ItemStack key = pad.inventory.getStackInSlot(2);
        return !key.isEmpty() && key.getItem() == LaunchInfraItems.LAUNCH_KEY.get();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        boolean hasCodes = hasCodes();
        boolean hasKey = hasKey();

        if (hasCodes) guiGraphics.blit(TEXTURE, x + 121, y + 32, 192, 0, 6, 8);
        if (hasKey) guiGraphics.blit(TEXTURE, x + 139, y + 32, 192, 0, 6, 8);

        if (hasCodes && hasKey && pad.missileLoaded) {
            Random rand = new Random((long) pad.getBlockPos().getX() * 131_071L + pad.getBlockPos().getZ());
            int launchCodes = rand.nextInt(100_000_000);

            for (int i = 0; i < 8; i++) {
                int magnitude = (int) Math.pow(10, i);
                int digit = (launchCodes % (magnitude * 10)) / magnitude;
                guiGraphics.blit(TEXTURE, x + 109 + 6 * i, y + 85, 192 + 6 * digit, 8, 6, 8);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, RELEASE_X, RELEASE_Y, RELEASE_SIZE, RELEASE_SIZE)) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));

            CompoundTag data = new CompoundTag();
            data.putBoolean("release", true);
            PacketDistributor.sendToServer(new LaunchPadRustedControlPacket(pad.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + RELEASE_X, topPos + RELEASE_Y, RELEASE_SIZE, RELEASE_SIZE, List.of(
                Component.literal("Release Missile").withStyle(ChatFormatting.YELLOW),
                Component.literal("Missile is locked in launch position,"),
                Component.literal("releasing may cause damage to the missile."),
                Component.literal("Damaged missile can not be put back"),
                Component.literal("into launching position.")));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
