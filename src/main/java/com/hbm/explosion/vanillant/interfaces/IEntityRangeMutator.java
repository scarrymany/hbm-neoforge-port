package com.hbm.explosion.vanillant.interfaces;

import com.hbm.explosion.vanillant.ExplosionVNT;

/**
 * CE: {@code IEntityRangeMutator}. Lets an {@code EntityProcessorStandard}/{@code EntityProcessorCross}
 * scale the entity-search/damage range independently of the block-allocation size (e.g.
 * {@code withRangeMod(2F)} for antimatter's wider blast radius). Signature identical to CE's.
 */
public interface IEntityRangeMutator {

    float mutateRange(ExplosionVNT explosion, float range);
}
