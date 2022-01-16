package net.countercraft.movecraft.compat.v1_18_R1;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.WorldHandler;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.util.CollectionUtils;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.UnsafeUtils;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.ScheduledTick;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unused")
public class IWorldHandler extends WorldHandler {
    private static final Rotation ROTATION[];
    static {
        ROTATION = new Rotation[3];
        ROTATION[MovecraftRotation.NONE.ordinal()] = Rotation.NONE;
        ROTATION[MovecraftRotation.CLOCKWISE.ordinal()] = Rotation.CLOCKWISE_90;
        ROTATION[MovecraftRotation.ANTICLOCKWISE.ordinal()] = Rotation.COUNTERCLOCKWISE_90;
    }
    private final NextTickProvider tickProvider = new NextTickProvider();

    private final Plugin plugin;

    public IWorldHandler(Plugin plugin) {
        this.plugin = plugin;
    }
//    @Override
//    public void addPlayerLocation(Player player, double x, double y, double z, float yaw, float pitch){
//        ServerPlayer ePlayer = ((CraftPlayer) player).getHandle();
//        ePlayer.connection.teleport(x, y, z, yaw, pitch, EnumSet.allOf(ClientboundPlayerPositionPacket.RelativeArgument.class));
//    }

    @Override
    public void rotateCraft(@NotNull Craft craft, @NotNull MovecraftLocation originPoint, @NotNull MovecraftRotation rotation) {
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        HashMap<BlockPos,BlockPos> rotatedPositions = new HashMap<>();
        MovecraftRotation counterRotation = rotation == MovecraftRotation.CLOCKWISE ? MovecraftRotation.ANTICLOCKWISE : MovecraftRotation.CLOCKWISE;
        for(MovecraftLocation newLocation : craft.getHitBox()){
            rotatedPositions.put(locationToPosition(MathUtils.rotateVec(counterRotation, newLocation.subtract(originPoint)).add(originPoint)),locationToPosition(newLocation));
        }
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        ServerLevel nativeWorld = ((CraftWorld) craft.getWorld()).getHandle();
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for(BlockPos position : rotatedPositions.keySet()){
            //BlockEntity tile = nativeWorld.removeBlockEntity(position);
            BlockEntity tile = removeBlockEntity(nativeWorld,position);
            if(tile == null)
                continue;
            if (tile instanceof SignBlockEntity) {
                tile.setBlockState(tile.getBlockState().rotate(ROTATION[rotation.ordinal()]));
            }
//            tile.a(ROTATION[rotation.ordinal()]);
            //get the nextTick to move with the tile
            tiles.add(new TileHolder(tile, tickProvider.getNextTick(nativeWorld,position), position));
        }

        //*******************************************
        //*   Step three: Translate all the blocks  *
        //*******************************************
        // blockedByWater=false means an ocean-going vessel
        //TODO: Simplify
        //TODO: go by chunks
        //TODO: Don't move unnecessary blocks
        //get the blocks and rotate them
        HashMap<BlockPos, BlockState> blockData = new HashMap<>();
        for(BlockPos position : rotatedPositions.keySet()){
            blockData.put(position,nativeWorld.getBlockState(position).rotate(ROTATION[rotation.ordinal()]));
        }
        //create the new block and process redstone
        HashMap<BlockPos, BlockState> fireBlocks = new HashMap<>();
        for(Map.Entry<BlockPos,BlockState> entry : blockData.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            BlockPos rotatedPos = rotatedPositions.get(pos);
            setBlockFast(nativeWorld, rotatedPos, state);

            if (state.getBlock() instanceof FireBlock) {
                fireBlocks.put(pos, state);
            }
        }

        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        for(TileHolder tileHolder : tiles){
            moveBlockEntity(nativeWorld, rotatedPositions.get(tileHolder.getTilePosition()),tileHolder.getTile());
            if(tileHolder.getNextTick()==null)
                continue;
            final long currentTime = nativeWorld.N.getGameTime();
            nativeWorld.getBlockTicks().schedule( new ScheduledTick<>((Block) tileHolder.getNextTick().type(), rotatedPositions.get(tileHolder.getNextTick().pos()), tileHolder.getNextTick().triggerTick() - currentTime, tileHolder.getNextTick().priority(), tileHolder.getNextTick().subTickOrder()));
        }

        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        //TODO: add support for pass-through
        Collection<BlockPos> deletePositions =  CollectionUtils.filter(rotatedPositions.keySet(),rotatedPositions.values());
        for(BlockPos position : deletePositions){
            setBlockFast(nativeWorld, position, Blocks.AIR.defaultBlockState());
        }

        //*******************************************
        //*      Step six: Process redstone         *
        //*******************************************
        processRedstone(rotatedPositions.values(), nativeWorld);

        //*******************************************
        //*        Step seven: Process fire         *
        //*******************************************
        processFireSpread(fireBlocks, nativeWorld);
    }

    @Override
    public void translateCraft(@NotNull Craft craft, @NotNull MovecraftLocation displacement, @NotNull org.bukkit.World world) {
        //TODO: Add support for rotations
        //A craftTranslateCommand should only occur if the craft is moving to a valid position
        //*******************************************
        //*      Step one: Convert to Positions     *
        //*******************************************
        BlockPos translateVector = locationToPosition(displacement);
        List<BlockPos> positions = new ArrayList<>(craft.getHitBox().size());
        craft.getHitBox().forEach((movecraftLocation) -> positions.add(locationToPosition((movecraftLocation)).subtract(translateVector)));
        ServerLevel oldNativeWorld = ((CraftWorld) craft.getWorld()).getHandle();
        ServerLevel nativeWorld = ((CraftWorld) world).getHandle();
        //*******************************************
        //*         Step two: Get the tiles         *
        //*******************************************
        List<TileHolder> tiles = new ArrayList<>();
        //get the tiles
        for (BlockPos position : positions) {
            if (oldNativeWorld.getBlockState(position) == Blocks.AIR.defaultBlockState())
                continue;
            //BlockEntity tile = nativeWorld.removeBlockEntity(position);
            BlockEntity tile = removeBlockEntity(oldNativeWorld, position);
            if (tile == null)
                continue;
            //get the nextTick to move with the tile

            //nativeWorld.capturedTileEntities.remove(position);
            //nativeWorld.getChunkAtWorldCoords(position).getTileEntities().remove(position);
            tiles.add(new TileHolder(tile, tickProvider.getNextTick(oldNativeWorld, position), position));

        }
        //*******************************************
        //*   Step three: Translate all the blocks  *
        //*******************************************
        // blockedByWater=false means an ocean-going vessel
        //TODO: Simplify
        //TODO: go by chunks
        //TODO: Don't move unnecessary blocks
        //get the blocks and translate the positions
        List<BlockState> blockData = new ArrayList<>();
        List<BlockPos> newPositions = new ArrayList<>();
        for (BlockPos position : positions) {
            blockData.add(oldNativeWorld.getBlockState(position));
            newPositions.add(position.offset(translateVector));
        }
        //create the new block
        HashMap<BlockPos, BlockState> fireBlocks = new HashMap<>();
        for(int i = 0, positionSize = newPositions.size(); i<positionSize; i++) {
            BlockState state = blockData.get(i);
            BlockPos pos = newPositions.get(i);
            // Add to fire processing list
            if (state.getBlock() instanceof FireBlock) {
                fireBlocks.put(pos, state);
            }
            setBlockFast(nativeWorld, pos, state);
        }
        //*******************************************
        //*    Step four: replace all the tiles     *
        //*******************************************
        //TODO: go by chunks
        for (TileHolder tileHolder : tiles) {
            moveBlockEntity(nativeWorld, tileHolder.getTilePosition().offset(translateVector), tileHolder.getTile());
            if (tileHolder.getNextTick() == null)
                continue;
            final long currentTime = nativeWorld.getGameTime();
            nativeWorld.getBlockTicks().schedule(new ScheduledTick<>((Block) tileHolder.getNextTick().type(), tileHolder.getTilePosition().offset(translateVector), tileHolder.getNextTick().triggerTick() - currentTime, tileHolder.getNextTick().priority(), tileHolder.getNextTick().subTickOrder()));
        }
        //*******************************************
        //*   Step five: Destroy the leftovers      *
        //*******************************************
        List<BlockPos> deletePositions = positions;
        if (oldNativeWorld == nativeWorld) deletePositions = CollectionUtils.filter(positions,newPositions);
        for (BlockPos position : deletePositions) {
            setBlockFast(oldNativeWorld, position, Blocks.AIR.defaultBlockState());
        }

        //*******************************************
        //*      Step six: Process redstone         *
        //*******************************************
        processRedstone(newPositions, nativeWorld);

        //*******************************************
        //*        Step seven: Process fire         *
        //*******************************************
        processFireSpread(fireBlocks, nativeWorld);
    }

    @Nullable
    private BlockEntity removeBlockEntity(@NotNull Level world, @NotNull BlockPos position){
        return world.getChunkAt(position).blockEntities.remove(position);
    }

    @NotNull
    private BlockPos locationToPosition(@NotNull MovecraftLocation loc) {
        return new BlockPos(loc.getX(), loc.getY(), loc.getZ());
    }

    private void setBlockFast(@NotNull Level world, @NotNull BlockPos position,@NotNull BlockState data) {
        LevelChunk chunk = world.getChunkAt(position);
        LevelChunkSection levelChunkSection = chunk.getSections()[(position.getY() >> 4) - chunk.getMinSection()];
        if (levelChunkSection == null) {
            // Put a GLASS block to initialize the section. It will be replaced next with the real block.
            chunk.setBlockState(position, Blocks.GLASS.defaultBlockState(), false);
            levelChunkSection = chunk.getSections()[(position.getY() >> 4) - chunk.getMinSection()];
        }
        if(levelChunkSection.getBlockState(position.getX()&15, position.getY()&15, position.getZ()&15).equals(data)){
            //Block is already of correct type and data, don't overwrite
            return;
        }
        levelChunkSection.setBlockState(position.getX()&15, position.getY()&15, position.getZ()&15, data);

        world.sendBlockUpdated(position, data, data, 3);
        //world.getLightEngine().checkBlock(position); // boolean corresponds to if chunk section empty
        chunk.setUnsaved(true);
    }

    private void processRedstone(Collection<BlockPos> redstone, Level world) {
        for (BlockPos pos : redstone) {
            BlockState data = world.getBlockState(pos);
            if (isRedstoneComponent(data.getBlock())) {
                world.sendBlockUpdated(pos, data, data, 3);
                world.updateNeighborsAt(pos, data.getBlock());
            }
        }
    }

    private void processFireSpread(HashMap<BlockPos, BlockState> fireStates, ServerLevel world) {
        for (var entry: fireStates.entrySet()) {
            BlockState state = entry.getValue();
            if (state.getBlock() instanceof FireBlock fireBlock) {
                fireBlock.tick(state, world, entry.getKey(), world.random);
            }
        }
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull BlockData data){
        setBlockFast(location, MovecraftRotation.NONE, data);
    }

    @Override
    public void setBlockFast(@NotNull World world, @NotNull MovecraftLocation location, @NotNull BlockData data){
        setBlockFast(world, location, MovecraftRotation.NONE, data);
    }

    @Override
    public void setBlockFast(@NotNull World world, @NotNull MovecraftLocation location, @NotNull MovecraftRotation rotation, @NotNull BlockData data) {
        BlockState blockData;
        if(data instanceof CraftBlockData){
            blockData = ((CraftBlockData) data).getState();
        } else {
            blockData = (BlockState) data;
        }
        blockData = blockData.rotate(ROTATION[rotation.ordinal()]);
        Level nmsWorld = ((CraftWorld)(world)).getHandle();
        BlockPos BlockPos = locationToPosition(location);
        setBlockFast(nmsWorld, BlockPos, blockData);
    }

    @Override
    public void setBlockFast(@NotNull Location location, @NotNull MovecraftRotation rotation, @NotNull BlockData data) {
        BlockState blockData;
        if(data instanceof CraftBlockData){
            blockData = ((CraftBlockData) data).getState();
        } else {
            blockData = (BlockState) data;
        }
        blockData = blockData.rotate(ROTATION[rotation.ordinal()]);
        Level world = ((CraftWorld)(location.getWorld())).getHandle();
        BlockPos BlockPos = locationToPosition(MathUtils.bukkit2MovecraftLoc(location));
        setBlockFast(world,BlockPos,blockData);
    }

    @Override
    public void disableShadow(@NotNull Material type) {
        // Disabled
    }

    @Override
    public void processLight(@NotNull World world, HitBox hitBox) {
        ServerLevel nativeWorld = ((CraftWorld) world).getHandle();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (MovecraftLocation loc: hitBox) {
                    nativeWorld.getLightEngine().checkBlock(locationToPosition(loc));
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private Field getField(String name) {
        try {
            var field = ServerGamePacketListenerImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }
        catch (NoSuchFieldException ex) {
            System.out.println("Failed to find field " + name);
            return null;
        }
    }

    private final Field justTeleportedField = getField("justTeleported");
    private final Field awaitingPositionFromClientField = getField("y");
    private final Field lastPosXField = getField("lastPosX");
    private final Field lastPosYField = getField("lastPosY");
    private final Field lastPosZField = getField("lastPosZ");
    private final Field awaitingTeleportField = getField("z");
    private final Field awaitingTeleportTimeField = getField("A");
    private final Field aboveGroundVehicleTickCountField = getField("E");

    @Override
    public void teleportPlayer(Player player, Location location, float yaw, float pitch) {
        ServerPlayer handle = ((CraftPlayer) player).getHandle();

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        if (handle.containerMenu != handle.inventoryMenu) {
            handle.closeContainer();
        }

        handle.absMoveTo(x, y, z, handle.getYRot() + yaw, handle.getXRot() + pitch);

        var connection = handle.connection;

        int teleportAwait;

        try {
            justTeleportedField.set(connection, true);
            awaitingPositionFromClientField.set(connection, new Vec3(x, y, z));
            lastPosXField.set(connection, x);
            lastPosYField.set(connection, y);
            lastPosZField.set(connection, z);

            teleportAwait = awaitingTeleportField.getInt(connection) + 1;
            if (teleportAwait == 2147483647) teleportAwait = 0;
            awaitingTeleportField.set(connection, teleportAwait);

            awaitingTeleportTimeField.set(connection, aboveGroundVehicleTickCountField.get(connection));
        }
        catch (IllegalAccessException ex) {
            ex.printStackTrace();
            return;
        }

        var packet = new ClientboundPlayerPositionPacket(x, y, z, yaw, pitch, EnumSet.of(ClientboundPlayerPositionPacket.RelativeArgument.X_ROT, ClientboundPlayerPositionPacket.RelativeArgument.Y_ROT), teleportAwait, false);
        connection.send(packet);
    }

    private boolean isRedstoneComponent(Block block) {
        return block instanceof RedStoneWireBlock ||
                block instanceof DiodeBlock ||
                block instanceof ButtonBlock ||
                block instanceof LeverBlock;
    }

    private void moveBlockEntity(@NotNull Level nativeWorld, @NotNull BlockPos newPosition, @NotNull BlockEntity tile){
        LevelChunk chunk = nativeWorld.getChunkAt(newPosition);
        try {
            var positionField = BlockEntity.class.getDeclaredField("o"); // o is obfuscated worldPosition
            UnsafeUtils.setField(positionField, tile, newPosition);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        tile.setLevel(nativeWorld);
        tile.clearRemoved();
        if(nativeWorld.captureBlockStates) {
            nativeWorld.capturedTileEntities.put(newPosition, tile);
            return;
        }
        chunk.setBlockEntity(tile);
        chunk.blockEntities.put(newPosition, tile);
    }

    private static class TileHolder{
        @NotNull private final BlockEntity tile;
        @Nullable
        private final ScheduledTick<?> nextTick;
        @NotNull private final BlockPos tilePosition;

        public TileHolder(@NotNull BlockEntity tile, @Nullable ScheduledTick<?> nextTick, @NotNull BlockPos tilePosition){
            this.tile = tile;
            this.nextTick = nextTick;
            this.tilePosition = tilePosition;
        }


        @NotNull
        public BlockEntity getTile() {
            return tile;
        }

        @Nullable
        public ScheduledTick<?> getNextTick() {
            return nextTick;
        }

        @NotNull
        public BlockPos getTilePosition() {
            return tilePosition;
        }
    }
}