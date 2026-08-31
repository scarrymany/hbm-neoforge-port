package com.hbm.items.tool;

import com.hbm.items.machine.ItemSatChip;
import com.hbm.packet.toclient.SatPanelPayload;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemSatInterface} (83 lines, read in full) -
 * {@code extends ItemSatChip}, right-click opens a client-only, containerless GUI ({@code
 * GUIScreenSatInterface}/{@code GUIScreenSatCoord}, chosen by {@link #coordVariant} rather than
 * CE's {@code this == ModItems.sat_interface} identity check - this port registers {@code
 * sat_interface}/{@code sat_coord} as two instances of this same class, parameterized at
 * construction, matching CE's own "same class, two registered items" shape), and its per-tick hook
 * pushes a {@link SatPanelPayload} to the holding player every 2 ticks while the item is the active
 * hotbar item - the satellite panel is a live-streamed one-way sync, not a request/response GUI
 * open. Panel controls round-trip back via {@link com.hbm.packet.toserver.SatPanelActionPayload}
 * (see that class's javadoc for why it's a new payload rather than reusing
 * {@link com.hbm.packet.toserver.ItemControlPacket}).
 */
public class ItemSatInterface extends ItemSatChip {

    private final boolean coordVariant;

    public ItemSatInterface(String descKey, Properties properties, boolean coordVariant) {
        super(descKey, properties);
        this.coordVariant = coordVariant;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            int freq = this.getFreq(stack);
            if (coordVariant) {
                com.hbm.client.ClientScreens.satCoord(freq);
            } else {
                com.hbm.client.ClientScreens.satInterface(freq);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * CE: {@code onUpdate}, gated to {@code entity instanceof EntityPlayerMP && ticksExisted % 2 == 0
     * && getHeldItemMainhand() == stack}. {@code Item#inventoryTick}'s {@code isSelected} parameter
     * is the exact 1.21.1 equivalent of CE's own main-hand check.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || !isSelected || !(entity instanceof ServerPlayer player)) return;
        if (entity.tickCount % 2 != 0) return;
        if (player.getMainHandItem() != stack) return;

        int freq = this.getFreq(stack);
        Satellite sat = SatelliteSavedData.getData(level).getSatFromFreq(freq);
        if (sat == null) return;

        PacketDistributor.sendToPlayer(player, SatPanelPayload.of(freq, sat, level));
    }
}
