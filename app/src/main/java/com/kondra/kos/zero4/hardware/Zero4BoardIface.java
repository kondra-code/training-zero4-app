/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.hardware;

import java.io.IOException;

import com.tccc.kos.commons.core.service.blink.binarymsg.BinaryMsg;
import com.tccc.kos.commons.core.service.blink.binarymsg.BinaryMsgIface;
import com.tccc.kos.commons.core.service.blink.binarymsg.BinaryMsgSession;
import com.tccc.kos.commons.util.convert.Convert;

/**
 * Iface that provides access to the Zero4 board hardware via adapter.
 * <p>
 * An adapter is a native program that interfaces to hardware or native
 * services and connects back to java using a blink connection. When an
 * adapter connects to java an iface is created for the connection, allowing
 * messages to be sent back and forth to the native code adapter.
 * <p>
 * The Zero4 demo board only supports starting and stopping pumps. It's
 * common that ifaces support both command / response requests as well
 * as real-time events from adapters.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
public class Zero4BoardIface extends BinaryMsgIface {
    // name of this interface
    public static final String NAME = "tier1.zero4";

    // api numbers for the protocol
    private static final int API_PUMP = 2;

    public Zero4BoardIface(BinaryMsgSession session) {
        super(NAME, session, null);
    }

    /**
     * Start a pump.
     *
     * @param pos          0-3 are micros, 4/5 are macros
     * @param rate         rate of to pour (ignored by macros)
     * @param durationMs   how long to pour
     */
    public void startPump(int pos, double rate, int durationMs) throws IOException {
        BinaryMsg msg = msg(API_PUMP);
        msg.writeInt(pos);
        msg.writeInt(Convert.toQ8(rate));
        msg.writeInt(durationMs);
        sendAndRecv(msg);
    }

    /**
     * Stop a pump.
     *
     * @param pos   0-3 are micros, 4/5 are macros
     */
    public void stopPump(int pos) throws IOException {
        // stop the pump by sending a zero rate and duration
        startPump(pos, 0, 0);
    }
}
