package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class SpeedSign implements Listener{
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
                if (PlainTextComponentSerializer.plainText().serialize(sign.lines().get(0)).contains("Speed:")) {
                    sign.line(1, Component.text("0 m/s"));
                    sign.line(2, Component.text( "0ms"));
                    sign.line(3, Component.text( "0T"));
                }
            }
        }
    }

    @EventHandler
    public void onSignTranslate(SignTranslateEvent event) {
        Craft craft = event.getCraft();
        if (!PlainTextComponentSerializer.plainText().serialize(event.getLines().get(0)).contains("Speed:")) {
            return;
        }
        event.setLine(0, Component.text(String.format("%.2f",craft.getSpeed()) + "m/s"));
        event.setLine(1, Component.text(String.format("%.2f",craft.getMeanCruiseTime() * 1000) + "ms"));
        event.setLine(2, Component.text(craft.getTickCooldown() + "T"));
    }
}
