package net.countercraft.movecraft.craft;

import net.countercraft.movecraft.craft.type.CraftType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerSubCraft extends PlayerCraftImpl {
    @NotNull
    private Craft parent;

    public PlayerSubCraft(@NotNull CraftType type, @NotNull World world, @NotNull Player pilot, @NotNull Craft parent) {
        super(type, world, pilot);
        this.parent = parent;
    }

    public @NotNull Craft getParent() {
        return parent;
    }

    public void setParent(@NotNull Craft parent) {
        this.parent = parent;
    }
}
