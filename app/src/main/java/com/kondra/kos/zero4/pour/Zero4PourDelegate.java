/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.pour;

import com.tccc.kos.ext.dispense.pipeline.beverage.BeveragePipelineDelegate;

/**
 * kOS provides support for pouring fixed volume beverages. Common examples
 * of fixed volume pours would be various cup sizes or an overall max
 * volume for the device. A {@code BeveragePipelineDelegate} provides a way
 * for kOS to get volume information for various fixed volume pours without
 * needing to know the abstractions around the volumes. That is, kOS doesn't
 * need to know about cups in order to support cup sizes. Simply use the
 * standard fixed volume pour api's that take a named volume and kOS will
 * pass the name to this delegate for conversion to a volume. Since the zero4
 * application doesn't use cup sizes, we only implement the {@code getMaxPourVolume}
 * method so that we have an overall limitation on any given beverage pour.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
public class Zero4PourDelegate implements BeveragePipelineDelegate {

    @Override
    public double getMaxPourVolume() {
        return 350;
    }
}
