package net.countercraft.movecraft.events;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called after a craft fails rotation.
 * @see Craft
 */
public class CraftFailRotateEvent extends CraftEvent {
    @NotNull private final HitBox oldHitBox;
    @NotNull private final HitBox newHitBox;
    @NotNull private final MovecraftLocation originPoint;
    @NotNull private final MovecraftRotation rotation;

    @NotNull private static final HandlerList HANDLERS = new HandlerList();

    public CraftFailRotateEvent(@NotNull Craft craft, @NotNull MovecraftRotation rotation, @NotNull MovecraftLocation originPoint, @NotNull HitBox oldHitBox, @NotNull HitBox newHitBox) {
        super(craft);
        this.rotation = rotation;
        this.originPoint = originPoint;
        this.oldHitBox = oldHitBox;
        this.newHitBox = newHitBox;
    }

    @NotNull
    public MovecraftRotation getRotation() {
        return rotation;
    }

    @NotNull
    public MovecraftLocation getOriginPoint() {
        return originPoint;
    }

    @NotNull
    public HitBox getNewHitBox() {
        return newHitBox;
    }

    @NotNull
    public HitBox getOldHitBox(){
        return oldHitBox;
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
