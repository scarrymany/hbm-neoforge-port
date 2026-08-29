package com.hbm.api.block;

public interface IMiningDrill {

    /**
     * What era the drill belongs to, usually for IDrillInteraction to adjust outputs
     */
    DrillType getDrillTier();

    /**
     * An arbitrary "rating" of the drill. Hand powered pre-industrial drills would be &lt;10, the auto mining drill is 50 and the laser miner is 100.
     */
    int getDrillRating();

    enum DrillType {
        PRIMITIVE,
        INDUSTRIAL,
        HITECH
    }
}
