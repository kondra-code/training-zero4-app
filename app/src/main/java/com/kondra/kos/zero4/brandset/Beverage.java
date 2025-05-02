/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.brandset;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * A pourable beverage. A beveage is defined by a recipe which
 * combines ingredients together. Since the Zero4 has variable
 * rate pumps, these recipe parts also include flow rate information
 * for each of the ingredients.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
@Getter @Setter
public class Beverage {
    private String id;               // unique id of the beverage
    private String name;             // display name of the beverage
    private double rate;             // overall rate of the beverage
    private List<RecipePart> recipe; // recipe to pour the beverage
}
