package com.ives.relative.entities.components.body;

import com.artemis.Component;
import com.ives.relative.entities.components.network.Networkable;

/**
 * Created by Ives on 12/12/2014.
 */
public class Velocity extends Component implements Networkable {
    public float vx, vy;

    public Velocity() {
    }

    public Velocity(float vx, float vy) {

        this.vx = vx;
        this.vy = vy;
    }
}
