package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class DescendSign implements Listener{

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getWorld();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            var block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
            if(!Tag.SIGNS.isTagged(block.getType())){
                continue;
            }
            BlockState state = block.getState(false);
            if(state instanceof Sign){
                Sign sign = (Sign) state;
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: ON")) {
                    sign.setLine(0, "Descend: OFF");
                    sign.setColor(DyeColor.RED);
                    sign.setGlowingText(true);
                }
            }
        }
    }

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!Tag.SIGNS.isTagged(event.getClickedBlock().getType())) {
            return;
        }
        BlockState state = event.getClickedBlock().getState(false);
        if (!(state instanceof Sign)) {
            return;
        }
        Sign sign = (Sign) state;
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: OFF")) {
            if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
                return;
            }
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (!c.getType().getBoolProperty(CraftType.CAN_CRUISE)) {
                return;
            }
            //c.resetSigns(true, true, false);
            sign.setLine(0, "Descend: ON");
            sign.setColor(DyeColor.GREEN);
            sign.setGlowingText(true);
            sign.update(true);

            c.setCruiseDirection(CruiseDirection.DOWN);
            c.setLastCruiseUpdate(System.currentTimeMillis());
            c.setCruising(true);
            c.resetSigns(sign);

            if (!c.getType().getBoolProperty(CraftType.MOVE_ENTITIES)) {
                CraftManager.getInstance().addReleaseTask(c);
            }
            return;
        }
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Descend: ON")) {
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (c != null && c.getType().getBoolProperty(CraftType.CAN_CRUISE)) {
                sign.setLine(0, "Descend: OFF");
                sign.setColor(DyeColor.RED);
                sign.setGlowingText(true);
                sign.update(true);
                c.setCruising(false);
                c.resetSigns(sign);
            }
        }
    }
}
