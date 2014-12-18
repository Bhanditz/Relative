package com.ives.relative.entities.components.planet;

import com.artemis.Component;
import com.ives.relative.entities.components.network.Networkable;

/**
 * Created by Ives on 10/12/2014.
 */
public class Seed extends Component implements Networkable {
    public String seed;

    public Seed() {
    }

    public Seed(String seed) {
        this.seed = seed;
    }
}
