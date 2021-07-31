package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called after a craft is translated.
 * @see Craft
 */
public class CraftPostTranslateEvent extends CraftEvent {
    private int dx, dy, dz;
    @NotNull private World world;
    @NotNull private static final HandlerList HANDLERS = new HandlerList();
    public CraftPostTranslateEvent(@NotNull Craft craft, int dx, int dy, int dz, @NotNull World world) {
        super(craft);
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.world = world;
    }

    /**
     * Gets the translation change in X direction
     * @return translation change in X direction
     */
    public int getDx() {
        return dx;
    }

    /**
     * Gets the translation change in Y direction
     * @return translation change in Y direction
     */
    public int getDy() {
        return dy;
    }

    /**
     * Gets the translation change in Z direction
     * @return translation change in Z direction
     */
    public int getDz() {
        return dz;
    }


    /**
     * Gets the destination world
     * @return world to translate to
     */
    @NotNull
    public World getWorld() {
        return world;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

}
