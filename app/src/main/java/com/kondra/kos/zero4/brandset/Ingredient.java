/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.brandset;

import com.tccc.kos.ext.dispense.service.ingredient.BaseIngredient;

/**
 * Ingredient in the Zero4 brandset. Since we don't need any additional
 * properties, this class simply extends {@code BaseIngredient} and
 * defines some constants for our ingredient id's. These constants
 * are simply for convenience so we can hard code the assignment of
 * ingredients to pumps for this demo.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
public class Ingredient extends BaseIngredient {
    // well defined ingredient id's
    public static final String WATER    = "water";
    public static final String CARB     = "carb";
    public static final String LEMON    = "lemon";
    public static final String CHERRY   = "cherry";
    public static final String LIME     = "lime";
    public static final String TROPICAL = "tropical";
}
