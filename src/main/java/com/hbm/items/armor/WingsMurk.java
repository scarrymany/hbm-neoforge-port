package com.hbm.items.armor;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.handler.ArmorUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * CE {@code com.hbm.items.armor.WingsMurk} — both {@code wings_murk} and {@code wings_limp}.
 * Flight tick 1:1 ({@code WingsMurk.java:44-115}). Client model/texture
 * TODO(CE: WingsMurk.java:27-42).
 */
public class WingsMurk extends JetpackBase {

    private final boolean murk;

    public WingsMurk(Item.Properties properties, boolean murk) {
        super(properties);
        this.murk = murk;
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        if (player.onGround()) return;

        ArmorUtil.resetFlightTime(player);

        if (player.fallDistance > 0) player.fallDistance = 0;

        Vec3 mot = player.getDeltaMovement();
        if (mot.y < -0.4D) {
            player.setDeltaMovement(mot.x, -0.4D, mot.z);
            mot = player.getDeltaMovement();
        }

        if (!murk) {
            if (player.isShiftKeyDown() && mot.y < -0.08D) {
                double mo = mot.y * -0.2D;
                Vec3 look = player.getLookAngle().scale(mo);
                player.setDeltaMovement(mot.x + look.x, mot.y + mo + look.y, mot.z + look.z);
            }
            return;
        }

        HbmPlayerAttachment props = HbmPlayerAttachment.getData(player);

        if (props.isJetpackActive()) {
            if (mot.y < 0.6D) {
                player.setDeltaMovement(mot.x, mot.y + 0.2D, mot.z);
            } else {
                player.setDeltaMovement(mot.x, 0.8D, mot.z);
            }
        } else if (props.getEnableBackpack() && !player.isShiftKeyDown()) {
            double vy = mot.y;
            if (vy < -1D) {
                player.setDeltaMovement(mot.x, vy + 0.4D, mot.z);
            } else if (vy < -0.1D) {
                player.setDeltaMovement(mot.x, vy + 0.2D, mot.z);
            } else if (vy < 0D) {
                player.setDeltaMovement(mot.x, 0D, mot.z);
            }
        }

        if (props.getEnableBackpack()) {
            Vec3 orig = player.getLookAngle();
            Vec3 look = new Vec3(orig.x, 0, orig.z).normalize();
            double mod = player.isShiftKeyDown() ? 0.25D : 1D;
            mot = player.getDeltaMovement();

            if (player.zza != 0) {
                player.setDeltaMovement(
                        mot.x + look.x * 0.35 * player.zza * mod,
                        mot.y,
                        mot.z + look.z * 0.35 * player.zza * mod);
                mot = player.getDeltaMovement();
            }

            if (player.xxa != 0) {
                look = look.yRot((float) Math.PI * 0.5F);
                player.setDeltaMovement(
                        mot.x + look.x * 0.15 * player.xxa * mod,
                        mot.y,
                        mot.z + look.z * 0.15 * player.xxa * mod);
            }
        }
    }
}
