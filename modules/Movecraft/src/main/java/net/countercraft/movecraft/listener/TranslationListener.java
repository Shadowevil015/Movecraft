package net.countercraft.movecraft.listener;

import com.fastasyncworldedit.core.FaweAPI;
import com.fastasyncworldedit.core.extent.processor.lighting.RelightMode;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

public class TranslationListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTranslate(CraftTranslateEvent event) {
        relightHitBoxes(event.getCraft(), event.getOldHitBox(), event.getNewHitBox());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRotate(CraftRotateEvent event) {
        relightHitBoxes(event.getCraft(), event.getOldHitBox(), event.getNewHitBox());
    }

    private void relightHitBoxes(Craft craft, HitBox oldHitBox, HitBox newHitBox) {
        World world = craft.getWorld();
        com.sk89q.worldedit.world.World worldeditWorld = BukkitAdapter.adapt(world);

        RelightMode relightMode = RelightMode.valueOf(com.fastasyncworldedit.core.configuration.Settings.IMP.LIGHTING.MODE);

        var oldHitBoxMin = BlockVector3.at(oldHitBox.getMinX(), oldHitBox.getMinY(), oldHitBox.getMinZ());
        var oldHitBoxMax = BlockVector3.at(oldHitBox.getMaxX(), oldHitBox.getMaxY(), oldHitBox.getMaxZ());
        CuboidRegion oldHitBoxRegion = new CuboidRegion(oldHitBoxMin, oldHitBoxMax);

        var newHitBoxMin = BlockVector3.at(newHitBox.getMinX(), newHitBox.getMinY(), newHitBox.getMinZ());
        var newHitBoxMax = BlockVector3.at(newHitBox.getMaxX(), newHitBox.getMaxY(), newHitBox.getMaxZ());
        CuboidRegion newHitBoxRegion = new CuboidRegion(newHitBoxMin, newHitBoxMax);

        new BukkitRunnable() {
            @Override
            public void run() {
                FaweAPI.fixLighting(worldeditWorld, oldHitBoxRegion, null, relightMode);
                FaweAPI.fixLighting(worldeditWorld, newHitBoxRegion, null, relightMode);
            }
        }.runTaskAsynchronously(Movecraft.getInstance());
    }

}
