/*
 * This file is part of Movecraft.
 *
 *     Movecraft is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Movecraft is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Movecraft.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.TypesReloadedEvent;
import net.countercraft.movecraft.exception.NonCancellableReleaseException;
import net.countercraft.movecraft.localisation.I18nSupport;
import net.countercraft.movecraft.processing.CachedMovecraftWorld;
import net.countercraft.movecraft.processing.WorldManager;
import net.countercraft.movecraft.processing.effects.Effect;
import net.countercraft.movecraft.processing.functions.CraftSupplier;
import net.countercraft.movecraft.processing.tasks.detection.DetectionTask;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;
import java.util.logging.Level;

import static net.countercraft.movecraft.util.ChatUtils.ERROR_PREFIX;

public class CraftManager implements Iterable<Craft>{
    private static CraftManager ourInstance;
    @NotNull private final Set<Craft> craftList = new ConcurrentSkipListSet<>(Comparator.comparingInt(Object::hashCode));
    @NotNull private final ConcurrentMap<Player, PlayerCraft> craftPlayerIndex = new ConcurrentHashMap<>();
    @NotNull private final ConcurrentMap<Craft, BukkitTask> releaseEvents = new ConcurrentHashMap<>();
    @NotNull private Set<CraftType> craftTypes;
    @NotNull private final WeakHashMap<Player, Long> overboards = new WeakHashMap<>();
    @NotNull private final Set<Craft> sinking = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void initialize(boolean loadCraftTypes) {
        ourInstance = new CraftManager(loadCraftTypes);
    }

    private CraftManager(boolean loadCraftTypes) {
        if(loadCraftTypes) {
            this.craftTypes = loadCraftTypes();
        }
        else {
            this.craftTypes = new HashSet<>();
        }
    }

    public static CraftManager getInstance() {
        return ourInstance;
    }

    @NotNull
    public Set<CraftType> getCraftTypes() {
        return Collections.unmodifiableSet(craftTypes);
    }

    @NotNull
    private Set<CraftType> loadCraftTypes() {
        File craftsFile = new File(Movecraft.getInstance().getDataFolder().getAbsolutePath() + "/types");

        if (craftsFile.mkdirs()) {
            Movecraft.getInstance().saveResource("types/Airship.craft", false);
            Movecraft.getInstance().saveResource("types/Airskiff.craft", false);
            Movecraft.getInstance().saveResource("types/BigAirship.craft", false);
            Movecraft.getInstance().saveResource("types/BigSubAirship.craft", false);
            Movecraft.getInstance().saveResource("types/Elevator.craft", false);
            Movecraft.getInstance().saveResource("types/LaunchTorpedo.craft", false);
            Movecraft.getInstance().saveResource("types/Ship.craft", false);
            Movecraft.getInstance().saveResource("types/SubAirship.craft", false);
            Movecraft.getInstance().saveResource("types/Submarine.craft", false);
            Movecraft.getInstance().saveResource("types/Turret.craft", false);
        }

        Set<CraftType> craftTypes = new HashSet<>();
        File[] files = craftsFile.listFiles();
        if (files == null) {
            return craftTypes;
        }

        for (File file : files) {
            if (file.isFile()) {

                if (file.getName().contains(".craft")) {
                    try {
                        CraftType type = new CraftType(file);
                        craftTypes.add(type);
                    }
                    catch (IllegalArgumentException | CraftType.TypeNotFoundException | ParserException | ScannerException e) {
                        Movecraft.getInstance().getLogger().log(Level.SEVERE, I18nSupport.getInternationalisedString("Startup - failure to load craft type") + " '" + file.getName() + "' " + e.getMessage());
                    }
                }
            }
        }
        if (craftTypes.isEmpty()) {
            Movecraft.getInstance().getLogger().log(Level.SEVERE, ERROR_PREFIX + I18nSupport.getInternationalisedString("Startup - No Crafts Found"));
        }
        Movecraft.getInstance().getLogger().log(Level.INFO, String.format(I18nSupport.getInternationalisedString("Startup - Number of craft files loaded"), craftTypes.size()));
        return craftTypes;
    }

    public void initCraftTypes() {
        this.craftTypes = loadCraftTypes();
        Bukkit.getServer().getPluginManager().callEvent(new TypesReloadedEvent());
    }

    public void addCraft(@NotNull PlayerCraft c) {
        if(craftPlayerIndex.containsKey(c.getPilot()))
            throw new IllegalStateException("Players may only have one PlayerCraft associated with them!");

        craftList.add(c);
        craftPlayerIndex.put(c.getPilot(), c);
    }

    public void addCraft(@NotNull Craft c) {
        if(c instanceof PlayerCraft){
            addCraft((PlayerCraft) c);
        }
        else {
            this.craftList.add(c);
        }
    }

    public void removeCraft(@NotNull Craft c, @NotNull CraftReleaseEvent.Reason reason) {
        CraftReleaseEvent e = new CraftReleaseEvent(c, reason);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (e.isCancelled())
            return;

        removeReleaseTask(c);

        craftList.remove(c);
        if(c instanceof PlayerCraft)
            this.craftPlayerIndex.remove(((PlayerCraft) c).getPilot());
        // if its sinking, just remove the craft without notifying or checking
        if(c.getHitBox().isEmpty())
            Movecraft.getInstance().getLogger().warning(I18nSupport.getInternationalisedString("Release - Empty Craft Release Console"));
        else {
            if (c instanceof PlayerCraft)
                c.getAudience().sendMessage(Component.text(I18nSupport.getInternationalisedString("Release - Craft has been released")));
            if (c instanceof PilotedCraft)
                Movecraft.getInstance().getLogger().info(String.format(I18nSupport.getInternationalisedString("Release - Player has released a craft console"), ((PilotedCraft) c).getPilot().getName(), c.getType().getStringProperty(CraftType.NAME), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
            else
                Movecraft.getInstance().getLogger().info(String.format(I18nSupport.getInternationalisedString("Release - Null Craft Release Console"), c.getType().getStringProperty(CraftType.NAME), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
        }
        Movecraft.getInstance().getAsyncManager().addWreck(c);
    }

    public void forceRemoveCraft(@NotNull Craft c) {
        this.craftList.remove(c);
        if(c instanceof PlayerCraft)
            craftPlayerIndex.remove(((PlayerCraft) c).getPilot());
        CraftReleaseEvent e = new CraftReleaseEvent(c, CraftReleaseEvent.Reason.FORCE);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if(e.isCancelled()) {
            throw new NonCancellableReleaseException();
        }
    }

    public void detect(@NotNull MovecraftLocation startPoint,
                        @NotNull CraftType type, @NotNull CraftSupplier supplier,
                        @NotNull World world, @NotNull Player player,
                        @NotNull Audience audience,
                        @NotNull Function<Craft, Effect> postDetection) {
        WorldManager.INSTANCE.submit(new DetectionTask(
                startPoint, CachedMovecraftWorld.of(world),
                type, supplier,
                world, player,
                audience,
                postDetection
        ));
    }

    @NotNull
    public Set<Craft> getCraftsInWorld(@NotNull World w) {
        Set<Craft> crafts = new HashSet<>();
        for (Craft c : craftList) {
            if (c.getWorld() == w)
                crafts.add(c);
        }
        return crafts;
    }

    @Contract("null -> null")
    @Nullable
    public PlayerCraft getCraftByPlayer(@Nullable Player p) {
        if(p == null)
            return null;
        return craftPlayerIndex.get(p);
    }


    public PlayerCraft getCraftByPlayerName(String name) {
        for (var entry : craftPlayerIndex.entrySet()) {
            if (entry.getKey() != null && entry.getKey().getName().equals(name))
                return entry.getValue();
        }
        return null;
    }

    public void removeCraftByPlayer(Player player) {
        PilotedCraft craft = craftPlayerIndex.remove(player);
        if (craft != null) {
            craftList.remove(craft);
        }
    }

    @Nullable
    @Deprecated
    public Player getPlayerFromCraft(@NotNull Craft c) {
        for (var entry : craftPlayerIndex.entrySet()) {
            if (entry.getValue() == c)
                return entry.getKey();
        }
        return null;
    }

    @NotNull
    public Set<PlayerCraft> getPlayerCraftsInWorld(World world) {
        Set<PlayerCraft> crafts = new HashSet<>();
        for (PlayerCraft craft : craftPlayerIndex.values()) {
            if (craft.getWorld() == world)
                crafts.add(craft);
        }
        return crafts;
    }

    @Deprecated
    public void removePlayerFromCraft(Craft c) {
        if (!(c instanceof PlayerCraft)) {
            return;
        }
        removeReleaseTask(c);
        Player p = ((PlayerCraft) c).getPilot();
        p.sendMessage(I18nSupport.getInternationalisedString("Release - Craft has been released message"));
        Movecraft.getInstance().getLogger().info(String.format(I18nSupport.getInternationalisedString("Release - Player has released a craft console"), p.getName(), c.getType().getStringProperty(CraftType.NAME), c.getHitBox().size(), c.getHitBox().getMinX(), c.getHitBox().getMinZ()));
        c.setNotificationPlayer(null);
        craftPlayerIndex.remove(p);
    }


    @Deprecated
    public final void addReleaseTask(final Craft c) {
        Player p = getPlayerFromCraft(c);
        if (p != null) {
            p.sendMessage(I18nSupport.getInternationalisedString("Release - Player has left craft"));
        }
        BukkitTask releaseTask = new BukkitRunnable() {
            @Override
            public void run() {
                removeCraft(c, CraftReleaseEvent.Reason.PLAYER);
                // I'm aware this is not ideal, but you shouldn't be using this anyways.
            }
        }.runTaskLater(Movecraft.getInstance(), (20 * 15));
        releaseEvents.put(c, releaseTask);

    }

    @Deprecated
    public final void removeReleaseTask(final Craft c) {
        Player p = getPlayerFromCraft(c);
        if (p != null) {
            if (releaseEvents.containsKey(c)) {
                if (releaseEvents.get(c) != null)
                    releaseEvents.get(c).cancel();
                releaseEvents.remove(c);
            }
        }
    }

    @Deprecated
    public boolean isReleasing(final Craft craft) {
        return releaseEvents.containsKey(craft);
    }

    @NotNull
    @Deprecated
    public Set<Craft> getCraftList() {
        return Collections.unmodifiableSet(craftList);
    }

    @Nullable
    public CraftType getCraftTypeFromString(String s) {
        for (CraftType t : craftTypes) {
            if (s.equalsIgnoreCase(t.getStringProperty(CraftType.NAME))) {
                return t;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return craftList.isEmpty();
    }

    @NotNull
    @Override
    public Iterator<Craft> iterator() {
        return Collections.unmodifiableSet(craftList).iterator();
    }

    public void addOverboard(Player player) {
        overboards.put(player, System.currentTimeMillis());
    }

    @NotNull
    public long getTimeFromOverboard(Player player) {
        return overboards.getOrDefault(player, 0L);
    }

    @Nullable
    public Craft fastNearestCraftToLoc(Location loc) {
        Craft ret = null;
        long closestDistSquared = Long.MAX_VALUE;
        Set<Craft> craftsList = CraftManager.getInstance().getCraftsInWorld(loc.getWorld());
        for (Craft i : craftsList) {
            if (i.getHitBox().isEmpty())
                continue;
            int midX = (i.getHitBox().getMaxX() + i.getHitBox().getMinX()) >> 1;
//				int midY=(i.getMaxY()+i.getMinY())>>1; don't check Y because it is slow
            int midZ = (i.getHitBox().getMaxZ() + i.getHitBox().getMinZ()) >> 1;
            long distSquared = (long) (Math.pow(midX -  loc.getX(), 2) + Math.pow(midZ - (int) loc.getZ(), 2));
            if (distSquared < closestDistSquared) {
                closestDistSquared = distSquared;
                ret = i;
            }
        }
        return ret;
    }
}
