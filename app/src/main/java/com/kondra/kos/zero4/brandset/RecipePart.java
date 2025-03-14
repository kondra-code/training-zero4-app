/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.brandset;

import lombok.Getter;
import lombok.Setter;

/**
 * Part of a recipe that describes a single ingredient and rate.
 * A typical fixed-flow recipe has no need for rate information
 * so a recipe would typically just be a list of ingredient id's.
 * However, since the Zero4 controls variable-rate micro pumps,
 * each ingredient in a recipe requires a corresponding rate.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
@Getter @Setter
public class RecipePart {
    private String ingredientId;   // id of the ingredient
    private double rate;           // rate the ingredient should pour in ml/s
}
