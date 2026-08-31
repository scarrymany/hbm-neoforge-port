package com.hbm.client;

import com.hbm.blockentity.IBufPacketReceiver;
import com.hbm.client.render.item.weapon.GunAnimationClientState;
import com.hbm.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm.inventory.gui.SatPanelClientState;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.BufPacket;
import com.hbm.packet.toclient.ExplosionEffectSyncPacket;
import com.hbm.packet.toclient.ExplosionRemovalSyncPacket;
import com.hbm.packet.toclient.GunAnimationPayload;
import com.hbm.packet.toclient.HbmEffectPacket;
import com.hbm.packet.toclient.NukeExplosionRemovalSyncPacket;
import com.hbm.packet.toclient.RadFogPayload;
import com.hbm.packet.toclient.SatPanelPayload;
import com.hbm.particle.ModParticleTypes;
import com.hbm.render.anim.sedna.BusAnimationSedna;
import com.hbm.weapon.anim.GunAnimationType;
import com.hbm.weapon.anim.HbmAnimationType;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.BiFunction;

/**
 * Dedicated-server-safe S2C payload bodies. Common packet classes must not mention
 * {@code net.minecraft.client.*} and must not annotate the registrar method-ref with
 * {@code @OnlyIn} — RuntimeDistCleaner strips those methods, then
 * {@code HbmNetwork.registerPackets} NPEs / NSMEs on dedicated server.
 */
public final class ClientPackets {

    private ClientPackets() {
    }

    public static void explosionRemoval(ExplosionRemovalSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            for (BlockPos pos : packet.positions()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        });
    }

    public static void explosionEffect(ExplosionEffectSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            ExplosionEffectStandard.performClient(level, packet.x(), packet.y(), packet.z(), packet.size(), packet.affectedBlocks());
        });
    }

    public static void nukeExplosionRemoval(NukeExplosionRemovalSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            for (BlockPos pos : packet.positions()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        });
    }

    public static void buf(BufPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;

            BlockEntity be = level.getBlockEntity(packet.pos());
            if (!(be instanceof IBufPacketReceiver receiver)) return;

            RegistryFriendlyByteBuf buf =
                    new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(packet.data()), level.registryAccess(), ConnectionType.OTHER);
            try {
                receiver.deserialize(buf);
            } catch (Exception e) {
                MainRegistry.logger.warn("A ByteBuf sync packet failed to deserialize (buffer underflow - more data was" +
                        " read than the packet actually contained). Block: {}", be.getBlockState().getBlock());
                MainRegistry.logger.warn(e.getMessage(), e);
            } finally {
                buf.release();
            }
        });
    }

    public static void satPanel(SatPanelPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            SatPanelClientState.LATEST = packet;
        });
    }

    public static void radFog(RadFogPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = Minecraft.getInstance().level;
            if (level == null) return;
            level.addParticle(ModParticleTypes.RADIATION_FOG.get(), packet.x(), packet.y(), packet.z(), 0.0D, 0.0D, 0.0D);
        });
    }

    public static void hbmEffect(HbmEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            packet.effect().summonParticle(level, packet.x(), packet.y(), packet.z(), packet.data());
        });
    }

    public static void gunAnimation(GunAnimationPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) return;

            if (!(stack.getItem() instanceof ItemGunBaseNT gun)) {
                MainRegistry.logger.debug(
                        "Received GunAnimationPayload for a non-ItemGunBaseNT held item ({}) - tool " +
                                "animation playback is out of c6-weapon-gun-rendering's scope, ignoring.",
                        stack.getItem());
                return;
            }

            int gunIndex = packet.gunIndex();
            if (gunIndex < 0 || gunIndex >= GunAnimationClientState.hotbar[0].length) return;
            if (gunIndex >= gun.getConfigCount()) return;

            int slot = player.getInventory().selected;
            if (slot < 0 || slot > 8) slot = Math.abs(slot) % 9;

            GunAnimationType[] values = GunAnimationType.values();
            int ordinal = packet.animationType();
            if (ordinal < 0 || ordinal >= values.length) return;
            GunAnimationType type = values[ordinal];

            if (type == GunAnimationType.CYCLE) {
                if (gunIndex < gun.lastShot.length) gun.lastShot[gunIndex] = System.currentTimeMillis();
                gun.shotRand = player.level().random.nextDouble();
            }

            GunConfig config = gun.getConfig(stack, gunIndex);
            BiFunction<ItemStack, HbmAnimationType, BusAnimationSedna> anims = config.getAnims(stack);
            if (anims == null) return;

            BusAnimationSedna animation = anims.apply(stack, type);
            if (animation == null && type == GunAnimationType.ALT_CYCLE) {
                animation = anims.apply(stack, GunAnimationType.CYCLE);
            }

            if (animation != null) {
                boolean isReloadAnimation = type == GunAnimationType.RELOAD || type == GunAnimationType.RELOAD_CYCLE;
                GunAnimationClientState.hotbar[slot][gunIndex] = new GunAnimationClientState.Animation(
                        stack.getItem().getDescriptionId(), System.currentTimeMillis(), animation, type,
                        isReloadAnimation && config.getReloadAnimSequential(stack));
            }
        });
    }
}
