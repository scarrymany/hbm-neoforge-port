package com.hbm.handler.neutron;

import com.hbm.api.rbmk.IRBMKColumn;
import net.minecraft.core.BlockPos;

/**
 * Wraps one {@link IRBMKColumn} as a cacheable node in the neutron flux graph. CE:
 * {@code com.hbm.handler.neutron.NeutronNode} (27 lines, read in full) wraps a raw
 * {@code TileEntity} (kept generic there to also serve Chicago-pile content via a
 * {@code Map<String, Object>} data bag); this port's version is typed directly against
 * {@link IRBMKColumn} instead, since Pile content is out of scope for this pass (see
 * {@link NeutronStream}'s javadoc) - this is the one place that generalization would need
 * revisiting if Pile content is ever ported.
 */
public abstract class NeutronNode {

    protected final NeutronStream.NeutronKind kind;
    protected final BlockPos pos;
    protected final IRBMKColumn column;

    protected NeutronNode(IRBMKColumn column, NeutronStream.NeutronKind kind) {
        this.column = column;
        this.kind = kind;
        this.pos = column.getRbmkPos();
    }

    public IRBMKColumn getColumn() {
        return column;
    }

    public BlockPos getPos() {
        return pos;
    }

    public NeutronStream.NeutronKind getKind() {
        return kind;
    }
}
