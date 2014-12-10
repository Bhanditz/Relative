package com.ives.relative.entities.components.planet;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.physics.box2d.World;

/**
 * Created by Ives on 4/12/2014.
 */
public class WorldComponent extends Component {
    public transient World world = null;
    public int velocityIterations = 8;
    public int positionIterations = 3;

    public WorldComponent() {
    }

    public WorldComponent(World world, int velocityIterations, int positionIterations) {
        this.world = world;
        this.velocityIterations = velocityIterations;
        this.positionIterations = positionIterations;
    }
}