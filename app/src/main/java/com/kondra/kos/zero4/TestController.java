/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4;

import com.tccc.kos.commons.core.context.annotations.Autowired;
import com.tccc.kos.commons.core.dispatcher.annotations.ApiController;
import com.tccc.kos.commons.core.dispatcher.annotations.ApiEndpoint;
import com.tccc.kos.commons.core.dispatcher.annotations.ApiEndpoint.Param;
import com.tccc.kos.commons.core.dispatcher.annotations.HandleVariable;
import com.tccc.kos.commons.core.service.trouble.TroubleService;
import com.tccc.kos.ext.dispense.Pump;
import com.tccc.kos.ext.dispense.PumpTrouble;

/**
 * Test controller to block / unblock ingredients
 *
 * @author David Vogt (david@kondra.com)
 * @version 2025-05-04
 */
@ApiController(base = "/test",
        title = "Test service",
        desc = "Interact with zero4 internals for testing.")
public class TestController {
    @Autowired
    private TroubleService troubleService;

    @ApiEndpoint(GET = "/blockPump/{pump}",
            desc = "Block pouring on the specified pump by creating a block trouble.",
            params = @Param(name = "pump", desc = "Path of the pump to block."))
    public void blockPump(@HandleVariable("pump") Pump<?> pump) {
        troubleService.add(new BlockTrouble(pump));
    }

    @ApiEndpoint(GET = "/unblockPump/{pump}",
            desc = "Unblock pouring on the specified pump by removing the block trouble.",
            params = @Param(name = "pump", desc = "Path of the pump to unblock."))
    public void unblockPump(@HandleVariable("pump") Pump<?> pump) {
        troubleService.removeTroubles(t -> (t instanceof BlockTrouble) && t.isImpacted(pump));
    }

    /**
     * Trouble class for a pump that blocks beveage pouring
     */
    private class BlockTrouble extends PumpTrouble {
        public BlockTrouble(Pump<?> pump) {
            super(pump);
            blockBeveragePour();
        }
    }
}
