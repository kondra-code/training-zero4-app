/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.hardware.pumps;

import com.kondra.kos.zero4.hardware.Zero4Board;

/**
 * Macro pump that can only pour fixed rate.
 * <p>
 * This subclass simply overrides the type of the pump. The type
 * is used to determine compatible pump intents and is typically
 * used by the UI to determine pump capabilities.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
public class MacroPump extends BasePump {

    public MacroPump(Zero4Board board, String name, String category, int pos) {
        super(board, name, category, pos);
    }

    @Override
    public String getType() {
        return "valve";
    }
}
