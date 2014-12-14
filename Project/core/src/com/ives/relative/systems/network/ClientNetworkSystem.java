package com.ives.relative.systems.network;

import com.artemis.Aspect;
import com.artemis.Entity;
import com.artemis.annotations.Wire;
import com.artemis.systems.IntervalEntitySystem;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.ives.relative.core.client.ClientManager;
import com.ives.relative.core.client.ClientNetwork;
import com.ives.relative.entities.commands.Command;
import com.ives.relative.entities.components.body.Physics;
import com.ives.relative.entities.components.body.Position;
import com.ives.relative.entities.components.body.Velocity;
import com.ives.relative.entities.components.network.NetworkC;
import com.ives.relative.managers.CommandManager;
import com.ives.relative.managers.NetworkManager;
import com.ives.relative.network.Network;
import com.ives.relative.network.packets.UpdatePacket;
import com.ives.relative.network.packets.input.CommandPacket;
import com.ives.relative.network.packets.requests.RequestEntity;
import com.ives.relative.network.packets.updates.PositionPacket;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ives on 13/12/2014.
 */
@Wire
public class ClientNetworkSystem extends IntervalEntitySystem {
    public static float CLIENT_NETWORK_INTERVAL = 1 / 20f;
    public long playerNetworkId;
    protected ClientManager clientManager;
    protected CommandManager commandManager;

    /**
     * This contains every byte of the command, this will be sent to the server
     */
    Array<Byte> commandNetworkList;

    Map<Integer, byte[]> sentCommands;
    int sequence;

    public ClientNetworkSystem(ClientNetwork network) {
        super(Aspect.getAspectForAll(NetworkC.class, Position.class), CLIENT_NETWORK_INTERVAL);
        commandNetworkList = new Array<Byte>();


        sentCommands = new HashMap<Integer, byte[]>();
        processRequests(network);

    }

    public void addCommand(Command command) {
        commandNetworkList.add(commandManager.getID(command.getClass().getSimpleName()));
    }

    public void registerPlayer(long playerNetworkId) {
        this.playerNetworkId = playerNetworkId;
    }

    public long getPlayerID() {
        return playerNetworkId;
    }

    /**
     * Sends the input of the player to the server, also puts it in a local variable to know how to apply Server
     * Reconciliation.
     * @param entities
     */
    @Override
    protected void processEntities(ImmutableBag<Entity> entities) {
        if (commandNetworkList.size != 0) {
            int connectionID = ClientNetwork.CONNECTIONID;
            long entityID = this.playerNetworkId;
            CommandPacket commandPacket = new CommandPacket(sequence, commandNetworkList, entityID);
            clientManager.network.sendObjectUDP(connectionID, commandPacket);

            byte[] bytes = new byte[commandNetworkList.size];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = commandNetworkList.get(i);
            }
            sentCommands.put(sequence, bytes);
            commandNetworkList.clear();

            sequence++;
        }
    }

    public void processRequests(final Network network) {
        network.endPoint.addListener(new Listener() {
            @Override
            public void received(Connection connection, final Object object) {
                if (object instanceof UpdatePacket) {
                    if (object instanceof PositionPacket) {
                        Gdx.app.postRunnable(new Runnable() {
                            @Override
                            public void run() {
                                PositionPacket packet = (PositionPacket) object;
                                if (!processPosition(packet)) {
                                    network.sendObjectTCP(ClientNetwork.CONNECTIONID, new RequestEntity(((PositionPacket) object).entityID));
                                }
                                applyServerReconciliation(packet);
                            }
                        });
                    }
                }
            }
        });
    }

    public boolean processPosition(PositionPacket packet) {
        Entity entity = world.getManager(NetworkManager.class).getNetworkEntity(packet.entityID);
        //networkEntity.edit().add(position).add(velocity);
        if (entity != null) {
            float x = packet.x;
            float y = packet.y;
            float vx = packet.vx;
            float vy = packet.vy;
            float rotation = packet.rotation;

            Position localPosition = entity.getWorld().getMapper(Position.class).get(entity);
            Velocity localVelocity = entity.getWorld().getMapper(Velocity.class).get(entity);
            Physics physics = entity.getWorld().getMapper(Physics.class).get(entity);

            Body body = physics.body;
            body.setTransform(x, y, rotation);
            localPosition.x = x;
            localPosition.y = y;

            //body.setLinearVelocity(vx, vy);
            //localVelocity.vx = vx;
            //localVelocity.vy = vy;

            return true;
        } else {
            return false;
        }
    }

    /**
     * This applies sent input which hasn't yet been processed by the server. Purely keeps it locally smooth.
     *
     * @param packet The packet which has been received.
     */
    public void applyServerReconciliation(UpdatePacket packet) {
        //Server Reconciliation
        if (packet.entityID == playerNetworkId) {
            sentCommands.remove(packet.sequence);
            System.out.println("Removed packet: " + packet.sequence);

            /*
            Collection<byte[]> bytes = sentCommands.values();
            Collection<Integer> sequences = sentCommands.keySet();
            for(int i = 0; i < sentCommands.size(); i++) {
                int localSequence = (Integer) sequences.toArray()[i];
                byte[] commands = (byte[]) bytes.toArray()[i];
                if (packet.sequence < localSequence) {
                    for (Byte command : commands) {
                        commandManager.executeCommand(command, world.getManager(NetworkManager.class).getNetworkEntity(playerNetworkId));
                    }
                } else {
                    sentCommands.remove(localSequence);
                }
            }
            */

            for (Map.Entry entry : sentCommands.entrySet()) {
                Integer localSequence = (Integer) entry.getKey();
                byte[] commands = (byte[]) entry.getValue();

                System.out.println("Executed extra command with sequence: " + localSequence + " while receiving: " + packet.sequence);
                for (Byte command : commands) {
                    commandManager.executeCommand(command, world.getManager(NetworkManager.class).getNetworkEntity(playerNetworkId));
                }
            }

        }
    }
}