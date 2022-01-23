package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.util.Counter;
import net.countercraft.movecraft.util.Tags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class StatusSign implements Listener{

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
                if (ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase("Status:")) {
                    sign.setLine(1, "");
                    sign.setLine(2, "");
                    sign.setLine(3, "");
                }
            }
        }
    }

    @EventHandler
    public void onSignTranslate(SignTranslateEvent event) {
        Craft craft = event.getCraft();
        if (!PlainTextComponentSerializer.plainText().serialize(event.getLines().get(0)).contains("Status:")) {
            return;
        }
        int fuel=0;

        var v = craft.getType().getObjectProperty(CraftType.FUEL_TYPES);
        if(!(v instanceof Map<?, ?>))
            throw new IllegalStateException("FUEL_TYPES must be of type Map");
        var fuelTypes = (Map<?, ?>) v;
        for(var e : fuelTypes.entrySet()) {
            if(!(e.getKey() instanceof Material))
                throw new IllegalStateException("Keys in FUEL_TYPES must be of type Material");
            if(!(e.getValue() instanceof Double))
                throw new IllegalStateException("Values in FUEL_TYPES must be of type Double");
        }

        for (MovecraftLocation ml : craft.getHitBox()) {
            Block block = craft.getWorld().getBlockAt(ml.getX(), ml.getY(), ml.getZ());
            if(Tags.FURNACES.contains(block.getType())) {
                InventoryHolder inventoryHolder = (InventoryHolder) block.getState(false);
                for (ItemStack iStack : inventoryHolder.getInventory()) {
                    if (iStack == null || !fuelTypes.containsKey(iStack.getType()))
                        continue;
                    fuel += iStack.getAmount() * (double) fuelTypes.get(iStack.getType());
                }
            }
        }
        String fuelText="";
        int cruiseSkipBlocks = (int) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_CRUISE_SKIP_BLOCKS, craft.getWorld());
        cruiseSkipBlocks++;
        double fuelBurnRate = (double) craft.getType().getPerWorldProperty(CraftType.PER_WORLD_FUEL_BURN_RATE, craft.getWorld());
        int fuelRange= (int) Math.round((fuel * (1 + cruiseSkipBlocks)) / fuelBurnRate);
        TextColor color;
        if(fuelRange>1000) {
            color = NamedTextColor.GREEN;
        } else if(fuelRange>100) {
            color = NamedTextColor.YELLOW;
        } else {
            color = NamedTextColor.RED;
        }
        fuelText+="Fuel range:";
        fuelText+=fuelRange;
        event.setLine(0, Component.text(fuelText, color));
    }
}