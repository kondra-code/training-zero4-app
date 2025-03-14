/**
 * (C) Copyright 2025, TCCC, All rights reserved.
 */
package com.kondra.kos.zero4.pour;

import java.io.IOException;
import com.tccc.kos.commons.util.KosUtil;
import com.tccc.kos.ext.dispense.pipeline.beverage.Pourable;

import lombok.Getter;
import lombok.Setter;

/**
 * {@code Pourable} implementation for Zero4 that defines a beverage using
 * a beverage id.
 * <p>
 * kOS abstracts the concept of what to pour into a {@code Pourable}. A pourable
 * is created using a user-defined definition string. This can be anything from
 * a beverage id to an entire custom recipe. It is entirely up to the developer
 * to define the semantics of pouring and kOS wraps this implementation into
 * higher level api's and functionality.
 *
 * @author David Vogt
 * @version 2025-03-13
 */
public class BevPourable extends Pourable {
    @Getter
    private BevDef bevDef;             // the definition of what to pour

    /**
     * Create a new pourable from the specified definition string. We expect
     * the string to be a json encoding of a {@code BevDef} object. This could
     * just as easily be a beverage id string, but by wrapping it into an object,
     * we can extend the object later to include additional pour options, such
     * as beverage plus flavors.
     */
    public BevPourable(String definitionStr) throws IOException {
        // Parse the definition using the built-in jackson mapper
        bevDef = KosUtil.getMapper().readValue(definitionStr, BevDef.class);
    }

    @Override
    public Object getDefinition() {
        return bevDef;
    }

    /**
     * A class that represents how the beverage is specified via the
     * kOS select beverage endpoint.
     */
    @Getter @Setter
    public static class BevDef {
        private String bevId;      // beverage id
    }
}
