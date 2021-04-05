package net.countercraft.movecraft.sign;

import io.papermc.lib.PaperLib;
import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.utils.LegacyUtils;
import net.countercraft.movecraft.utils.SignUtils;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class CruiseSign implements Listener{

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!SignUtils.isSign(block)) {
            return;
        }

        Sign sign = (Sign) PaperLib.getBlockState(block, false).getState();
        Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: OFF")) {
            if (c == null) {
                return;
            }

            if (!c.getType().getCanCruise()) {
                return;
            }
            //c.resetSigns(false, true, true);

            sign.setLine(0, "Cruise: ON");
            sign.update(true);

            org.bukkit.material.Sign materialSign = (org.bukkit.material.Sign) block.getState().getData();
            BlockFace face = ((Directional) materialSign).getFacing();
            c.setCruiseDirection(CruiseDirection.fromBlockFace(face));

            c.setLastCruiseUpdate(System.currentTimeMillis());
            c.setCruising(true);
            c.resetSigns(sign);
            if (!c.getType().getMoveEntities()) {
                CraftManager.getInstance().addReleaseTask(c);
            }
            return;
        }
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Cruise: ON")
                && CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) != null
                && CraftManager.getInstance().getCraftByPlayer(event.getPlayer()).getType().getCanCruise()) {
            sign.setLine(0, "Cruise: OFF");
            sign.update(true);
            c.setCruising(false);
            c.resetSigns(sign);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!event.getLine(0).equalsIgnoreCase("Cruise: OFF") && !event.getLine(0).equalsIgnoreCase("Cruise: ON")) {
            return;
        }
        if (player.hasPermission("movecraft.cruisesign") || !Settings.RequireCreatePerm) {
            return;
        }
        player.sendMessage(I18nSupport.getInternationalisedString("Insufficient Permissions"));
        event.setCancelled(true);
    }
}
