package net.countercraft.movecraft.events;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.kyori.adventure.text.Component;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class SignTranslateEvent extends CraftEvent{
    private static final HandlerList HANDLERS = new HandlerList();
    @NotNull private final List<MovecraftLocation> locations;
    @NotNull private final List<Component> lines;
    private boolean updated = false;

    public SignTranslateEvent(@NotNull Craft craft, @NotNull List<Component> lines, @NotNull List<MovecraftLocation> locations) throws IndexOutOfBoundsException{
        super(craft);
        this.locations = locations;
        this.lines=lines;
    }

    @NotNull
    public List<Component> getLines() {
        this.updated = true;
        return lines;
    }

    public Component getLine(int index) throws IndexOutOfBoundsException{
        if(index > 3 || index < 0)
            throw new IndexOutOfBoundsException();
        return lines.get(index);
    }

    public void setLine(int index, Component line){
        if(index > 3 || index < 0)
            throw new IndexOutOfBoundsException();
        this.updated = true;
        lines.set(index, line);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    public List<MovecraftLocation> getLocations() {
        return Collections.unmodifiableList(locations);
    }

    public boolean isUpdated() {
        return updated;
    }
}
