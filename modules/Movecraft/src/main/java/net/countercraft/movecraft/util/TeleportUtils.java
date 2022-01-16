package net.countercraft.movecraft.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.Set;

public class TeleportUtils {
    public static void teleport(Player player, Location loc, float yawChange, float pitchChange) {
        WrapperPlayServerPlayerPositionAndLook positionPacket = new WrapperPlayServerPlayerPositionAndLook(loc.getX(), loc.getY(), loc.getZ(), yawChange, pitchChange, RelativeFlag.getMaskByRelativeFlags(Set.of(RelativeFlag.YAW, RelativeFlag.PITCH)), new Random().nextInt(), false);
        positionPacket.setRelative(RelativeFlag.YAW, true);
        positionPacket.setRelative(RelativeFlag.PITCH, true);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, positionPacket);
    }
}
