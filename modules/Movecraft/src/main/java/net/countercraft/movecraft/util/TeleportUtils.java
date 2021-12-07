package net.countercraft.movecraft.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlags;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class TeleportUtils {
    public static void teleport(Player player, Location loc, float yawChange, float pitchChange) {
        WrapperPlayServerPlayerPositionAndLook positionPacket = new WrapperPlayServerPlayerPositionAndLook(loc.getX(), loc.getY(), loc.getZ(), yawChange, pitchChange, 0, (int) loc.toBlockKey());
        positionPacket.setRelative(RelativeFlags.X, false);
        positionPacket.setRelative(RelativeFlags.Y, false);
        positionPacket.setRelative(RelativeFlags.Z, false);
        positionPacket.setRelative(RelativeFlags.YAW, true);
        positionPacket.setRelative(RelativeFlags.PITCH, true);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, positionPacket);
    }
}
