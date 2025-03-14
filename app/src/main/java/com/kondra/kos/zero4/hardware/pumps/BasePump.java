/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.hardware.pumps;

import com.kondra.kos.zero4.hardware.Zero4Board;
import com.tccc.kos.commons.util.concurrent.future.FutureWork;
import com.tccc.kos.ext.dispense.Pump;
import com.tccc.kos.ext.dispense.PumpConfig;

import lombok.Getter;

/**
 * Base pump for zero4 board. This simply extends the kOS {@code Pump}
 * class and implements the required time and volume based pour methods.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
abstract public class BasePump extends Pump<PumpConfig> {
    @Getter
    private int pos;    // position of the pump on the board

    public BasePump(Zero4Board board, String name, String category, int pos) {
        super(board, name, category);
        this.pos = pos;
    }

    /**
     * All pumps / valves must support timed pours via {@code tpour()}. While the
     * rate is supplied, fixed rate valves are free to ignore the rate as the
     * caller is expected to understand that the rate will always be the
     * calibrated rate of the valve.
     */
    @Override
    public FutureWork tpour(int duration, double rate) {
        // Simply pass the tpour request to the board the pump is attached to
        return ((Zero4Board)getBoard()).tpour(this, duration, rate);
    }

    /**
     * All pumps / valves must support volume pours via {@code vpour()}. For devices
     * without flow meters, volume pours are typically just converted to time pours
     * using the volume and requested flow rate. For fixed rate valves, the caller
     * is expected to understand the calibrated rate.
     */
    @Override
    public FutureWork vpour(double volume, double rate) {
        // convert the volume pour to a timed pour
        return tpour((int)(volume / rate), rate);
    }
}
