package com.ives.relative.entities.components.body;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.physics.box2d.Body;

/**
 * Created by Ives on 2/12/2014.
 */
public class BodyComponent extends Component {
    public String worldID;
    public int z;
    public transient Body body = null;

    public BodyComponent() {
    }

    public BodyComponent(Body body, int z, String worldID) {
        this.body = body;
        this.z = z;
        this.worldID = worldID;
    }
}