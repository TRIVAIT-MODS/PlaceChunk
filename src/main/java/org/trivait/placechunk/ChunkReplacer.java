package org.trivait.placechunk;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChunkReplacer {
    private static final Queue<ReplaceTask> TASK_QUEUE = new ConcurrentLinkedQueue<>();
    private static final int BLOCKS_PER_TICK = 4096;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ChunkReplacer::onServerTick);
    }

    public static boolean isValidTarget(BlockGetter level, BlockPos pos, BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return true;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean shouldReplaceBlock(BlockGetter level, BlockPos pos, BlockState state) {
        if (state == null || state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    public static void replaceChunkAt(ServerLevel world, BlockPos targetPos, BlockState targetState) {
        ChunkPos chunkPos = new ChunkPos(targetPos.getX() >> 4, targetPos.getZ() >> 4);
        int bottomY = world.getMinY();
        int topY = world.getMaxY();

        CompletableFuture.supplyAsync(() -> {
            List<BlockPos> positions = new ArrayList<>();
            int startX = chunkPos.getMinBlockX();
            int startZ = chunkPos.getMinBlockZ();

            for (int y = bottomY; y < topY; y++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        positions.add(new BlockPos(startX + x, y, startZ + z));
                    }
                }
            }
            return positions;
        }).thenAccept(positions -> TASK_QUEUE.add(new ReplaceTask(world, positions, targetState)));
    }

    private static void onServerTick(MinecraftServer server) {
        ReplaceTask currentTask = TASK_QUEUE.peek();
        if (currentTask == null) {
            return;
        }

        int processed = 0;
        while (processed < BLOCKS_PER_TICK && currentTask.index < currentTask.positions.size()) {
            BlockPos pos = currentTask.positions.get(currentTask.index++);
            BlockState currentState = currentTask.level.getBlockState(pos);

            if (currentState != currentTask.targetState && shouldReplaceBlock(currentTask.level, pos, currentState)) {
                currentTask.level.setBlock(pos, currentTask.targetState, 2 | 16);
                processed++;
            }
        }

        if (currentTask.index >= currentTask.positions.size()) {
            TASK_QUEUE.poll();
        }
    }

    private static class ReplaceTask {
        final ServerLevel level;
        final List<BlockPos> positions;
        final BlockState targetState;
        int index = 0;

        ReplaceTask(ServerLevel level, List<BlockPos> positions, BlockState targetState) {
            this.level = level;
            this.positions = positions;
            this.targetState = targetState;
        }
    }
}