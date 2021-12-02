package net.countercraft.movecraft.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPositionRotation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportUtils {

    public static void teleport(Player player, Location location, float yawChange, float pitch) {
        WrapperPlayClientPositionRotation posPacket = new WrapperPlayClientPositionRotation(new Vector3d(location.getX(), location.getY(), location.getZ()), yawChange, pitch, true);
        //PacketEvents.getAPI().getPlayerManager().sendPacket(player, posPacket);
    }
}