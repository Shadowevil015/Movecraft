package net.countercraft.movecraft.mapUpdater.update;

import com.google.common.collect.Sets;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.config.Settings;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.SignTranslateEvent;
import net.countercraft.movecraft.util.Tags;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.countercraft.movecraft.util.hitboxes.SetHitBox;
import net.countercraft.movecraft.util.hitboxes.SolidHitBox;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;

public class CraftTranslateCommand extends UpdateCommand {
    final MovecraftLocation[] SHIFTS = new MovecraftLocation[]{
            new MovecraftLocation(0,-1,0),
            new MovecraftLocation(1,0,0),
            new MovecraftLocation(-1,0,0),
            new MovecraftLocation(0,0,1),
            new MovecraftLocation(0,0,-1)};

    @NotNull private final Craft craft;
    @NotNull private final MovecraftLocation displacement;
    @NotNull private final World world;
    @NotNull private final HitBox oldHitBox;

    public CraftTranslateCommand(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull World world, @NotNull HitBox oldHitBox){
        this.craft = craft;
        this.displacement = displacement;
        this.world = world;
        this.oldHitBox = oldHitBox;
    }

    @Override
    public void doUpdate() {
        Logger logger = Movecraft.getInstance().getLogger();
        if (craft.getHitBox().isEmpty()){
            logger.warning("Attempted to move craft with empty HashHitBox!");
            CraftManager.getInstance().removeCraft(craft, CraftReleaseEvent.Reason.EMPTY);
            return;
        }
        long time = System.nanoTime();
        World oldWorld = craft.getWorld();
        final EnumSet<Material> passthroughBlocks = craft.getType().getMaterialSetProperty(CraftType.PASSTHROUGH_BLOCKS);
        if(craft.getSinking()){
            passthroughBlocks.addAll(Tags.FLUID);
            passthroughBlocks.addAll(Tag.LEAVES.getValues());
            passthroughBlocks.addAll(Tags.SINKING_PASSTHROUGH);
        }
        if(passthroughBlocks.isEmpty()){
            //translate the craft
            Movecraft.getInstance().getWorldHandler().translateCraft(craft,displacement,world);
            craft.setWorld(world);
            //trigger sign events
            this.sendSignEvents();
        } else {
            final Set<MovecraftLocation> to = Sets.difference(craft.getHitBox().asSet(), oldHitBox.asSet());
            //place phased blocks
            for (MovecraftLocation location : to) {
                var data = world.getBlockData(location.getX(), location.getY(), location.getZ());
                if (passthroughBlocks.contains(data.getMaterial())) {
                    craft.getPhaseBlocks().put(location, data);
                }
            }
            //The subtraction of the set of coordinates in the HitBox cube and the HitBox itself
            final var invertedHitBox = new HashSet<>(craft.getHitBox().boundingHitBox().asSet());
            invertedHitBox.removeAll(craft.getHitBox().asSet());

            //place phased blocks
            final Set<MovecraftLocation> overlap = new HashSet<>(craft.getPhaseBlocks().keySet());
            overlap.retainAll(craft.getHitBox().asSet());
            final int minX = craft.getHitBox().getMinX();
            final int maxX = craft.getHitBox().getMaxX();
            final int minY = craft.getHitBox().getMinY();
            final int maxY = overlap.isEmpty() ? craft.getHitBox().getMaxY() : Collections.max(overlap, Comparator.comparingInt(MovecraftLocation::getY)).getY();
            final int minZ = craft.getHitBox().getMinZ();
            final int maxZ = craft.getHitBox().getMaxZ();
            final HitBox[] surfaces = {
                    new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(minX, maxY, maxZ)),
                    new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(maxX, maxY, minZ)),
                    new SolidHitBox(new MovecraftLocation(maxX, minY, maxZ), new MovecraftLocation(minX, maxY, maxZ)),
                    new SolidHitBox(new MovecraftLocation(maxX, minY, maxZ), new MovecraftLocation(maxX, maxY, minZ)),
                    new SolidHitBox(new MovecraftLocation(minX, minY, minZ), new MovecraftLocation(maxX, minY, maxZ))};
            final SetHitBox validExterior = new SetHitBox();
            for (HitBox hitBox : surfaces) {
                validExterior.addAll(Sets.difference(hitBox.asSet(),craft.getHitBox().asSet()));
            }

            //Check to see which locations in the from set are actually outside of the craft
            final Set<MovecraftLocation> confirmed = craft.getSinking() ? invertedHitBox: verifyExterior(invertedHitBox, validExterior);

            //A set of locations that are confirmed to be "exterior" locations
            final Set<MovecraftLocation> failed = invertedHitBox;
            invertedHitBox.removeAll(confirmed);

            final WorldHandler handler = Movecraft.getInstance().getWorldHandler();
            for (MovecraftLocation location : failed) {
                var data = world.getBlockData(location.getX(), location.getY(), location.getZ());
                if (!passthroughBlocks.contains(data.getMaterial())) {
                    continue;
                }
                craft.getPhaseBlocks().put(location, data);
            }
            //translate the craft
            handler.translateCraft(craft, displacement,world);
            craft.setWorld(world);
            //trigger sign events
            this.sendSignEvents();

            for (MovecraftLocation l : failed){
                MovecraftLocation orig = l.subtract(displacement);
                if (failed.contains(orig) || craft.getHitBox().contains(orig)){
                    continue;
                }
                confirmed.add(orig);

            }

            //place confirmed blocks if they have been un-phased
            for (MovecraftLocation location : confirmed) {
                if (!craft.getPhaseBlocks().containsKey(location)) {
                    continue;
                }
                //Do not place if it is at a collapsed HitBox location
                if (!craft.getCollapsedHitBox().isEmpty() && craft.getCollapsedHitBox().contains(location))
                    continue;
                var phaseBlock = craft.getPhaseBlocks().remove(location);
                handler.setBlockFast(oldWorld, location, phaseBlock);
            }

            for(MovecraftLocation location : oldHitBox) {
                if (craft.getPhaseBlocks().containsKey(location) && !craft.getHitBox().contains(location)) {
                    var phaseBlock = craft.getPhaseBlocks().remove(location);
                    handler.setBlockFast(oldWorld, location, phaseBlock);
                }
            }

            var airBlockData = Material.AIR.createBlockData();
            for (MovecraftLocation location : failed) {
                var data = oldWorld.getBlockData(location.getX(), location.getY(), location.getZ());
                if (passthroughBlocks.contains(data.getMaterial())) {
                    craft.getPhaseBlocks().put(location, data);
                    handler.setBlockFast(oldWorld, location, airBlockData);

                }
            }
        }
        if (!craft.isNotProcessing()) {
            craft.setProcessing(false);
        }
        time = System.nanoTime() - time;
        if(Settings.Debug) {
            logger.info("Total time: " + (time / 1e6) + " milliseconds. Moving with cooldown of " + craft.getTickCooldown() + ". Speed of: " + String.format("%.2f", craft.getSpeed()) + ". Displacement of: " + displacement);
        }

        // Only add cruise time if cruising
        if(craft.getCruising() && displacement.getY() == 0 && (displacement.getX() == 0 || displacement.getZ() == 0))
            craft.addCruiseTime(time / 1e9f);
    }

    @NotNull
    private Set<MovecraftLocation> verifyExterior(Set<MovecraftLocation> invertedHitBox, SetHitBox validExterior) {
        Set<MovecraftLocation> visited = new HashSet<>(validExterior.asSet());
        Queue<MovecraftLocation> queue = new ArrayDeque<>();
        for(var node : validExterior){
            //If the node is already a valid member of the exterior of the HitBox, continued search is unitary.
            for(var shift : SHIFTS){
                var shifted = node.add(shift);
                if(invertedHitBox.contains(shifted) && visited.add(shifted)){
                    queue.add(shifted);
                }
            }
        }
        while (!queue.isEmpty()) {
            var node = queue.poll();
            //If the node is already a valid member of the exterior of the HitBox, continued search is unitary.
            for(var shift : SHIFTS){
                var shifted = node.add(shift);
                if(invertedHitBox.contains(shifted) && visited.add(shifted)){
                    queue.add(shifted);
                }
            }
        }
        return visited;
    }

    private void sendSignEvents() {
        HashMap<List<Component>, List<MovecraftLocation>> signs = new HashMap<>();
        Map<MovecraftLocation, Sign> signStates = new HashMap<>();
        for (MovecraftLocation location : craft.getHitBox()) {
            Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
            if (!Tag.SIGNS.isTagged(block.getType())) {
                continue;
            }
            BlockState state = block.getState(false);
            if (state instanceof Sign) {
                Sign sign = (Sign) state;
                List<Component> lines = new ArrayList<>(sign.lines());
                if (!signs.containsKey(lines))
                    signs.put(lines, new ArrayList<>());
                signs.get(lines).add(location);
                signStates.put(location, sign);
            }
        }
        for (var entry : signs.entrySet()) {
            SignTranslateEvent event = new SignTranslateEvent(craft, entry.getKey(), entry.getValue());
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (!event.isUpdated()) {
                continue;
            }
            for (MovecraftLocation location : entry.getValue()) {
                Block block = world.getBlockAt(location.getX(), location.getY(), location.getZ());
                if (!Tag.SIGNS.isTagged(block.getType())) {
                    continue;
                }
                BlockState state = block.getState(false);
                if (!(state instanceof Sign)) {
                    continue;
                }
                Sign sign = signStates.get(location);
                for (int i = 0; i < 4; i++) {
                    sign.line(i, entry.getKey().get(i));
                }
            }
        }
    }

    @NotNull
    public Craft getCraft() {
        return craft;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CraftTranslateCommand)) {
            return false;
        }
        CraftTranslateCommand other = (CraftTranslateCommand) obj;
        return other.craft.equals(this.craft) &&
                other.displacement.equals(this.displacement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(craft, displacement);
    }
}
