package com.hbm.blockentity.machine.fusion;

/** CE {@code IFusionPowerReceiver}. */
public interface IFusionPowerReceiver {

    boolean receivesFusionPower();

    void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b);
}
