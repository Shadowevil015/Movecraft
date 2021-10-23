package net.countercraft.movecraft.util;

import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.RelativeArgument.X_ROT;
import static net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT;

public class TeleportUtils {

    private static final Set<ClientboundPlayerPositionPacket.RelativeArgument> TELEPORT_FLAGS = Collections.unmodifiableSet(EnumSet.of(X_ROT, Y_ROT));

    private static Field justTeleportedField;
    private static Field awaitingPositionFromClientField;
    private static Field lastPosXField;
    private static Field lastPosYField;
    private static Field lastPosZField;
    private static Field awaitingTeleportField;
    private static Field awaitingTeleportTimeField;
    private static Field aboveGroundVehicleTickCountField;

    static {
        try {
            justTeleportedField = getField("justTeleported");
            awaitingPositionFromClientField = getField("y"); //awaitingPositionFromClient
            lastPosXField = getField("lastPosX");
            lastPosYField = getField("lastPosY");
            lastPosZField = getField("lastPosZ");
            awaitingTeleportField = getField("z"); //awaitingTeleport
            awaitingTeleportTimeField = getField("A"); //awaitingTeleportTime
            aboveGroundVehicleTickCountField = getField("E"); //aboveGroundVehicleTickCount
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private static Field getField(String name) throws NoSuchFieldException {
        Field field = ServerGamePacketListenerImpl.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static void teleport(Player player, Location location, float yawChange) {

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        ServerPlayer handle = ((CraftPlayer) player).getHandle();
        //if (handle.containerMenu != handle.inventoryMenu) handle.closeContainer();
        handle.absMoveTo(x, y, z, handle.getYRot(), handle.getXRot());
        ServerGamePacketListenerImpl connection = handle.connection;
        int teleportAwait = 0;
        try {
            justTeleportedField.set(connection, true);
            awaitingPositionFromClientField.set(connection, new Vec3(x, y, z));
            lastPosXField.set(connection, x);
            lastPosYField.set(connection, y);
            lastPosZField.set(connection, z);
            teleportAwait = awaitingTeleportField.getInt(connection) + 1;
            if (teleportAwait == 2147483647) teleportAwait = 0;
            awaitingTeleportField.set(connection, teleportAwait);
            awaitingTeleportTimeField.set(connection, aboveGroundVehicleTickCountField.get(connection));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        connection.send(new ClientboundPlayerPositionPacket(x, y, z, yawChange, 0, TELEPORT_FLAGS, teleportAwait, false));
    }
}