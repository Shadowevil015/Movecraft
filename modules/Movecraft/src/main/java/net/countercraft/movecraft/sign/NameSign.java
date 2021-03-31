package net.countercraft.movecraft.sign;

import io.papermc.lib.PaperLib;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.utils.ChatUtils;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class NameSign implements Listener {
    private static final String HEADER = "Name:";

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getLine(0).equalsIgnoreCase(HEADER) && Settings.RequireNamePerm && !event.getPlayer().hasPermission("movecraft.name.place")) {
            event.getPlayer().sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Insufficient permissions");
            event.setCancelled(true);
        }
    }
}
