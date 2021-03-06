package net.countercraft.movecraft.events;

import net.countercraft.movecraft.craft.Craft;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ManOverboardEvent extends CraftEvent implements Cancellable {
    @NotNull private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private Location location;
    @NotNull private Player player;
    private boolean cancelled = false;
    private String failMessage = "";

    public ManOverboardEvent(@NotNull Player player, @NotNull Craft c, @NotNull Location location) {
        super(c);
        this.player = player;
        this.location = location;
    }

    @Override @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public void setLocation(@NotNull Location location) {
        this.location = location;
    }

    @NotNull
    public Location getLocation() {
        return location;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void setFailMessage(String failMessage) {
        this.failMessage = failMessage;
    }
}
