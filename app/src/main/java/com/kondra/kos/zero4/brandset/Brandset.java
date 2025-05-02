/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.brandset;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.tccc.kos.ext.dispense.service.ingredient.BaseIngredient;
import com.tccc.kos.ext.dispense.service.ingredient.IngredientSource;

import lombok.Getter;
import lombok.Setter;

/**
 * A brandset is a common term for all the data that describes available
 * ingredients and beverages, including information required to pour beverages,
 * such as recipes in the case of Zero4 since micro-pumps are variable rate
 * and require per-ingredient rate information.
 * <p>
 * This class is designed to be deserialized directly from a json file so
 * all related objects are setup as typical java beans.
 * <p>
 * kOS comes to know about avilable ingredients through one or more {@code IngredientSource}
 * classes. Since this brandset already contains a list of available ingredients
 * we simply implement this interface so that we can add the brandset directly
 * as an {@code IngredientSource}.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
@Getter @Setter
public class Brandset implements IngredientSource {
    private List<Ingredient> ingredients;  // ingredients in the brandset
    private List<Beverage> beverages;      // beverages in the brandset

    /**
     * Return the recipe parts for the specified beverage.
     */
    public List<RecipePart> getBeverageRecipeParts(String id) {
        Optional<Beverage> bev = beverages.stream().filter(b -> b.getId().equals(id)).findFirst();
        return bev.isPresent() ? bev.get().getRecipe() : Collections.emptyList();
    }

    /**
     * Return the beverage with the specified id
     */
    public Beverage getBeverage(String id) {
        return beverages.stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null);
    }

    /**
     * Part of the {@code IngredientSource} interface which identifies the unique id of
     * the source so that ingredients from different sources can be uniquely identified.
     */
    @Override
    public String getSourceId() {
        return "brandset";
    }

    /**
     * Part of the {@code IngredientSource} interface which returns an ingredient object
     * for the specified id.
     */
    @Override
    public BaseIngredient getIngredient(String id) {
        return ingredients.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
    }
}
