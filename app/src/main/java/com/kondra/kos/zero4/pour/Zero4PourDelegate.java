package com.kondra.kos.zero4.pour;

import com.tccc.kos.ext.dispense.pipeline.beverage.BeveragePipelineDelegate;

public class Zero4PourDelegate implements BeveragePipelineDelegate {

    @Override
    public double getMaxPourVolume() {
        return 350;
    }

}
