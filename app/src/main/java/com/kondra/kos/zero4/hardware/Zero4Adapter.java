/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.hardware;

import com.tccc.kos.core.service.spawn.Adapter;

/**
 * Adapter for the Zero4 board.
 * <p>
 * An adapter is a native program that interfaces to an external
 * hardware device or native functionality not easily accessible
 * by java. Adapters are started by java through {@code SpawnService}
 * and connect back to java using the blink network protocol. This
 * allows adapters to run on the current node or even remote nodes
 * and still work with java. Some adapter are started manually,
 * while others are dynamically started and stopped based on hardware
 * detection, such as usb insertions and removals.
 * <p>
 * The {@code Adapter} class is an abstraction of how to run the
 * native adapter program.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
public class Zero4Adapter extends Adapter {
    // name of the adapter binary
    private static final String ADAPTER_NAME = "zero4Adapter";

    /**
     * Create an instance of the zero4 adapter using the
     * standard location of the adapter binary.
     */
    public Zero4Adapter() {
        super(ADAPTER_NAME);
        setBasePath("/usr/bin");
    }
}
