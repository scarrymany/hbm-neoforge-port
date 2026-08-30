package com.hbm.blockentity;

import net.minecraft.world.entity.Entity;

/**
 * Ported from CE's {@code com.hbm.tileentity.IRadarCommandReceiver} (a trivial 2-method interface).
 * Implemented by {@link com.hbm.blockentity.bomb.LaunchPadBaseBlockEntity} per
 * {@code docs/phase3/missile_launch_infra.md}'s Key design/API decisions ("costs nothing to add even
 * before the radar console exists, since {@code TileEntityLaunchPadBase} already needs to implement
 * it to compile"). Its only real consumer in CE, {@code TileEntityMachineRadarNT} (an unported radar
 * console block entity sending remote fire commands over the radar-command network), is out of this
 * package's scope - the actual "fire this launch pad from a radar screen" feature waits on that
 * console, not on this interface.
 */
public interface IRadarCommandReceiver {

    boolean sendCommandPosition(int x, int y, int z);

    boolean sendCommandEntity(Entity target);
}
