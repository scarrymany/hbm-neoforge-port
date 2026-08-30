package com.hbm.blockentity.turret;

import com.hbm.items.weapon.sedna.BulletConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityTurretFriendly} (extends {@code TileEntityTurretChekhov} in
 * CE too - a faster-firing 5.56mm variant of the same chassis). Ammo: CE's
 * {@code XFactory556mm.r556_sp}/{@code fmj}/{@code jhp}/{@code ap} - not ported yet.
 */
public class TurretFriendlyBlockEntity extends TurretChekhovBlockEntity {

    public TurretFriendlyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        // TODO(phase3-gun-content): CE uses XFactory556mm.r556_sp/fmj/jhp/ap.
        return Collections.emptyList();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretFriendly");
    }

    @Override
    public int getDelay() {
        return 5;
    }
}
