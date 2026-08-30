package com.hbm.blockentity.bomb;

import com.hbm.items.weapon.ItemMissileStandard;
import com.hbm.items.weapon.ItemMissileStandard.MissileFormFactor;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.tileentity.bomb.TileEntityLaunchPadLarge} (322 lines, read in
 * full) - adds the erector/lift animation state machine gating {@code isReadyForLaunch()}: a missile
 * must be raised from horizontal storage ({@code erector} 90&deg;&rarr;0&deg;) and lifted
 * ({@code lift} 1&rarr;0) before it can fire, then both retract afterward. Form-factor-dependent
 * speed halving for {@code ATLAS}/{@code HUGE} missiles preserved exactly.
 * <p>
 * <b>Not ported</b>: CE's {@code AudioWrapper}/{@code getLoopedSound} erector/lift motor loop sound
 * - this port's looped-block-audio bridge is an unverified/not-yet-wired dependency (documented
 * precedent: {@code MachineRefineryBlockEntity}'s own javadoc already drops looped boiler audio for
 * the identical reason). The one-shot stop sounds ({@code wgh_stop}/{@code garage_stop}) are kept
 * since those are plain {@code Level#playSound} calls, not loops. The {@code Tower}/
 * {@code LaunchSmoke} particle effects are dropped for the same {@code HbmEffectNT}-absence reason
 * documented on {@link LaunchPadBlockEntity}.
 */
public class LaunchPadLargeBlockEntity extends LaunchPadBaseBlockEntity {

    public int formFactor = -1;
    /** Whether the missile has already been placed on the launchpad; renders statically once true. */
    public boolean erected = false;
    /** Whether the missile can be lifted; does not render at all if false and not erected. */
    public boolean readyToLoad = false;
    /** Ties the erected transition into the delay so the erector animation isn't cut short. */
    public boolean scheduleErect = false;
    public float lift = 1F;
    public float erector = 90F;
    public float prevLift = 1F;
    public float prevErector = 90F;
    public float syncLift;
    public float syncErector;
    private int sync;
    /** Delay between erector movements. */
    public int delay = 20;

    protected boolean liftMoving = false;
    protected boolean erectorMoving = false;

    public LaunchPadLargeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 7);
    }

    @Override
    public boolean isReadyForLaunch() {
        return this.erected && this.readyToLoad;
    }

    @Override
    public double getLaunchOffset() {
        return 2D;
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (!level.isClientSide) {
            this.prevLift = this.lift;
            this.prevErector = this.erector;

            float erectorSpeed = 1.5F;
            float liftSpeed = 0.025F;

            if (this.isMissileValid() && inventory.getStackInSlot(0).getItem() instanceof ItemMissileStandard missile) {
                this.formFactor = missile.formFactor.ordinal();

                if (missile.formFactor == MissileFormFactor.ATLAS || missile.formFactor == MissileFormFactor.HUGE) {
                    erectorSpeed /= 2F;
                    liftSpeed /= 2F;
                }

                if (this.erector == 90F && this.lift == 1F) {
                    this.readyToLoad = true;
                }
            } else {
                readyToLoad = false;
                erected = false;
                delay = 20;
            }

            if (this.power >= 75_000) {
                if (delay > 0) {
                    delay--;

                    if (delay < 10 && scheduleErect) {
                        this.erected = true;
                        this.scheduleErect = false;
                    }

                    // no missile or the erector hasn't returned to zero yet - retract
                    if (inventory.getStackInSlot(0).isEmpty() || !readyToLoad) {
                        if (erector < 90F) {
                            erector = Math.min(erector + erectorSpeed, 90F);
                            if (erector == 90F) delay = 20;
                        } else if (lift < 1F) {
                            lift = Math.min(lift + liftSpeed, 1F);
                            if (erector == 1F) {
                                readyToLoad = true;
                                delay = 20;
                            }
                        }
                    }
                } else {
                    if (!erected && readyToLoad) {
                        this.state = STATE_LOADING;

                        if (erector != 0F) {
                            erector = Math.max(erector - erectorSpeed, 0F);
                            if (erector == 0F) delay = 20;
                        } else if (lift > 0) {
                            lift = Math.max(lift - liftSpeed, 0F);
                            if (lift == 0F) {
                                scheduleErect = true;
                                delay = 20;
                            }
                        }
                    } else {
                        if (erector < 90F) {
                            erector = Math.min(erector + erectorSpeed, 90F);
                            if (erector == 90F) delay = 20;
                        } else if (lift < 1F) {
                            lift = Math.min(lift + liftSpeed, 1F);
                            if (erector == 1F) {
                                readyToLoad = true;
                                delay = 20;
                            }
                        }
                    }
                }
            }

            if (!this.hasFuel() || !this.isMissileValid()) this.state = STATE_MISSING;
            if (this.erected && this.canLaunch()) this.state = STATE_READY;

            boolean prevLiftMoving = this.liftMoving;
            boolean prevErectorMoving = this.erectorMoving;
            this.liftMoving = this.prevLift != this.lift;
            this.erectorMoving = this.prevErector != this.erector;

            if (prevLiftMoving && !this.liftMoving) {
                level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), HBMSoundHandler.wgh_stop.get(), SoundSource.BLOCKS, 2F, 1F);
            }
            if (prevErectorMoving && !this.erectorMoving) {
                level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), HBMSoundHandler.garage_stop.get(), SoundSource.BLOCKS, 2F, 1F);
            }
        } else {
            this.prevLift = this.lift;
            this.prevErector = this.erector;

            if (this.sync > 0) {
                this.lift = this.lift + ((this.syncLift - this.lift) / (float) this.sync);
                this.erector = this.erector + ((this.syncErector - this.erector) / (float) this.sync);
                --this.sync;
            } else {
                this.lift = this.syncLift;
                this.erector = this.syncErector;
            }
        }

        super.updateEntity();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.liftMoving);
        buf.writeBoolean(this.erectorMoving);
        buf.writeBoolean(this.erected);
        buf.writeBoolean(this.readyToLoad);
        buf.writeByte((byte) this.formFactor);
        buf.writeFloat(this.lift);
        buf.writeFloat(this.erector);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.liftMoving = buf.readBoolean();
        this.erectorMoving = buf.readBoolean();
        this.erected = buf.readBoolean();
        this.readyToLoad = buf.readBoolean();
        this.formFactor = buf.readByte();
        this.syncLift = buf.readFloat();
        this.syncErector = buf.readFloat();

        if (this.lift != this.syncLift || this.erector != this.syncErector) {
            this.sync = 3;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.erected = tag.getBoolean("erected");
        this.readyToLoad = tag.getBoolean("readyToLoad");
        this.lift = tag.getFloat("lift");
        this.erector = tag.getFloat("erector");
        this.formFactor = tag.getInt("formFactor");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("erected", erected);
        tag.putBoolean("readyToLoad", readyToLoad);
        tag.putFloat("lift", lift);
        tag.putFloat("erector", erector);
        tag.putInt("formFactor", formFactor);
    }

    @Override
    public void finalizeLaunch(Entity missile) {
        super.finalizeLaunch(missile);
        this.erected = false;
    }

    @Override
    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + 5, p.getY(), p.getZ() - 2, Direction.EAST),
                new DirPos(p.getX() + 5, p.getY(), p.getZ() + 2, Direction.EAST),
                new DirPos(p.getX() - 5, p.getY(), p.getZ() - 2, Direction.WEST),
                new DirPos(p.getX() - 5, p.getY(), p.getZ() + 2, Direction.WEST),
                new DirPos(p.getX() - 2, p.getY(), p.getZ() + 5, Direction.SOUTH),
                new DirPos(p.getX() + 2, p.getY(), p.getZ() + 5, Direction.SOUTH),
                new DirPos(p.getX() - 2, p.getY(), p.getZ() - 5, Direction.NORTH),
                new DirPos(p.getX() + 2, p.getY(), p.getZ() - 5, Direction.NORTH)
        };
    }
}
