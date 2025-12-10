package cn.nukkit.entity;

import cn.nukkit.block.Block;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.SimpleAxisAlignedBB;

import java.util.*;

public class EntityCollisionCache {
    private static final int BLOCK_CACHE_SIZE = 1024;
    private static final int BLOCK_CACHE_MASK = BLOCK_CACHE_SIZE - 1;
    private static final int CHUNK_CACHE_SIZE = 128;

    private static final int BLOCK_CACHE_TTL = 100;
    private static final int CHUNK_CACHE_TTL = 200;

    private static final ThreadLocal<Block[]> BLOCK_POOL = ThreadLocal.withInitial(() -> new Block[256]);
    private static final ThreadLocal<FullChunk[]> CHUNK_POOL = ThreadLocal.withInitial(() -> new FullChunk[64]);

    private final BlockCacheEntry[] blockCache = new BlockCacheEntry[BLOCK_CACHE_SIZE];
    private final ChunkCacheEntry[] chunkCache = new ChunkCacheEntry[CHUNK_CACHE_SIZE];

    private final Entity entity;
    private long lastCleanupTick = 0;
    private int cleanupCounter = 0;

    public EntityCollisionCache(Entity entity) {
        this.entity = entity;
    }

    public List<Block> getCollisionBlocks(AxisAlignedBB bb, double motionX, double motionY, double motionZ) {
        AxisAlignedBB boundingBox = bb.clone();
        double speed = motionX * motionX + motionY * motionY + motionZ * motionZ;
        double expand = Math.max(0.5, Math.sqrt(speed) * 1.5);

        AxisAlignedBB expandedBB = boundingBox.grow(expand, expand, expand);
        List<Block> blocks = getBlocksInBoundingBox(expandedBB);

        List<Block> collisionBlocks = new ArrayList<>();

        double motionAbsX = Math.abs(motionX);
        double motionAbsY = Math.abs(motionY);
        double motionAbsZ = Math.abs(motionZ);
        AxisAlignedBB trajectoryBB = boundingBox.grow(motionAbsX + 0.3, motionAbsY + 0.3, motionAbsZ + 0.3);

        for (Block block : blocks) {
            if (block.getId() == Block.NETHER_PORTAL) {
                AxisAlignedBB portalBB = new SimpleAxisAlignedBB(
                        block.x, block.y, block.z,
                        block.x + 1, block.y + 1, block.z + 1
                );

                if (trajectoryBB.intersectsWith(portalBB)) {
                    collisionBlocks.add(block);
                }
            } else if (block.collidesWithBB(boundingBox, true)) {
                collisionBlocks.add(block);
            }
        }

        return collisionBlocks;
    }

    public List<Block> getBlocksInBoundingBox(AxisAlignedBB bb) {
        int minX = NukkitMath.floorDouble(bb.getMinX());
        int minY = Math.max(NukkitMath.floorDouble(bb.getMinY()), this.entity.getLevel().getMinBlockY());
        int minZ = NukkitMath.floorDouble(bb.getMinZ());
        int maxX = NukkitMath.ceilDouble(bb.getMaxX());
        int maxY = Math.min(NukkitMath.ceilDouble(bb.getMaxY()), this.entity.getLevel().getMaxBlockY());
        int maxZ = NukkitMath.ceilDouble(bb.getMaxZ());

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0) {
            return Collections.emptyList();
        }

        if (!this.entity.getLevel().isYInRange(minY) && !this.entity.getLevel().isYInRange(maxY)) {
            return Collections.emptyList();
        }

        int chunkMinX = minX >> 4;
        int chunkMaxX = maxX >> 4;
        int chunkMinZ = minZ >> 4;
        int chunkMaxZ = maxZ >> 4;

        int chunksWidth = chunkMaxX - chunkMinX + 1;
        int chunksDepth = chunkMaxZ - chunkMinZ + 1;

        FullChunk[] chunks = CHUNK_POOL.get();
        if (chunks.length < chunksWidth * chunksDepth) {
            chunks = new FullChunk[chunksWidth * chunksDepth];
            CHUNK_POOL.set(chunks);
        }

        loadChunksBatchFast(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, chunksDepth, chunks);

        Block[] blockPool = BLOCK_POOL.get();
        int poolIndex = 0;
        int totalBlocks = sizeX * sizeY * sizeZ;

        if (blockPool.length < totalBlocks) {
            blockPool = new Block[Math.max(totalBlocks, 256)];
            BLOCK_POOL.set(blockPool);
        }

        long currentTick = this.entity.getServer().getTick();
        int levelId = this.entity.getLevel().getId();

        for (int x = minX; x <= maxX; x++) {
            int chunkX = x >> 4;
            int localX = x & 0x0f;
            int chunkIdxX = chunkX - chunkMinX;

            for (int z = minZ; z <= maxZ; z++) {
                int chunkZ = z >> 4;
                int chunkIdxZ = chunkZ - chunkMinZ;
                int chunkArrayIdx = chunkIdxX * chunksDepth + chunkIdxZ;

                FullChunk chunk = chunks[chunkArrayIdx];
                if (chunk == null) continue;

                int localZ = z & 0x0f;

                for (int y = minY; y <= maxY; y++) {
                    int cacheKey = (x * 31 + y) * 31 + z + levelId;
                    int cacheIdx = cacheKey & BLOCK_CACHE_MASK;

                    BlockCacheEntry entry = blockCache[cacheIdx];
                    Block block;

                    if (entry != null && entry.key == cacheKey && currentTick - entry.timestamp < BLOCK_CACHE_TTL) {
                        block = entry.block;
                    } else {
                        int blockId = chunk.getBlockId(localX, y, localZ);
                        int blockMeta = chunk.getBlockData(localX, y, localZ);

                        block = Block.get(blockId, blockMeta, this.entity.getLevel(), x, y, z);

                        if (blockId != Block.AIR) {
                            blockCache[cacheIdx] = new BlockCacheEntry(cacheKey, block, currentTick);
                        }
                    }

                    blockPool[poolIndex++] = block;
                }
            }
        }

        List<Block> result = new ArrayList<>(poolIndex);
        for (int i = 0; i < poolIndex; i++) {
            result.add(blockPool[i]);
        }

        cleanupCounter++;
        if (cleanupCounter > 500) {
            cleanupOldCacheLazy(currentTick);
            cleanupCounter = 0;
        }

        return result;
    }

    private void loadChunksBatchFast(int minCX, int maxCX, int minCZ, int maxCZ, int depth, FullChunk[] chunks) {
        long currentTick = this.entity.getServer().getTick();
        int levelId = this.entity.getLevel().getId();

        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                int idx = (cx - minCX) * depth + (cz - minCZ);

                int cacheKey = (cx * 31 + cz) + levelId;
                int cacheIdx = cacheKey & (CHUNK_CACHE_SIZE - 1);

                ChunkCacheEntry entry = chunkCache[cacheIdx];
                if (entry != null && entry.key == cacheKey &&
                        currentTick - entry.timestamp < CHUNK_CACHE_TTL) {
                    chunks[idx] = entry.chunk;
                    continue;
                }

                FullChunk chunk = this.entity.getLevel().getChunkIfLoaded(cx, cz);
                if (chunk != null) {
                    chunkCache[cacheIdx] = new ChunkCacheEntry(cacheKey, chunk, currentTick);
                    chunks[idx] = chunk;
                }
            }
        }
    }

    public void cleanupOldCache() {
        long currentTick = this.entity.getServer().getTick();
        if (currentTick - lastCleanupTick < 500) {
            return;
        }
        cleanupOldCacheLazy(currentTick);
    }

    private void cleanupOldCacheLazy(long currentTick) {
        for (int i = 0; i < BLOCK_CACHE_SIZE; i += 2) {
            BlockCacheEntry entry = blockCache[i];
            if (entry != null && currentTick - entry.timestamp > BLOCK_CACHE_TTL * 2) {
                blockCache[i] = null;
            }
        }

        if (currentTick % 1000 == 0) {
            for (int i = 0; i < CHUNK_CACHE_SIZE; i++) {
                ChunkCacheEntry entry = chunkCache[i];
                if (entry != null && currentTick - entry.timestamp > CHUNK_CACHE_TTL * 2) {
                    chunkCache[i] = null;
                }
            }
        }

        lastCleanupTick = currentTick;
    }

    private static class BlockCacheEntry {
        final int key;
        final Block block;
        final long timestamp;

        BlockCacheEntry(int key, Block block, long timestamp) {
            this.key = key;
            this.block = block;
            this.timestamp = timestamp;
        }
    }

    private static class ChunkCacheEntry {
        final int key;
        final FullChunk chunk;
        final long timestamp;

        ChunkCacheEntry(int key, FullChunk chunk, long timestamp) {
            this.key = key;
            this.chunk = chunk;
            this.timestamp = timestamp;
        }
    }
}