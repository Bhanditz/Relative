package com.ives.relative.systems.client;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.systems.IntervalEntitySystem;
import com.artemis.utils.ImmutableBag;
import com.artemis.utils.IntBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.ives.relative.core.client.ClientManager;
import com.ives.relative.core.client.ClientNetwork;
import com.ives.relative.core.client.Player;
import com.ives.relative.entities.commands.ClickCommand;
import com.ives.relative.entities.commands.Command;
import com.ives.relative.entities.commands.DoNothingCommand;
import com.ives.relative.entities.components.body.Location;
import com.ives.relative.entities.components.body.Physics;
import com.ives.relative.entities.components.body.Velocity;
import com.ives.relative.entities.components.network.NetworkC;
import com.ives.relative.entities.events.EntityEvent;
import com.ives.relative.entities.events.EntityEventObserver;
import com.ives.relative.entities.events.position.MovementEvent;
import com.ives.relative.entities.events.position.StoppedMovementEvent;
import com.ives.relative.managers.AuthorityManager;
import com.ives.relative.managers.CommandManager;
import com.ives.relative.managers.NetworkManager;
import com.ives.relative.managers.event.EventManager;
import com.ives.relative.network.Network;
import com.ives.relative.network.packets.BasePacket;
import com.ives.relative.network.packets.UpdatePacket;
import com.ives.relative.network.packets.handshake.planet.ChunkPacket;
import com.ives.relative.network.packets.input.CommandClickPacket;
import com.ives.relative.network.packets.input.CommandPressPacket;
import com.ives.relative.network.packets.requests.RequestEntity;
import com.ives.relative.network.packets.updates.CreateEntityPacket;
import com.ives.relative.network.packets.updates.DeltaPositionPacket;
import com.ives.relative.network.packets.updates.GrantEntityAuthority;
import com.ives.relative.network.packets.updates.PositionPacket;
import com.ives.relative.universe.UniverseSystem;

import java.util.Iterator;

/**
 * Created by Ives on 13/12/2014.
 * This is the system which handles networked commands and movement. This system communicates with the server and updatees
 * every entity accordingly.
 */
@Wire
public class ClientNetworkSystem extends IntervalEntitySystem implements EntityEventObserver {
    public static float CLIENT_NETWORK_INTERVAL = 1 / 60f;
    protected ClientManager clientManager;
    protected CommandManager commandManager;
    protected NetworkManager networkManager;
    protected AuthorityManager authorityManager;

    protected NetworkReceiveSystem networkReceiveSystem;
    protected UniverseSystem universeSystem;

    protected ComponentMapper<Location> mPosition;
    protected ComponentMapper<Velocity> mVelocity;
    protected ComponentMapper<Physics> mPhysics;

    int sequence;
    int frame = 0;
    private Array<Integer> requestedEntities;
    private Array<Integer> grantedEntities;
    private Array<Integer> entitiesToSend;

    public ClientNetworkSystem(ClientNetwork network) {
        super(Aspect.getAspectForAll(NetworkC.class, Location.class), CLIENT_NETWORK_INTERVAL);
        requestedEntities = new Array<Integer>();
        grantedEntities = new Array<Integer>();
        entitiesToSend = new Array<Integer>();

        processRequests(network);
    }

    @Override
    protected void initialize() {
        super.initialize();
        world.getManager(EventManager.class).addObserver(this);
    }

    public void sendDownCommand(Command command) {
        if (command instanceof DoNothingCommand)
            return;

        clientManager.network.sendObjectUDP(ClientNetwork.CONNECTION_ID, new CommandPressPacket(sequence, Player.NETWORK_ID, commandManager.getID(command), true));
        sequence++;
    }

    public void sendClickCommand(ClickCommand clickCommand) {
        clientManager.network.sendObjectUDP(ClientNetwork.CONNECTION_ID, new CommandClickPacket(sequence, Player.NETWORK_ID, commandManager.getID(clickCommand), true, clickCommand.getWorldPosClicked()));
        sequence++;
    }

    /**
     * Sends the up command to the server
     *
     * @param command just the command which needs to be sent, can be a dummy command if needed
     */
    public void sendUpCommand(Command command) {
        if (command instanceof DoNothingCommand)
            return;
        clientManager.network.sendObjectUDP(ClientNetwork.CONNECTION_ID, new CommandPressPacket(sequence, Player.NETWORK_ID, commandManager.getID(command), false));
        sequence++;
    }

    public void sendUnClickCommand(ClickCommand clickCommand) {
        clientManager.network.sendObjectUDP(ClientNetwork.CONNECTION_ID, new CommandClickPacket(sequence, Player.NETWORK_ID, commandManager.getID(clickCommand), false, clickCommand.getWorldPosClicked()));
    }

    public Entity getPlayer() {
        return networkManager.getEntity(Player.NETWORK_ID);
    }

    /**
     * Sends the input of the player to the server, also puts it in a local variable to know how to apply Server
     * Reconciliation.
     *
     * @param entities Entities to process
     */
    @Override
    protected void processEntities(IntBag entities) {
        frame++;

        Iterator<Integer> it = entitiesToSend.iterator();
        while (it.hasNext()) {
            int id = it.next();
            if (id == Player.NETWORK_ID || frame % 10 == 0) {
                Entity e = networkManager.getEntity(id);
                PositionPacket positionPacket = new PositionPacket(e, sequence, id);
                clientManager.network.sendObjectUDP(ClientNetwork.CONNECTION_ID, positionPacket);
                it.remove();
            }
        }
    }

    public void processRequests(final Network network) {
        network.endPoint.addListener(new Listener() {
            @Override
            public void received(final Connection connection, final Object object) {
                if (object instanceof UpdatePacket) {
                    if (object instanceof PositionPacket) {
                        Gdx.app.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                PositionPacket packet = (PositionPacket) object;
                                if (!processPosition(packet)) {
                                    network.sendObjectTCP(ClientNetwork.CONNECTION_ID, new RequestEntity(packet.entityID));
                                    requestedEntities.add(packet.entityID);
                                }
                            }
                        });
                    }
                    if (object instanceof DeltaPositionPacket) {
                        Gdx.app.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                processDeltaPosition((DeltaPositionPacket) object);
                            }
                        });
                    }

                    if (object instanceof CreateEntityPacket) {
                        CreateEntityPacket packet = (CreateEntityPacket) object;
                        networkReceiveSystem.addDataForProcessing(packet);
                        requestedEntities.removeValue(packet.entityID, true);
                    }
                }

                if (object instanceof ChunkPacket) {
                    networkReceiveSystem.addDataForProcessing((BasePacket) object);
                }

                if (object instanceof GrantEntityAuthority) {
                    //If the entity isn't loaded yet
                    processAuthority((GrantEntityAuthority) object);
                }
            }
        });
    }

    public void processAuthority(GrantEntityAuthority packet) {
        Entity entity = networkManager.getEntity(packet.entity);
        System.out.println(packet.entity);
        if (entity != null) {
            grantedEntities.add(packet.entity);
        }
    }

    public boolean processPosition(PositionPacket packet) {
        Entity entity = networkManager.getEntity(packet.entityID);
        //networkEntity.edit().add(position).add(velocity);
        if (entity != null) {
            float x = packet.x;
            float y = packet.y;
            float vx = packet.vx;
            float vy = packet.vy;
            float rotation = packet.rotation;
            float rVelocity = packet.vr;

            Location localPosition = mPosition.get(entity);
            Physics physics = mPhysics.get(entity);

            Body body = physics.body;
            Vector2 bodyPos = body.getTransform().getPosition();
            if (bodyPos.x != x || bodyPos.y != y) {
                body.setTransform(x, y, rotation);
                localPosition.space = universeSystem.getSpace(packet.universeBody);
            }

            Vector2 bodyVel = body.getLinearVelocity();
            if (bodyVel.x != vx || bodyVel.y != vy) {
                body.setLinearVelocity(vx, vy);
            }

            body.setAngularVelocity(rVelocity);

            return true;
        }
        return false;
    }

    public boolean processDeltaPosition(DeltaPositionPacket packet) {
        Entity e = networkManager.getEntity(packet.entityID);
        if (e != null) {
            float dx = packet.dx;
            float dy = packet.dy;

            Location p = mPosition.get(e);
            Physics physics = mPhysics.get(e);

            Body body = physics.body;
            body.applyLinearImpulse(new Vector2(dx * body.getMass() * 4, dy * body.getMass() * 4), body.getPosition(), true);
            return true;
        }
        return false;
    }

    @Override
    public void onNotify(EntityEvent event) {
        if (event instanceof MovementEvent) {
            int id = networkManager.getNetworkID(event.entity);
            if (authorityManager.isEntityAuthorizedByPlayer(ClientNetwork.CONNECTION_ID, event.entity)) {
                if (!entitiesToSend.contains(id, true)) {
                    entitiesToSend.add(id);
                }
            }
        } else if (event instanceof StoppedMovementEvent) {
            int id = networkManager.getNetworkID(event.entity);
            if (authorityManager.isEntityAuthorizedByPlayer(ClientNetwork.CONNECTION_ID, event.entity)) {
                if (!entitiesToSend.contains(id, true)) {
                    entitiesToSend.add(id);
                }
            }
        }
    }
}
