package com.ives.relative.network.packets.updates;

import com.artemis.Component;
import com.badlogic.gdx.utils.Array;
import com.ives.relative.managers.NetworkManager;
import com.ives.relative.network.packets.UpdatePacket;

/**
 * Created by Ives on 18/12/2014.
 *
 * Gets sent to the client, the client converts it back to an entity.
 */
public class CreateEntityPacket extends UpdatePacket {
    public Component[] components;
    public boolean delta;
    public NetworkManager.Type type;

    public CreateEntityPacket() {
    }

    public CreateEntityPacket(Array<Component> components, int id, boolean delta, int sequence) {
        super(sequence, id);
        this.components = new Component[components.size];
        for (int i = 0; i < components.size; i++) {
            this.components[i] = components.get(i);
        }
        this.delta = delta;
    }

    public CreateEntityPacket(Array<Component> components, int id, boolean delta, int sequence, NetworkManager.Type type) {
        super(sequence, id);
        this.components = new Component[components.size];
        for (int i = 0; i < components.size; i++) {
            this.components[i] = components.get(i);
        }
        this.delta = delta;
        this.type = type;
    }
}
