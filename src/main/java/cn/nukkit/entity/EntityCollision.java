package cn.nukkit.entity;

import cn.nukkit.block.Block;
import cn.nukkit.level.ChunkLoader;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.SimpleAxisAlignedBB;
import cn.nukkit.math.Vector3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityCollision implements ChunkLoader {
    private static final int BLOCK_CACHE_SIZE = 1024;
    private static final int BLOCK_CACHE_MASK = BLOCK_CACHE_SIZE - 1;
    private static final int CHUNK_CACHE_SIZE = 128;
    private static final int BLOCK_CACHE_TTL = 20;
    private static final int CHUNK_CACHE_TTL = 60;
    private static final Block[] FAST_BLOCK_POOL = new Block[1024];
    private static final FullChunk[] FAST_CHUNK_POOL = new FullChunk[256];
    private static int poolCleanupCounter = 0;
    private static final Set<Vector3> recentBlockChanges = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final int COLLISION_CACHE_SIZE = 64;

    static {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                recentBlockChanges.clear();
            }
        }, 0, 1000);
    }

    private final BlockCacheEntry[] blockCache = new BlockCacheEntry[BLOCK_CACHE_SIZE];
    private final ChunkCacheEntry[] chunkCache = new ChunkCacheEntry[CHUNK_CACHE_SIZE];
    private final Map<Long, List<Block>> collisionCache = new HashMap<>();
    private final Entity entity;
    private long lastCleanupTick = 0;
    private int cleanupCounter = 0;
    private AxisAlignedBB lastBoundingBox = null;
    private long lastCheckTick = 0;
    private byte insideCache = 0;
    private double lastSpeedSq = 0;
    private int adaptiveCheckInterval = 5;
    private long lastMovementTick = 0;

    public EntityCollision(Entity entity) {
        this.entity = entity;
    }

    public List<Block> getCollisionBlocks(AxisAlignedBB bb, double motionX, double motionY, double motionZ) {
        long currentTick = entity.getServer().getTick();
        double speedSq = motionX * motionX + motionY * motionY + motionZ * motionZ;

        updateAdaptiveCheckInterval(speedSq, currentTick);
        long cacheKey = calculateCacheKey(bb, currentTick);

        if (collisionCache.containsKey(cacheKey) && hasRecentBlockChanges(bb)) {
            List<Block> cached = collisionCache.get(cacheKey);
            if (cached != null) return cached;
        }

        if (speedSq < 0.0001 && entity instanceof EntityLiving) {
            if (currentTick % adaptiveCheckInterval != 0 && !isNearDangerousBlocks(bb)) {
                List<Block> empty = Collections.emptyList();
                cacheCollisionResult(cacheKey, empty);
                return empty;
            }
        }

        AxisAlignedBB boundingBox = bb.clone();
        double baseExpand = 0.3;
        double speedFactor = Math.min(2.0, Math.sqrt(speedSq) * 2.0);
        double expand = baseExpand + speedFactor;
        AxisAlignedBB expandedBB = boundingBox.grow(expand, expand, expand);
        int maxBlocks = calculateMaxBlocks(speedSq);
        List<Block> blocks = getBlocksInBoundingBoxFast(expandedBB, maxBlocks);

        if (blocks.isEmpty()) {
            cacheCollisionResult(cacheKey, Collections.emptyList());
            return Collections.emptyList();
        }

        List<Block> collisionBlocks = new ArrayList<>(Math.min(blocks.size(), 16));
        AxisAlignedBB trajectoryBB = createTrajectoryBB(boundingBox, motionX, motionY, motionZ);

        for (Block block : blocks) {
            int blockId = block.getId();
            if (blockId == Block.AIR) continue;

            if (blockId == Block.NETHER_PORTAL || blockId == Block.END_PORTAL) {
                AxisAlignedBB portalBB = new SimpleAxisAlignedBB(block.x, block.y, block.z, block.x + 1, block.y + 1, block.z + 1);
                if (trajectoryBB.intersectsWith(portalBB)) collisionBlocks.add(block);
            } else {
                AxisAlignedBB blockBB = block.getBoundingBox();
                if (blockBB == null) {
                    blockBB = new SimpleAxisAlignedBB(block.x, block.y, block.z, block.x + 1, block.y + 1, block.z + 1);
                }
                if (trajectoryBB.intersectsWith(blockBB)) collisionBlocks.add(block);
            }
        }

        cacheCollisionResult(cacheKey, collisionBlocks);
        return collisionBlocks;
    }

    private List<Block> getBlocksInBoundingBoxFast(AxisAlignedBB bb, int maxBlocks) {
        long currentTickForCache = entity.getServer().getTick();

        if (bb.equals(lastBoundingBox) && currentTickForCache - lastCheckTick < adaptiveCheckInterval) {
            if (hasRecentBlockChanges(bb)) return Collections.emptyList();
        }

        int minX = NukkitMath.floorDouble(bb.getMinX());
        int minY = Math.max(NukkitMath.floorDouble(bb.getMinY()), this.entity.getLevel().getMinBlockY());
        int minZ = NukkitMath.floorDouble(bb.getMinZ());
        int maxX = NukkitMath.ceilDouble(bb.getMaxX());
        int maxY = Math.min(NukkitMath.ceilDouble(bb.getMaxY()), this.entity.getLevel().getMaxBlockY());
        int maxZ = NukkitMath.ceilDouble(bb.getMaxZ());

        if (minY > maxY) return Collections.emptyList();

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        int totalBlocks = sizeX * sizeY * sizeZ;

        if (totalBlocks <= 0 || totalBlocks > maxBlocks) {
            return sampleBlocksInArea(minX, minY, minZ, maxX, maxY, maxZ, maxBlocks);
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

        FullChunk[] chunks = FAST_CHUNK_POOL;
        if (chunks.length < chunksWidth * chunksDepth) {
            chunks = new FullChunk[chunksWidth * chunksDepth];
        }

        loadChunksBatchFast(chunkMinX, chunkMaxX, chunkMinZ, chunkMaxZ, chunksDepth, chunks);

        Block[] blockPool = FAST_BLOCK_POOL;
        int poolIndex = 0;
        poolCleanupCounter++;

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
                    if (!this.entity.getLevel().isYInRange(y)) continue;
                    Vector3 blockPos = new Vector3(x, y, z);
                    boolean recentlyChanged = recentBlockChanges.contains(blockPos);
                    int cacheKey = ((x * 31 + y) * 31 + z) & BLOCK_CACHE_MASK;
                    BlockCacheEntry entry = blockCache[cacheKey];
                    Block block;

                    if (!recentlyChanged && entry != null && entry.x == x && entry.y == y && entry.z == z &&
                            currentTickForCache - entry.timestamp < BLOCK_CACHE_TTL) {
                        block = entry.block;
                    } else {
                        int blockId = chunk.getBlockId(localX, y, localZ);
                        int blockMeta = chunk.getBlockData(localX, y, localZ);
                        block = Block.get(blockId, blockMeta, entity.getLevel(), x, y, z);
                        if (blockId != Block.AIR) {
                            blockCache[cacheKey] = new BlockCacheEntry(x, y, z, block, currentTickForCache);
                        }
                        recentBlockChanges.remove(blockPos);
                    }

                    if (block != null && block.getId() != Block.AIR) {
                        if (poolIndex < blockPool.length) {
                            blockPool[poolIndex++] = block;
                        } else {
                            return convertPoolToList(blockPool, poolIndex);
                        }
                    }
                }
            }
        }

        if (poolIndex == 0) return Collections.emptyList();
        List<Block> result = convertPoolToList(blockPool, poolIndex);

        cleanupCounter++;
        if (cleanupCounter > 100) {
            cleanupOldCacheLazy(currentTickForCache);
            cleanupCounter = 0;
        }

        if (poolCleanupCounter > 500) {
            Arrays.fill(FAST_BLOCK_POOL, null);
            Arrays.fill(FAST_CHUNK_POOL, null);
            poolCleanupCounter = 0;
        }

        lastBoundingBox = bb.clone();
        lastCheckTick = currentTickForCache;
        return result;
    }

    private void updateAdaptiveCheckInterval(double speedSq, long currentTick) {
        if (Math.abs(speedSq - lastSpeedSq) > 0.1) {
            lastSpeedSq = speedSq;
            if (speedSq > 1.0) adaptiveCheckInterval = 1;
            else if (speedSq > 0.1) adaptiveCheckInterval = 2;
            else if (speedSq > 0.01) adaptiveCheckInterval = 3;
            else adaptiveCheckInterval = 5;

            if (currentTick - lastMovementTick > 20) adaptiveCheckInterval = 10;
        }
        if (speedSq > 0.001) lastMovementTick = currentTick;
    }

    private long calculateCacheKey(AxisAlignedBB bb, long tick) {
        return (((long) (bb.getMinX() * 100)) << 48) |
                (((long) (bb.getMinY() * 100)) << 32) |
                (((long) (bb.getMinZ() * 100)) << 16) |
                (tick / adaptiveCheckInterval);
    }

    private void cacheCollisionResult(long key, List<Block> result) {
        if (collisionCache.size() > COLLISION_CACHE_SIZE) {
            long oldestKey = collisionCache.keySet().iterator().next();
            collisionCache.remove(oldestKey);
        }
        collisionCache.put(key, result.isEmpty() ? Collections.emptyList() : new ArrayList<>(result));
    }

    private boolean hasRecentBlockChanges(AxisAlignedBB bb) {
        int minX = NukkitMath.floorDouble(bb.getMinX());
        int minY = Math.max(NukkitMath.floorDouble(bb.getMinY()), 0);
        int minZ = NukkitMath.floorDouble(bb.getMinZ());
        int maxX = NukkitMath.ceilDouble(bb.getMaxX());
        int maxY = Math.min(NukkitMath.ceilDouble(bb.getMaxY()), 255);
        int maxZ = NukkitMath.ceilDouble(bb.getMaxZ());

        for (Vector3 pos : recentBlockChanges) {
            if (pos.x >= minX && pos.x <= maxX &&
                    pos.y >= minY && pos.y <= maxY &&
                    pos.z >= minZ && pos.z <= maxZ) {
                return false;
            }
        }
        return true;
    }

    private boolean isNearDangerousBlocks(AxisAlignedBB bb) {
        AxisAlignedBB safetyBB = bb.grow(2, 2, 2);
        return isInsideSpecialBlock(safetyBB, Block.LAVA) ||
                isInsideSpecialBlock(safetyBB, Block.FIRE) ||
                isInsideSpecialBlock(safetyBB, Block.CACTUS);
    }

    private int calculateMaxBlocks(double speedSq) {
        int baseLimit = 512;
        int speedBonus = (int) (speedSq * 1000);
        return Math.min(baseLimit + speedBonus, 2048);
    }

    private AxisAlignedBB createTrajectoryBB(AxisAlignedBB bb, double motionX, double motionY, double motionZ) {
        double minX = bb.getMinX() + Math.min(0, motionX) - 0.3;
        double minY = bb.getMinY() + Math.min(0, motionY) - 0.3;
        double minZ = bb.getMinZ() + Math.min(0, motionZ) - 0.3;
        double maxX = bb.getMaxX() + Math.max(0, motionX) + 0.3;
        double maxY = bb.getMaxY() + Math.max(0, motionY) + 0.3;
        double maxZ = bb.getMaxZ() + Math.max(0, motionZ) + 0.3;
        return new SimpleAxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private List<Block> sampleBlocksInArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int maxSamples) {
        List<Block> samples = new ArrayList<>();
        Random random = new Random();
        int rangeX = maxX - minX + 1;
        int rangeY = maxY - minY + 1;
        int rangeZ = maxZ - minZ + 1;
        for (int i = 0; i < maxSamples; i++) {
            int x = minX + random.nextInt(rangeX);
            int y = minY + random.nextInt(rangeY);
            int z = minZ + random.nextInt(rangeZ);
            Block block = entity.getLevel().getBlock(x, y, z);
            if (block.getId() != Block.AIR) samples.add(block);
        }
        return samples;
    }

    private List<Block> convertPoolToList(Block[] pool, int count) {
        List<Block> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Block block = pool[i];
            if (block != null) result.add(block);
            pool[i] = null;
        }
        return result;
    }

    private void loadChunksBatchFast(int minCX, int maxCX, int minCZ, int maxCZ, int depth, FullChunk[] chunks) {
        long currentTick = this.entity.getServer().getTick();
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                int idx = (cx - minCX) * depth + (cz - minCZ);
                int cacheKey = (cx * 31 + cz);
                int cacheIdx = cacheKey & (CHUNK_CACHE_SIZE - 1);
                ChunkCacheEntry entry = chunkCache[cacheIdx];
                if (entry != null && entry.key == cacheKey && currentTick - entry.timestamp < CHUNK_CACHE_TTL) {
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

    public boolean isInsideSpecialBlock(AxisAlignedBB bb, int targetBlockId) {
        long currentTick = entity.getServer().getTick();
        if (bb.equals(lastBoundingBox) && currentTick - lastCheckTick < adaptiveCheckInterval) {
            if (targetBlockId == Block.FIRE && (insideCache & 1) != 0) return true;
            if (targetBlockId == Block.LAVA && (insideCache & 2) != 0) return true;
            if (targetBlockId == Block.WATER && (insideCache & 4) != 0) return true;
            if (targetBlockId == Block.CACTUS && (insideCache & 8) != 0) return true;
        }

        lastBoundingBox = bb.clone();
        lastCheckTick = currentTick;
        insideCache = 0;

        int minX = NukkitMath.floorDouble(bb.getMinX());
        int minY = Math.max(NukkitMath.floorDouble(bb.getMinY()), entity.getLevel().getMinBlockY());
        int minZ = NukkitMath.floorDouble(bb.getMinZ());
        int maxX = NukkitMath.ceilDouble(bb.getMaxX());
        int maxY = Math.min(NukkitMath.ceilDouble(bb.getMaxY()), entity.getLevel().getMaxBlockY());
        int maxZ = NukkitMath.ceilDouble(bb.getMaxZ());

        boolean foundFire = false, foundLava = false, foundWater = false, foundCactus = false;

        for (int y = minY; y <= maxY; y++) {
            if (!entity.getLevel().isYInRange(y)) continue;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int blockId = entity.getLevel().getBlockIdAt(x, y, z);
                    switch (blockId) {
                        case Block.FIRE:
                            foundFire = true;
                            if (targetBlockId == Block.FIRE) {
                                insideCache |= 1;
                                return true;
                            }
                            break;
                        case Block.LAVA:
                        case Block.STILL_LAVA:
                            foundLava = true;
                            if (targetBlockId == Block.LAVA) {
                                insideCache |= 2;
                                return true;
                            }
                            break;
                        case Block.WATER:
                        case Block.STILL_WATER:
                            foundWater = true;
                            if (targetBlockId == Block.WATER) {
                                insideCache |= 4;
                                return true;
                            }
                            break;
                        case Block.CACTUS:
                            foundCactus = true;
                            if (targetBlockId == Block.CACTUS) {
                                insideCache |= 8;
                                return true;
                            }
                            break;
                    }
                    if (foundFire && foundLava && foundWater && foundCactus) break;
                }
            }
        }

        if (foundFire) insideCache |= 1;
        if (foundLava) insideCache |= 2;
        if (foundWater) insideCache |= 4;
        if (foundCactus) insideCache |= 8;

        return targetBlockId == Block.FIRE ? foundFire :
                targetBlockId == Block.LAVA ? foundLava :
                        targetBlockId == Block.WATER ? foundWater : foundCactus;
    }

    public List<Block> getBlocksInBoundingBox(AxisAlignedBB bb) {
        return getBlocksInBoundingBoxFast(bb, 512);
    }

    public void cleanupOldCache() {
        long currentTick = this.entity.getServer().getTick();
        if (currentTick - lastCleanupTick < 100) return;
        cleanupOldCacheLazy(currentTick);
    }

    private void cleanupOldCacheLazy(long currentTick) {
        for (int i = 0; i < BLOCK_CACHE_SIZE; i++) {
            BlockCacheEntry entry = blockCache[i];
            if (entry != null && currentTick - entry.timestamp > BLOCK_CACHE_TTL) {
                blockCache[i] = null;
            }
        }

        if (currentTick % 250 == 0) {
            for (int i = 0; i < CHUNK_CACHE_SIZE; i++) {
                ChunkCacheEntry entry = chunkCache[i];
                if (entry != null && currentTick - entry.timestamp > CHUNK_CACHE_TTL) {
                    chunkCache[i] = null;
                }
            }
        }

        if (collisionCache.size() > COLLISION_CACHE_SIZE * 2) {
            Iterator<Long> it = collisionCache.keySet().iterator();
            int toRemove = collisionCache.size() - COLLISION_CACHE_SIZE;
            for (int i = 0; i < toRemove && it.hasNext(); i++) {
                it.next();
                it.remove();
            }
        }

        lastCleanupTick = currentTick;
    }

    @Override public int getLoaderId() { return 0; }
    @Override public boolean isLoaderActive() { return false; }
    @Override public Position getPosition() { return null; }
    @Override public double getX() { return 0; }
    @Override public double getZ() { return 0; }
    @Override public Level getLevel() { return null; }
    @Override public void onChunkChanged(FullChunk chunk) {}
    @Override public void onChunkLoaded(FullChunk chunk) {}
    @Override public void onChunkUnloaded(FullChunk chunk) {}
    @Override public void onChunkPopulated(FullChunk chunk) {}
    @Override public void onBlockChanged(Vector3 pos) { recentBlockChanges.add(pos); }

    private static class BlockCacheEntry {
        final int x, y, z;
        final Block block;
        final long timestamp;
        BlockCacheEntry(int x, int y, int z, Block block, long timestamp) {
            this.x = x;
            this.y = y;
            this.z = z;
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