/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.hardware;

import java.util.ArrayList;
import java.util.List;

import com.kondra.kos.zero4.hardware.pumps.BasePump;
import com.kondra.kos.zero4.hardware.pumps.MacroPump;
import com.kondra.kos.zero4.hardware.pumps.MicroPump;
import com.tccc.kos.commons.util.KosUtil;
import com.tccc.kos.commons.util.concurrent.future.FutureEvent;
import com.tccc.kos.commons.util.concurrent.future.FutureWork;
import com.tccc.kos.core.service.assembly.Assembly;
import com.tccc.kos.core.service.hardware.BoardIfaceLink;
import com.tccc.kos.core.service.hardware.HardwareLink;
import com.tccc.kos.ext.dispense.Pump;
import com.tccc.kos.ext.dispense.PumpBoard;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Zero4 board abstraction.
 * <p>
 * kOS models hardware using {@code Board} classes. The kOS dispense extension,
 * which introduces support for pumps, nozzlels, etc... has an abstraction
 * for boards which manage pumps called {@code PumpBoard}. Since the Zero4
 * board contains support for both micros and macros, we model the board as
 * a {@code PumpBoard}.
 * <p>
 * This class interfaces directly to the Zero4 native code adapter using
 * {@code Zero4BoardIface}. This iface becomes available when the adapter starts
 * and connects via blink, and goes away when the adapter exits. External
 * software can simply interact with this board class and the class will check
 * if the iface is connected or not and behave accordingly.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
@Slf4j
@Getter
public class Zero4Board extends PumpBoard {
    // reason codes
    private static final String REASON_errNotConnected = "errNotConnected";

    private MacroPump water;        // plain water macro
    private MacroPump carb;         // carb water macro
    private List<Pump<?>> micros;   // micro pumps
    private Zero4BoardIface iface;  // iface to the native adapter that controls hardware

    /**
     * Create a new Zero4 board.
     */
    public Zero4Board(Assembly assembly) {
        super(assembly, "zero4");

        // Create water valves
        carb = new MacroPump(this, "carb", null, 4);
        water = new MacroPump(this, "water", null, 5);

        // Create micros
        micros = new ArrayList<>();
        for (int i=0; i<4; i++) {
            micros.add(new MicroPump(this, "micro" + (i+1), null, i));
        }
    }

    /**
     * Called by pump objects to start a time based pour. All pumps (or valve)
     * in kOS support both time and volume based pours. For simple valves, the
     * calibrated rate of the valve can be used to convert from volume to time.
     * <p>
     * Pours always return {@class FutureWork}, which as an async primitive in
     * kOS. This allows the pour to be started when the future is started. The
     * future will also track progress, estimated time, and automatically generate
     * update events for external systems such as UI code. Futures can also be
     * cancelled or aborted.
     */
    public FutureWork tpour(BasePump pump, int duration, double rate) {
        // Create a new future that will perform the requested pour
        FutureWork future = new FutureWork("tpour-" + pump.getName(), f -> {
            // If there is no iface, we can't turn the pump on
            if (iface == null) {
                f.fail(REASON_errNotConnected);
            } else {
                // Use the iface to run the pump for the specified duration.
                // Since the simple interface to the Zero4 board doesn't send
                // back pump status, use a timer to indicate that the future
                // is complete when the duration is complete. A more robust
                // implementation would send pump status back over the iface.
                log.info("start: {}", pump.getName());
                iface.startPump(pump.getPos(), rate, duration);
                KosUtil.scheduleCallback(() -> f.success(), duration);
            }
        });

        // Add a cancel event handler to the future
        future.append("cancel", FutureEvent.CANCEL, f -> {
            // If cancelled, use the iface to stop the pump
            log.info("cancel: {}", pump.getName());
            if (iface != null) {
                iface.stopPump(pump.getPos());
            }
        });

        // Add a complete event handler to the future
        future.append("stop",  FutureEvent.COMPLETE, f -> {
            // When the future is complete, regardless if how it ended, log
            // that the pump has stopped
            log.info("stop: {}", pump.getName());
        });

        return future;
    }

    /**
     * Part of the {@class Board} class. This defines the type of this board
     * and allows kOS to match incoming adapter connections with this instance.
     */
    @Override
    public String getType() {
        return "tier1.zero4";
    }

    /**
     * Part of the {@class Board} class. Since it's possible to have multiple
     * boards of the same type in a device, different instances of the board
     * are identified by unique instance id's. Since we only have a single
     * Zero4 board in this project, we don't need an instance id.
     */
    @Override
    public String getInstanceId() {
        return null;
    }

    /**
     * Called when an adapter for the actual hardware connects to java.
     * This links the iface to the {@class Board} instance and indicates
     * that the instance is now able to communicate directly to hardware.
     */
    @Override
    public void onLink(HardwareLink link) {
        // grab the iface from the link
        if (link instanceof BoardIfaceLink blink) {
            iface = new Zero4BoardIface(blink.getSession());
        }
    }

    /**
     * Called when an adapter shuts down and the connection to hardware
     * is lost. This indicates that the iface to the actual hardware is
     * no longer usable.
     */
    @Override
    public void onUnlink(HardwareLink link) {
        // remove the iface as session is gone
        iface = null;
    }
}
