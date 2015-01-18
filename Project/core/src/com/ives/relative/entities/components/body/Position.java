package com.ives.relative.entities.components.body;

import com.artemis.Component;
import com.ives.relative.entities.components.network.Networkable;
import com.ives.relative.universe.chunks.Chunk;

/**
 * Created by Ives on 12/12/2014.
 * Position component, this component gets synced with the body component
 */
public class Position extends Component implements Networkable {
    public float x, y;
    public float px, py;

    public int z;
    public int pz;

    public float rotation;
    public float protation;

    public transient Chunk chunk;

    public Position() {
    }

    public Position(float x, float y, int z, float rotation) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
    }
}
