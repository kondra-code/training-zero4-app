/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4;

import com.kondra.kos.zero4.brandset.Ingredient;
import com.kondra.kos.zero4.hardware.Zero4Adapter;
import com.kondra.kos.zero4.hardware.Zero4Board;
import com.kondra.kos.zero4.pour.Zero4PourDelegate;
import com.kondra.kos.zero4.pour.Zero4PourEngine;
import com.tccc.kos.commons.core.context.annotations.Autowired;
import com.tccc.kos.commons.util.json.JsonDescriptor;
import com.tccc.kos.commons.util.resource.ClassLoaderResourceLoader;
import com.tccc.kos.core.app.KosCore;
import com.tccc.kos.core.service.assembly.CoreAssembly;
import com.tccc.kos.core.service.spawn.SpawnService;
import com.tccc.kos.ext.dispense.DispenseAssembly;
import com.tccc.kos.ext.dispense.HolderBuilder;
import com.tccc.kos.ext.dispense.pipeline.beverage.BeverageNozzlePipeline;
import com.tccc.kos.ext.dispense.pipeline.ingredient.IngredientNozzlePipeline;
import com.tccc.kos.ext.dispense.pipeline.ingredient.XmlPumpIntentFactory;
import com.tccc.kos.ext.dispense.service.insertion.InsertionService;
import com.tccc.kos.ext.dispense.service.nozzle.Nozzle;

import lombok.Getter;

/**
 * Assembly class for the Zero4 demo kit dispenser.
 * <p>
 * kOS models hardware in software. This allows kOS to know when physical
 * hardware is missing or non-functional, and automatically raise troubles.
 * By modeling hardware in software, it also allows software to wire
 * listeners together even when the physical hardware is not yet available.
 * Not only does this simplify the process of wiring hardware event handlers
 * but it also allows kOS to fully support hot-swap hardware.
 * <p>
 * An {@code Assembly} is simply a logical container of hardware expected
 * in the device. This class supports lifecycle callbacks similar to
 * applications (load(), start(), started()). These are triggered when
 * the assembly is installed into the device.
 * <p>
 * Devices that allow expansion kits can utilize assemblies to model the
 * expansion hardware and add/remove that hardware atomically. This allows
 * kOS to know when to look for extra hardware, report it missing, and when
 * it has been uninstalled.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
public class Zero4Assembly extends DispenseAssembly implements CoreAssembly {
    @Autowired
    private InsertionService insertionService;    // used to insert ingredients
    @Autowired
    private SpawnService spawnService;            // used to start the Zero4 adapter
    @Getter
    private BeverageNozzlePipeline beveragePipeline;
    private Zero4Board zero4;

    public Zero4Assembly(JsonDescriptor descriptor) throws Exception {
        super("core", descriptor);
    }

    /**
     * Load lifecycle callback. This is responsible for creating any components
     * that need to be added to the assembly. Once this callback returns, any
     * components added by this method will be autowired, initialized and configured
     * before calling {@code start()}, which can then use these fully configured
     * components.
     */
    @Override
    public void load() throws Exception {
        // Create a nozzle for the dispenser and add to the assembly
        Nozzle nozzle = new Nozzle(this, "nozzle");
        addNozzle(nozzle);

        // Create the logical zero4 board and add to the assembly
        zero4 = new Zero4Board(this);
        addBoard(zero4);

        // kOS models how ingredients, pump and nozzles are connected, introducing
        // various logical components along the way which allows kOS to handle a
        // wide variety of configurations, ranging from multiple nozzles, multiple
        // ingredients in a single container, and even multiple pumps connected to
        // a single container but plumbed to different nozzles. The {code HolderBuilder}
        // class provides a simple way to build standard relationships with minimal effort.
        HolderBuilder builder = new HolderBuilder(this, nozzle);
        builder.buildWater(zero4.getWater());
        builder.buildCarb(zero4.getCarb());
        builder.setPumpIterator(zero4.getMicros(), 0, 1);
        builder.setNameIterator("M", 1, 1);
        builder.buildMicros(4, 0);

        // kOS abstracts different ways to pour from a nozzle using nozzle pipelines.
        // For ingredient pouring, the {@code IngredientNozzlePipeline} provides a concept
        // intents, where an intent is a named sequence of low level pump operations that
        // can be run as an atomic operation. This allows complex utility pours to be defined
        // using user-extensible xml files and poured by name. The following creates an intent
        // factory from the contents of intents.xml.
        XmlPumpIntentFactory intentFactory = new XmlPumpIntentFactory();
        intentFactory.addLoader(new ClassLoaderResourceLoader(getClass().getClassLoader()));
        intentFactory.load("intents.xml");

        // By adding an {@code IngredientNozzlePipeline} to a nozzle, any pumps connected to
        // the nozzle can be operated using intents defined in the supplied factory.
        IngredientNozzlePipeline ingredientPipeline = new IngredientNozzlePipeline(intentFactory);
        ingredientPipeline.setDilutionPump(zero4.getWater());
        nozzle.add(ingredientPipeline);

        // kOS treats ingredient and beverage pouring as fundamentally different, allowing
        // troubles to block these methods of pouring independently. The {@code BeverageNozzlePipeline}
        // provides built-in support for encoding recipe data into a beverage graph which automatically
        // computes availability of beverages as well as being able to compute which pumps to enable
        // to pour any particular beverage. By implementing a custom {@code BeveragePourEngine},
        // developers can model virtually any type of beverage pouring.
        Zero4PourEngine engine = new Zero4PourEngine();
        beveragePipeline = new BeverageNozzlePipeline(engine);

        // The beverage pour engine doesn't know about fixed volumes so we need to provide a delegate
        // that returns the max beverage pour volume. This can also be used to return named volumes
        // such as volumes for various cups, but we won't be using this in this tutorial.
        beveragePipeline.setDelegate(new Zero4PourDelegate());

        nozzle.add(beveragePipeline);
    }

    /**
     * Start lifecycle callback, called after {@code load()} returns and all components added to
     * the assembly during {@code load()} have been fully initialized.
     */
    @Override
    public void start() {
        // Load the zero4 adapter if not in the simulator. The adapter is a native program that
        // interfaces to the custom hardware on the Zero4 board and links back to the Zero4Board
        // object in the assembly, allowing the logical board to control the real hardware. The
        // kOS simulator doesn't have this hardware available so we skip starting the adapter when
        // running in the simulator to avoid seeing kOS attempt to restart the adapter in the logs.
        if (!KosCore.isSimulator()) {
            spawnService.addProcess(new Zero4Adapter());
        }
    }

    /**
     * Called after {@code start()} returns and the assembly is fully installed in the device.
     */
    @Override
    public void started() {
        // Water and carb are always connected so we can insert them as intrinsic ingredients.
        // Intrinsics are locked in place and cannot be replaced once installed.
        insertionService.insertIntrinsic(Ingredient.WATER, zero4.getWater().getHolder());
        insertionService.insertIntrinsic(Ingredient.CARB, zero4.getCarb().getHolder());

        // Insert the other ingredients via code just because it's a demo and it's convenient.
        // If these lines are commented out, the user must insert ingredients via api to make
        // them available for pouring. This can be done using the ingredient assignment tool
        // in kOS Studio or by using the corresponding endpoints using a tool such as postman.
        insertionService.insertIntrinsic(Ingredient.LEMON, zero4.getMicros().get(0).getHolder());
        insertionService.insertIntrinsic(Ingredient.LIME, zero4.getMicros().get(1).getHolder());
        insertionService.insertIntrinsic(Ingredient.CHERRY, zero4.getMicros().get(2).getHolder());
        insertionService.insertIntrinsic(Ingredient.TROPICAL, zero4.getMicros().get(3).getHolder());
    }
}
