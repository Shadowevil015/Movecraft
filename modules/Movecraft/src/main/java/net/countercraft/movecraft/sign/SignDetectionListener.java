package net.countercraft.movecraft.sign;

import io.papermc.lib.PaperLib;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SignDetectionListener implements Listener {

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event) {
        World world = event.getCraft().getWorld();
        for (MovecraftLocation location: event.getCraft().getHitBox()) {
            Block block = location.toBukkit(world).getBlock();
            if (!SignUtils.isSign(block)) {
                continue;
            }
            Sign sign = (Sign) PaperLib.getBlockState(block, false).getState();
            switch (sign.getLine(0).toUpperCase()) {
                case "CONTACTS:":
                case "STATUS":
                    sign.setLine(1, "");
                    sign.setLine(2, "");
                    sign.setLine(3, "");
                    sign.update();
                    break;
                case "ASCEND: ON":
                    sign.setLine(0, "Ascend: OFF");
                    sign.update();
                    break;
                case "CRUISE: ON":
                    sign.setLine(0, "Cruise: OFF");
                    sign.update();
                    break;
                case "DESCEND: ON":
                    sign.setLine(0, "Descend: OFF");
                    sign.update();
                    break;
                case "NAME:":
                    String name = Arrays.stream(sign.getLines()).skip(1).filter(f -> f != null && !f.trim().isEmpty()).collect(Collectors.joining(" "));
                    Player player = event.getCraft().getNotificationPlayer();
                    if (player == null || (Settings.RequireNamePerm && !player.hasPermission("movecraft.name.use"))) {
                        continue;
                    }
                    event.getCraft().setName(name);
                    break;
                default:
                    continue;
            }
        }
    }

}
