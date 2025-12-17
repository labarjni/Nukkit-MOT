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
    private static final int BLOCK_CACHE_MAX_SIZE = 2048;
    private static final int CHUNK_CACHE_MAX_SIZE = 256;
    private static final int COLLISION_CACHE_MAX_SIZE = 128;

    private final Map<Long, Block> blockCache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Block> eldest) {
            return size() > BLOCK_CACHE_MAX_SIZE;
        }
    };

    private final Map<Integer, FullChunk> chunkCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, FullChunk> eldest) {
            return size() > CHUNK_CACHE_MAX_SIZE;
        }
    };

    private final Map<Long, List<Block>> collisionCache = new LinkedHashMap<>(32, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, List<Block>> eldest) {
            return size() > COLLISION_CACHE_MAX_SIZE;
        }
    };

    private final Set<Integer> changedChunks = ConcurrentHashMap.newKeySet();

    private final Entity entity;
    private AxisAlignedBB lastCheckedBB = null;
    private int adaptiveCheckInterval = 5;
    private double lastSpeedSq = 0;
    private long lastMovementTick = 0;
    private byte insideSpecialCache = 0;
    private long lastSpecialCheckTick = 0;

    public EntityCollision(Entity entity) {
        this.entity = entity;
    }

    public List<Block> getCollisionBlocks(AxisAlignedBB bb, double motionX, double motionY, double motionZ) {
        long currentTick = entity.getServer().getTick();
        double speedSq = motionX * motionX + motionY * motionY + motionZ * motionZ;

        updateAdaptiveCheckInterval(speedSq, currentTick);
        cleanupChangedChunks();
        long cacheKey = calculateCacheKey(bb, motionX, motionY, motionZ);

        if (!hasRelevantChunkChanges(bb) && collisionCache.containsKey(cacheKey)) {
            return collisionCache.get(cacheKey);
        }

        if (speedSq < 0.0001 && entity instanceof EntityLiving) {
            if (currentTick % adaptiveCheckInterval != 0 && !isNearDangerousBlocks(bb)) {
                List<Block> empty = Collections.emptyList();
                collisionCache.put(cacheKey, empty);
                return empty;
            }
        }

        double expand = 0.3 + Math.min(2.0, Math.sqrt(speedSq) * 2.0);
        AxisAlignedBB expandedBB = bb.grow(expand, expand, expand);
        List<Block> blocks = getBlocksInBoundingBoxFast(expandedBB, calculateMaxBlocks(speedSq));

        if (blocks.isEmpty()) {
            collisionCache.put(cacheKey, Collections.emptyList());
            return Collections.emptyList();
        }

        AxisAlignedBB trajectoryBB = new SimpleAxisAlignedBB(
                bb.getMinX() + Math.min(0, motionX) - 0.3,
                bb.getMinY() + Math.min(0, motionY) - 0.3,
                bb.getMinZ() + Math.min(0, motionZ) - 0.3,
                bb.getMaxX() + Math.max(0, motionX) + 0.3,
                bb.getMaxY() + Math.max(0, motionY) + 0.3,
                bb.getMaxZ() + Math.max(0, motionZ) + 0.3
        );

        List<Block> collisionBlocks = new ArrayList<>(16);
        for (Block block : blocks) {
            int id = block.getId();
            if (id == Block.AIR) continue;

            if (id == Block.CAMPFIRE_BLOCK || id == Block.SOUL_CAMPFIRE_BLOCK) {
                collisionBlocks.add(block);
                continue;
            }

            if (id == Block.NETHER_PORTAL || id == Block.END_PORTAL) {
                AxisAlignedBB portalBB = new SimpleAxisAlignedBB(block.x, block.y, block.z, block.x + 1, block.y + 1, block.z + 1);
                if (trajectoryBB.intersectsWith(portalBB)) {
                    collisionBlocks.add(block);
                }
            } else {
                AxisAlignedBB blockBB = block.getBoundingBox();
                if (blockBB == null) {
                    blockBB = new SimpleAxisAlignedBB(block.x, block.y, block.z, block.x + 1, block.y + 1, block.z + 1);
                }
                if (trajectoryBB.intersectsWith(blockBB)) {
                    collisionBlocks.add(block);
                }
            }
        }

        collisionCache.put(cacheKey, collisionBlocks.isEmpty() ? Collections.emptyList() : new ArrayList<>(collisionBlocks));
        return collisionBlocks;
    }

    private List<Block> getBlocksInBoundingBoxFast(AxisAlignedBB bb, int maxBlocks) {
        Level level = entity.getLevel();
        int minX = NukkitMath.floorDouble(bb.getMinX());
        int minY = Math.max(NukkitMath.floorDouble(bb.getMinY()), level.getMinBlockY());
        int minZ = NukkitMath.floorDouble(bb.getMinZ());
        int maxX = NukkitMath.ceilDouble(bb.getMaxX());
        int maxY = Math.min(NukkitMath.ceilDouble(bb.getMaxY()), level.getMaxBlockY());
        int maxZ = NukkitMath.ceilDouble(bb.getMaxZ());

        if (minY > maxY) return Collections.emptyList();
        if (!level.isYInRange(minY) || !level.isYInRange(maxY)) return Collections.emptyList();

        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (totalBlocks > maxBlocks) {
            return sampleBlocksInArea(minX, minY, minZ, maxX, maxY, maxZ, maxBlocks);
        }

        List<Block> result = new ArrayList<>(totalBlocks);

        for (int x = minX; x <= maxX; x++) {
            int chunkX = x >> 4;
            int localX = x & 0x0f;
            for (int z = minZ; z <= maxZ; z++) {
                int chunkZ = z >> 4;
                int localZ = z & 0x0f;
                int chunkKey = encodeChunk(chunkX, chunkZ);
                FullChunk chunk = chunkCache.computeIfAbsent(chunkKey, k -> level.getChunkIfLoaded(chunkX, chunkZ));
                if (chunk == null) continue;

                for (int y = minY; y <= maxY; y++) {
                    if (!level.isYInRange(y)) continue;

                    long blockKey = ((long) x << 32) | (z & 0xFFFFFFFFL) | ((long) y << 32);
                    Block block = blockCache.get(blockKey);
                    if (block == null) {
                        int blockId = chunk.getBlockId(localX, y, localZ);
                        int blockData = chunk.getBlockData(localX, y, localZ);
                        block = Block.get(blockId, blockData, level, x, y, z);
                        if (blockId != Block.AIR) {
                            blockCache.put(blockKey, block);
                        }
                    }

                    if (block.getId() != Block.AIR) {
                        result.add(block);
                    }
                }
            }
        }

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

    private long calculateCacheKey(AxisAlignedBB bb, double motionX, double motionY, double motionZ) {
        long x = (long) (bb.getMinX() * 1000);
        long y = (long) (bb.getMinY() * 1000);
        long z = (long) (bb.getMinZ() * 1000);
        long mx = (long) (motionX * 1000);
        long my = (long) (motionY * 1000);
        long mz = (long) (motionZ * 1000);
        return x ^ (y << 16) ^ (z << 32) ^ mx ^ (my << 8) ^ (mz << 16);
    }

    private boolean hasRelevantChunkChanges(AxisAlignedBB bb) {
        Level level = entity.getLevel();
        int minChunkX = NukkitMath.floorDouble(bb.getMinX()) >> 4;
        int maxChunkX = NukkitMath.ceilDouble(bb.getMaxX()) >> 4;
        int minChunkZ = NukkitMath.floorDouble(bb.getMinZ()) >> 4;
        int maxChunkZ = NukkitMath.ceilDouble(bb.getMaxZ()) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (changedChunks.contains(encodeChunk(cx, cz))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNearDangerousBlocks(AxisAlignedBB bb) {
        AxisAlignedBB safetyBB = bb.grow(2, 2, 2);
        return isInsideSpecialBlock(safetyBB, Block.LAVA) ||
                isInsideSpecialBlock(safetyBB, Block.FIRE) ||
                isInsideSpecialBlock(safetyBB, Block.CACTUS) ||
                isInsideSpecialBlock(safetyBB, Block.CAMPFIRE_BLOCK) ||
                isInsideSpecialBlock(safetyBB, Block.SOUL_CAMPFIRE_BLOCK);
    }

    private int calculateMaxBlocks(double speedSq) {
        int baseLimit = 512;
        int speedBonus = (int) (speedSq * 1000);
        return Math.min(baseLimit + speedBonus, 2048);
    }

    private List<Block> sampleBlocksInArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int maxSamples) {
        List<Block> samples = new ArrayList<>();
        Random random = new Random();
        int rangeX = maxX - minX + 1;
        int rangeY = maxY - minY + 1;
        int rangeZ = maxZ - minZ + 1;
        Level level = entity.getLevel();

        for (int i = 0; i < maxSamples; i++) {
            int x = minX + random.nextInt(rangeX);
            int y = minY + random.nextInt(rangeY);
            int z = minZ + random.nextInt(rangeZ);
            Block block = level.getBlock(x, y, z);
            if (block.getId() != Block.AIR) samples.add(block);
        }
        return samples;
    }

    public boolean isInsideSpecialBlock(AxisAlignedBB bb, int targetBlockId) {
        long currentTick = entity.getServer().getTick();
        if (lastCheckedBB != null && lastCheckedBB.equals(bb) && currentTick == lastSpecialCheckTick) {
            return switch (targetBlockId) {
                case Block.FIRE -> (insideSpecialCache & 1) != 0;
                case Block.LAVA -> (insideSpecialCache & 2) != 0;
                case Block.WATER -> (insideSpecialCache & 4) != 0;
                case Block.CACTUS -> (insideSpecialCache & 8) != 0;
                case Block.CAMPFIRE_BLOCK, Block.SOUL_CAMPFIRE_BLOCK -> (insideSpecialCache & 16) != 0;
                default -> false;
            };
        }

        lastCheckedBB = bb.clone();
        lastSpecialCheckTick = currentTick;
        insideSpecialCache = 0;

        Level level = entity.getLevel();
        int minX = NukkitMath.floorDouble(bb.getMinX());
        int minY = Math.max(NukkitMath.floorDouble(bb.getMinY()), level.getMinBlockY());
        int minZ = NukkitMath.floorDouble(bb.getMinZ());
        int maxX = NukkitMath.ceilDouble(bb.getMaxX());
        int maxY = Math.min(NukkitMath.ceilDouble(bb.getMaxY()), level.getMaxBlockY());
        int maxZ = NukkitMath.ceilDouble(bb.getMaxZ());

        boolean foundFire = false, foundLava = false, foundWater = false, foundCactus = false, foundCampfire = false;

        for (int y = minY; y <= maxY; y++) {
            if (!level.isYInRange(y)) continue;
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int id = level.getBlockIdAt(x, y, z);
                    switch (id) {
                        case Block.FIRE -> foundFire = true;
                        case Block.LAVA, Block.STILL_LAVA -> foundLava = true;
                        case Block.WATER, Block.STILL_WATER -> foundWater = true;
                        case Block.CACTUS -> foundCactus = true;
                        case Block.CAMPFIRE_BLOCK, Block.SOUL_CAMPFIRE_BLOCK -> foundCampfire = true;
                    }
                    if (targetBlockId == Block.FIRE && foundFire) {
                        insideSpecialCache |= 1;
                        return true;
                    }
                    if (targetBlockId == Block.LAVA && foundLava) {
                        insideSpecialCache |= 2;
                        return true;
                    }
                    if (targetBlockId == Block.WATER && foundWater) {
                        insideSpecialCache |= 4;
                        return true;
                    }
                    if (targetBlockId == Block.CACTUS && foundCactus) {
                        insideSpecialCache |= 8;
                        return true;
                    }
                    if ((targetBlockId == Block.CAMPFIRE_BLOCK || targetBlockId == Block.SOUL_CAMPFIRE_BLOCK) && foundCampfire) {
                        insideSpecialCache |= 16;
                        return true;
                    }
                }
            }
        }

        if (foundFire) insideSpecialCache |= 1;
        if (foundLava) insideSpecialCache |= 2;
        if (foundWater) insideSpecialCache |= 4;
        if (foundCactus) insideSpecialCache |= 8;
        if (foundCampfire) insideSpecialCache |= 16;

        return switch (targetBlockId) {
            case Block.FIRE -> foundFire;
            case Block.LAVA -> foundLava;
            case Block.WATER -> foundWater;
            case Block.CACTUS -> foundCactus;
            default -> false;
        };
    }

    public List<Block> getBlocksInBoundingBox(AxisAlignedBB bb) {
        return getBlocksInBoundingBoxFast(bb, 512);
    }

    private void invalidateChunk(int chunkX, int chunkZ) {
        int minX = chunkX << 4;
        int maxX = minX + 15;
        int minZ = chunkZ << 4;
        int maxZ = minZ + 15;

        blockCache.entrySet().removeIf(entry -> {
            long key = entry.getKey();
            int x = (int) (key >> 32);
            int z = (int) (key & 0xFFFFFFFFL);
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        });

        if (isEntityNearChunk(chunkX, chunkZ)) {
            collisionCache.clear();
            lastCheckedBB = null;
        }
    }

    private boolean isEntityNearChunk(int chunkX, int chunkZ) {
        int eChunkX = entity.getChunkX();
        int eChunkZ = entity.getChunkZ();
        return Math.abs(eChunkX - chunkX) <= 2 && Math.abs(eChunkZ - chunkZ) <= 2;
    }

    private int encodeChunk(int x, int z) {
        return (x << 16) | (z & 0xFFFF);
    }

    private void cleanupChangedChunks() {
        long currentTick = entity.getServer().getTick();
        if (currentTick % 100 == 0) {
            int eChunkX = entity.getChunkX();
            int eChunkZ = entity.getChunkZ();
            changedChunks.removeIf(encoded -> {
                int cx = encoded >> 16;
                int cz = encoded;
                return Math.abs(cx - eChunkX) > 3 || Math.abs(cz - eChunkZ) > 3;
            });
        }
    }

    @Override
    public void onBlockChanged(Vector3 pos) {
        int chunkX = pos.getFloorX() >> 4;
        int chunkZ = pos.getFloorZ() >> 4;
        changedChunks.add(encodeChunk(chunkX, chunkZ));
    }

    @Override
    public void onChunkChanged(FullChunk chunk) {
        if (chunk != null) {
            changedChunks.add(encodeChunk(chunk.getX(), chunk.getZ()));
            invalidateChunk(chunk.getX(), chunk.getZ());
        }
    }

    @Override
    public void onChunkLoaded(FullChunk chunk) {}

    @Override
    public void onChunkUnloaded(FullChunk chunk) {
        if (chunk != null) {
            int chunkKey = encodeChunk(chunk.getX(), chunk.getZ());
            chunkCache.remove(chunkKey);
            invalidateChunk(chunk.getX(), chunk.getZ());
        }
    }

    @Override
    public void onChunkPopulated(FullChunk chunk) {}

    @Override
    public int getLoaderId() {
        return 0;
    }

    @Override
    public boolean isLoaderActive() {
        return false;
    }

    @Override
    public Position getPosition() {
        return entity.getPosition();
    }

    @Override
    public double getX() {
        return entity.getChunkX();
    }

    @Override
    public double getZ() {
        return entity.getChunkZ();
    }

    @Override
    public Level getLevel() {
        return entity.getLevel();
    }
}