package net.countercraft.movecraft.sign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;

public class ContactsSign implements Listener{

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
                if (PlainTextComponentSerializer.plainText().serialize(sign.lines().get(0)).contains("Contacts:")) {
                    sign.line(1, Component.empty());
                    sign.line(2, Component.empty());
                    sign.line(3, Component.empty());
                }
            }
        }
    }

    @EventHandler
    public final void onSignTranslateEvent(SignTranslateEvent event){
        List<Component> lines = event.getLines();
        Craft craft = event.getCraft();
        if (!PlainTextComponentSerializer.plainText().serialize(lines.get(0)).contains("Contacts:")) {
            return;
        }
        int signLine=1;
        for(Craft tcraft : craft.getContacts()) {
            if (tcraft.getSinking()) {
                continue;
            }
            MovecraftLocation center = craft.getHitBox().getMidPoint();
            MovecraftLocation tcenter = tcraft.getHitBox().getMidPoint();
            int distsquared= center.distanceSquared(tcenter);
            // craft has been detected
            String notification = tcraft.getType().getStringProperty(CraftType.NAME);
            if(notification.length()>9) {
                notification = notification.substring(0, 7);
            }
            int distance = (int) Math.sqrt(distsquared);
            notification += " " + (int)Math.sqrt(distsquared);
            int diffx=center.getX() - tcenter.getX();
            int diffz=center.getZ() - tcenter.getZ();
            if(Math.abs(diffx) > Math.abs(diffz)) {
                if(diffx<0) {
                    notification+=" E";
                } else {
                    notification+=" W";
                }
            } else {
                if(diffz<0) {
                    notification+=" S";
                } else {
                    notification+=" N";
                }
            }
            NamedTextColor color;
            if (distance >= 1000) {
                color = NamedTextColor.GREEN;
            }
            else if (distance > 500) {
                color = NamedTextColor.YELLOW;
            }
            else if (distance > 200) {
                color = NamedTextColor.GOLD;
            }
            else {
                color = NamedTextColor.RED;
            }
            lines.set(signLine++, Component.text(notification, color));
            if (signLine >= 4) {
                break;
            }

        }
        if(signLine<4) {
            for(int i=signLine; i<4; i++) {
                lines.set(signLine, Component.empty());
            }
        }
    }


}
