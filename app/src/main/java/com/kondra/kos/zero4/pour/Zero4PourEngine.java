/**
 * (C) Copyright 2025, Kondra, All rights reserved.
 */
package com.kondra.kos.zero4.pour;

import com.kondra.kos.zero4.Zero4App;
import com.kondra.kos.zero4.brandset.Beverage;
import com.kondra.kos.zero4.brandset.Brandset;
import com.kondra.kos.zero4.brandset.RecipePart;
import com.kondra.kos.zero4.pour.BevPourable.BevDef;
import com.tccc.kos.commons.core.context.annotations.Autowired;
import com.tccc.kos.commons.util.concurrent.future.FailedFuture;
import com.tccc.kos.commons.util.concurrent.future.FutureWork;
import com.tccc.kos.commons.util.concurrent.future.ParallelFuture;
import com.tccc.kos.commons.util.concurrent.future.SequencedFuture;
import com.tccc.kos.ext.dispense.pipeline.beverage.BeveragePourEngine;
import com.tccc.kos.ext.dispense.pipeline.beverage.BeveragePourEngineConfig;
import com.tccc.kos.ext.dispense.pipeline.beverage.BeveragePourSequence;
import com.tccc.kos.ext.dispense.pipeline.beverage.Pourable;
import com.tccc.kos.ext.dispense.pipeline.beverage.RecipeExtractor;
import com.tccc.kos.ext.dispense.pipeline.beverage.graph.BevGraphBuilder;
import com.tccc.kos.ext.dispense.pipeline.beverage.graph.BeverageNode;

/**
 * Pour engine for the Zero4 demo dispenser.
 * <p>
 * The primary purpose of a pour engine is to build a beverage graph
 * and convert a {@code Pourable} into something that can be poured.
 * <p>
 * kOS abstracts the relationship of pumps, ingredients and beverages
 * into a graph of dependencies called a beverage graph. The graph
 * captures which ingredients are connected to which pumps, how
 * ingredients are related into beverages and so on. By describing
 * these relationships, kOS can automatically provide availability
 * information and even compute how to pour a beverage. Simple changes
 * to the graph also allows multiple copies of the same ingredient,
 * ingredient synonyms, beverages + flavors and many other capabilities.
 * In this particular case, we construct a very basic graph to link
 * pumps to ingredients to beverages.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
public class Zero4PourEngine extends BeveragePourEngine<BeveragePourEngineConfig> {
    @Autowired
    private Zero4App app; // access to the brandset

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    /**
     * This is called by kOS any time the fundamental building blocks of the
     * beverage graph change. For example, assigning an ingredient to a pump
     * makes the new ingredient available to pour, thus the graph is rebuilt
     * to include the new ingredient.
     */
    @Override
    public void rebuildGraph(BevGraphBuilder builder) {
        // Add ingredient nodes for all the pumps. This is a convenience
        // method to build the first couple layers of the beverage graph
        builder.addIngredientNodes();

        // Get the brandset from the app
        Brandset brandset = app.getBrandset();

        // Incorporate all the beverages from the brandset into the graph
        for (Beverage bev : brandset.getBeverages()) {
            // Add the beverage node to the graph
            builder.addBeverage(new BeverageNode(bev.getId()).setNote(bev.getName()));

            // Link the beverage node to ingredient nodes using the recipe data
            for (RecipePart part : bev.getRecipe()) {
                builder.addDependency(bev.getId(), part.getIngredientId());
            }
        }
    }

    /**
     * Given a {@code Pourable} definition string, return a new pourable. This
     * allows the implementation to completely abstract how beverages are selected
     * and poured, allowing kOS to provide standard functionality while delegating
     * details the pour engine.
     */
    @Override
    public Pourable getPourable(String definitionStr) throws Exception {
        return new BevPourable(definitionStr);
    }

    /**
     * Return true if the specified pourable can still be poured. This is used
     * by kOS to check if a pourable is still valid for use.
     */
    @Override
    public boolean isPourable(Pourable pourable) {
        // Grab the beverage definition from the pourable
        BevDef def = ((BevPourable) pourable).getBevDef();

        // Pourable if the beverage node is available
        return isAvailable(def.getBevId());
    }

    /**
     * Given a pourable, return a future that can pour the beverage.
     */
    @Override
    protected FutureWork buildFuture(BeveragePourSequence seq, Pourable pourable) {
        // Grab the beverage definition from the pourable
        BevDef def = ((BevPourable)pourable).getBevDef();

        // Create recipe extractor to extract the pumps to use for the specified beverage.
        // This performs a downward search in the beverage graph to find available pumps
        // to pour the specified beverage.
        RecipeExtractor extractor = new RecipeExtractor(this).addIngredients(def.getBevId());

        // If the extractor didn't find a way to pour, return an error
        if (!extractor.isValid()) {
            return new FailedFuture("bev-pour", "errUnavailable");
        }

        // Get the beverage from the brandset
        Beverage bev = app.getBrandset().getBeverage(def.getBevId());

        // Compute duration of pour from volume
        int durationMs = (int)((pourable.getEffectiveVolume() / bev.getRate()) * 1000);

        // sequenced future for the pour
        SequencedFuture seqFuture = new SequencedFuture("pour");

        // first step is to mark all the pumps as started
        seqFuture.add(new FutureWork("startPumps", f -> {
            startPumps(extractor.getPumps(), pourable);
            f.success();
        }));

        // Parallel future to run all the pumps concurrently
        ParallelFuture pourFuture = new ParallelFuture("bev-pour");
        for (RecipePart part : bev.getRecipe()) {
            pourFuture.add(extractor.getPumpForIngredient(part.getIngredientId()).tpour(durationMs, part.getRate()));
        }
        seqFuture.add(pourFuture);

        // Return the future for the pour
        return seqFuture;
    }
}
