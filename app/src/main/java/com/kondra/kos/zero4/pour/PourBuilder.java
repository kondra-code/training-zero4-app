/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.pour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kondra.kos.zero4.brandset.Brandset;
import com.kondra.kos.zero4.brandset.RecipePart;
import com.kondra.kos.zero4.pour.BevPourable.BevDef;
import com.tccc.kos.commons.util.concurrent.future.FailedFuture;
import com.tccc.kos.commons.util.concurrent.future.FutureWork;
import com.tccc.kos.commons.util.concurrent.future.ParallelFuture;
import com.tccc.kos.ext.dispense.Pump;
import com.tccc.kos.ext.dispense.pipeline.beverage.BeveragePourEngine;
import com.tccc.kos.ext.dispense.pipeline.beverage.BeveragePumpEventInitiator;
import com.tccc.kos.ext.dispense.pipeline.beverage.Pourable;
import com.tccc.kos.ext.dispense.pipeline.beverage.RecipeExtractor;

/**
 * Used by the Zero4PourEngine to aggregate pumps, ingredients, rates and so on
 * for the purpose of determining how to pour a given beverage.
 * <p>
 * kOS uses volume based beverage pouring. For devices with flow meters, this data
 * can be fed back into kOS to track actual volumes. For fixed rate valves with no
 * flow meters, kOS will use the calibrated rates to generate simulated volume updates
 * so that all volume related capabilities within kOS are still available to the
 * developer. This means that a typical fixed rate device is actually very simple
 * to beverage pour as it primarily just involves identifying the valves to enable
 * for a given beverage. kOS can determine this automatically from the beverage
 * graph structure. However, since the Zero4 has variable rate micro-pumps, but
 * doesn't receive volume updates from the pumps, we need to compute the overall
 * flow rate of the beverage in order to simulate the volume events correctly.
 * <p>
 * This class also constructs a beverage pour using a collection of timed pours.
 * Typically, production hardware has pouring specific api's for beverages and
 * the pour engine would be designed to interface directly with the capabilities
 * of that hardware.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
public class PourBuilder extends Pourable {
    // reason codes
    private static final String REASON_errInvalidPourable = null;

    private RecipeExtractor extractor;         // extract pumps from bev graph
    private Map<String, PumpInfo> pumpsByIng;  // track pumps by ingredient to accumulate rates
    private double totalRate;                  // total rate of this pour
    private int durationMs;                    // duration of the pour based on vol / rate

    /**
     * Create a new builder with the pourable to pour
     */
    public PourBuilder(Pourable  pourable, BeveragePourEngine<?> engine, Brandset brandset) {
        // Grab the beverage definition from the pourable
        BevDef def = ((BevPourable)pourable).getBevDef();

        // Create recipe extractor to extract the pumps to use for the specified beverage.
        // This performs a downward search in the beverage graph to find available pumps
        // to pour the specified beverage.
        extractor = new RecipeExtractor(engine).addIngredients(def.getBevId());

        // If the extractor found a valid way to pour, setup the pour
        if (extractor.isValid()) {
            pumpsByIng = new HashMap<>();

            // Add all the pumps to a map where we can track their required rates
            for (Pump<?> pump : extractor.getPumps()) {
                pumpsByIng.put(pump.getIngredientId(), new PumpInfo(pump));
            }

            // Add recipe rates to the pumps in the map
            addRates(brandset.getBeverageRecipeParts(def.getBevId()));

            // Compute the total duration of the pour based on the requested volume and the total rate
            durationMs = (totalRate > 0) ? (int)(pourable.getEffectiveVolume() * 1000 / totalRate) : 0;
        }
    }

    /**
     * Add all the specified recipe parts to accumulate the rates.
     */
    private void addRates(List<RecipePart> parts) {
        for (RecipePart part : parts) {
            // Add the rate to the pump
            pumpsByIng.get(part.getIngredientId()).rate += part.getRate();

            // Add the rate to the total
            totalRate += part.getRate();
        }
    }

    /**
     * Build the pour future
     */
    public FutureWork pour(BeveragePumpEventInitiator initiator) {
        // If can't be poured, return a failed future
        if (!extractor.isValid()) {
            return new FailedFuture("bev-pour", REASON_errInvalidPourable);
        }

        // Build the pour from a bunch of tpours
        List<Pump<?>> pumps = new ArrayList<>();
        ParallelFuture future = new ParallelFuture("bev-pour");
        for (PumpInfo info : pumpsByIng.values()) {
            pumps.add(info.pump);
            future.add(info.pump.tpour(durationMs, info.rate));
        }

        // Add pumps to the initiator to generate required kOS pump events
        initiator.setPumps(pumps);

        return future;
    }

    /**
     * Class to accumulate rate information by pump
     */
    private class PumpInfo {
        private Pump<?> pump;   // the pump that is part of the pour
        private double rate;    // the total rate to use for this pump

        PumpInfo(Pump<?> pump) {
            this.pump = pump;
        }
    }
}
