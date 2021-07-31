package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.CruiseDirection;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.events.CraftDetectEvent;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class AscendSign implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftDetect(CraftDetectEvent event){
        World world = event.getCraft().getWorld();
        for(MovecraftLocation location: event.getCraft().getHitBox()){
            var block = location.toBukkit(world).getBlock();
            if (!Tag.SIGNS.isTagged(block.getType())) {
                continue;
            }
            BlockState state = block.getState(false);
            if(state instanceof Sign){
                Sign sign = (Sign) state;
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: ON")) {
                    sign.setLine(0, "Ascend: OFF");
                    //sign.update();
                    sign.setColor(DyeColor.RED);
                    sign.setGlowingText(true);
                }
            }
        }
    }


    @EventHandler
    public void onSignClickEvent(PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (!Tag.SIGNS.isTagged(block.getType())) {
            return;
        }
        BlockState state = block.getState(false);
        if (!(state instanceof Sign)){
            return;
        }
        Sign sign = (Sign) state;
        if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: OFF")) {
            if (CraftManager.getInstance().getCraftByPlayer(event.getPlayer()) == null) {
                return;
            }
            Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
            if (!c.getType().getCanCruise()) {
                return;
            }
            //c.resetSigns(true, false, true);
            sign.setLine(0, "Ascend: ON");
            //sign.update(true);
            sign.setColor(DyeColor.GREEN);
            sign.setGlowingText(true);

            c.setCruiseDirection(CruiseDirection.UP);
            c.setLastCruiseUpdate(System.currentTimeMillis());
            c.setCruising(true);
            c.resetSigns(sign);

            if (!c.getType().getMoveEntities()) {
                CraftManager.getInstance().addReleaseTask(c);
            }
            return;
        }
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Ascend: ON")) {
            return;
        }
        Craft c = CraftManager.getInstance().getCraftByPlayer(event.getPlayer());
        if (c == null || !c.getType().getCanCruise()) {
            return;
        }
        sign.setLine(0, "Ascend: OFF");
        sign.setColor(DyeColor.RED);
        sign.setGlowingText(true);
        //sign.update(true);

        c.setCruising(false);
        c.resetSigns(sign);

    }
}
