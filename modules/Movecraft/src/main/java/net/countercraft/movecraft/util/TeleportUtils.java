package net.countercraft.movecraft.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlags;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Random;

public class TeleportUtils {
    public static void teleport(Player player, double xChange, double yChange, double zChange, float yawChange, float pitchChange) {
        WrapperPlayServerPlayerPositionAndLook positionPacket = new WrapperPlayServerPlayerPositionAndLook(xChange, yChange, zChange, yawChange, pitchChange, 0, new Random().nextInt());
        positionPacket.setRelative(RelativeFlags.X, true);
        positionPacket.setRelative(RelativeFlags.Y, true);
        positionPacket.setRelative(RelativeFlags.Z, true);
        positionPacket.setRelative(RelativeFlags.YAW, true);
        positionPacket.setRelative(RelativeFlags.PITCH, true);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, positionPacket);
    }
}
