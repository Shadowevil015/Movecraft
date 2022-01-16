package net.countercraft.movecraft;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class WorldHandler {
    public abstract void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originLocation, @NotNull MovecraftRotation rotation);

    public abstract void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation newLocation, @NotNull World world);

    public abstract void setBlockFast(@NotNull Location location, @NotNull BlockData data);

    public abstract void setBlockFast(@NotNull World world, @NotNull MovecraftLocation location, @NotNull BlockData data);

    public abstract void setBlockFast(@NotNull Location location, @NotNull MovecraftRotation rotation, @NotNull BlockData data);

    public abstract void setBlockFast(@NotNull World world, @NotNull MovecraftLocation location, @NotNull MovecraftRotation rotation, @NotNull BlockData data);

    public abstract void disableShadow(@NotNull Material type);

    public abstract void processLight(@NotNull World world, HitBox hitBox);

    public abstract void teleportPlayer(Player player, Location location, float yaw, float pitch);
}
