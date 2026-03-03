package cn.nukkit.entity;

import cn.nukkit.block.*;
import cn.nukkit.entity.mob.EntityDrowned;
import cn.nukkit.entity.passive.*;
import cn.nukkit.entity.route.RouteFinder;
import cn.nukkit.entity.route.RouteFinderSearchTask;
import cn.nukkit.entity.route.RouteFinderThreadPool;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.BubbleParticle;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.Utils;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.math3.util.FastMath;

public abstract class EntityWalking extends BaseEntity {
    private static final double FLOW_MULTIPLIER = 0.1;

    private int targetCheckTick = 0;
    private int waterCheckTick = 0;

    @Getter
    @Setter
    protected RouteFinder route;

    public EntityWalking(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    protected void checkTarget() {
        if (this.isKnockback()) return;

        if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive() &&
                this.followTarget.canBeFollowed() && targetOption((EntityCreature) this.followTarget,
                this.distanceSquared(this.followTarget)) && this.target != null) {
            return;
        }

        this.followTarget = null;

        if (!this.passengers.isEmpty() && !(this instanceof EntityLlama) && !(this instanceof EntityPig)) {
            return;
        }

        Entity[] nearby = this.getLevel().getNearbyEntities(EntityRanges.createTargetSearchBox(this), this, false, true);
        for (Entity entity : nearby) {
            if (entity == this || !(entity instanceof EntityCreature creature) || entity.closed || !this.canTarget(entity)) {
                continue;
            }

            if (creature instanceof BaseEntity baseEntity && baseEntity.isFriendly() == this.isFriendly() && !this.isInLove()) {
                continue;
            }

            double distance = this.distanceSquared(creature);
            if (!this.targetOption(creature, distance)) {
                continue;
            }

            this.stayTime = 0;
            this.moveTime = 0;
            this.followTarget = creature;
            if (this.route == null && this.passengers.isEmpty()) {
                this.target = creature;
            }
        }

        if (!this.canSetTemporalTarget()) return;

        if (this.stayTime > 0) {
            if (Utils.rand(1, 100) > 5) return;
            this.target = this.add(Utils.rand(-30, 30), Utils.rand(-20.0, 20.0) / 10, Utils.rand(-30, 30));
        } else if (Utils.rand(1, 100) == 1) {
            this.stayTime = Utils.rand(80, 200);
            this.target = this.add(Utils.rand(-30, 30), Utils.rand(-20.0, 20.0) / 10, Utils.rand(-30, 30));
        } else if (this.moveTime <= 0 || this.target == null) {
            this.stayTime = 0;
            this.moveTime = Utils.rand(80, 200);
            double tx = this.x;
            double tz = this.z;
            int attempts = 0;
            boolean inWater = true;
            while (attempts++ < 10 && inWater) {
                tx = this.x + Utils.rand(-30, 30);
                tz = this.z + Utils.rand(-30, 30);
                int txFloor = NukkitMath.floorDouble(tx);
                int tzFloor = NukkitMath.floorDouble(tz);
                inWater = Block.isWater(level.getBlockIdAt(chunk, txFloor, level.getHighestBlockAt(txFloor, tzFloor), tzFloor));
            }
            this.target = new Vector3(tx, this.y + Utils.rand(-20.0, 20.0) / 10, tz);
        }
    }

    protected boolean checkJump(double dx, double dz) {
        if (this.motionY == this.getGravity() * 2) {
            return this.canSwimIn(level.getBlockIdAt(chunk, NukkitMath.floorDouble(this.x), (int) this.y, NukkitMath.floorDouble(this.z)));
        }

        if (this.canSwimIn(level.getBlockIdAt(chunk, NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z)))) {
            if (!(this instanceof EntityDrowned || this instanceof EntityIronGolem || this instanceof EntitySkeletonHorse) || this.target == null) {
                this.motionY = this.getGravity() * 2;
            }
            return true;
        }

        if (!this.onGround || this.stayTime > 0) return false;

        Block that = this.getLevel().getBlock(new Vector3(NukkitMath.floorDouble(this.x + dx), (int) this.y, NukkitMath.floorDouble(this.z + dz)));
        Block block = that.getSide(this.getHorizontalFacing());

        if (this.followTarget == null && this.passengers.isEmpty() && !block.down().isSolid() && !block.isSolid() && !block.down().down().isSolid()) {
            this.stayTime = 10;
        } else if (!block.canPassThrough() && !(block instanceof BlockFlowable || block.getId() == BlockID.SOUL_SAND) && block.up().canPassThrough() && that.up(2).canPassThrough()) {

            this.motionY = switch (block) {
                case BlockFence _, BlockFenceGate _ -> this.getGravity();
                case BlockStairs _ -> {
                    if (this.motionY <= this.getGravity() * 4) {
                        yield this.getGravity() * 4;
                    }
                    yield this.motionY;
                }
                default -> {
                    if (this.motionY <= this.getGravity() * 4) {
                        yield this.getGravity() * 4;
                    } else if (this.motionY <= this.getGravity() * 8) {
                        yield this.getGravity() * 8;
                    } else {
                        yield this.motionY + this.getGravity() * 0.25;
                    }
                }
            };

            return true;
        }
        return false;
    }

    @Override
    public Vector3 updateMove(int tickDiff) {
        if (!this.isInTickingRange()) return null;

        if (++waterCheckTick >= 5) {
            waterCheckTick = 0;
        }

        if (this.isImmobile()) return null;

        if (this.age % 10 == 0 && this.route != null && !this.route.isSearching()) {
            RouteFinderThreadPool.executeRouteFinderThread(new RouteFinderSearchTask(this.route));
            if (this.route.hasNext()) {
                this.target = this.route.next();
            }
        }

        if (this.isKnockback()) {
            if (this.riding == null) {
                this.move(this.motionX, this.motionY, this.motionZ);
                if (this instanceof EntityDrowned && this.isInsideOfWater()) {
                    this.motionY -= this.getGravity() * 0.3;
                } else {
                    this.motionY -= this.getGravity();
                }
                this.updateMovement();
            }
            return this.followTarget != null ? this.followTarget : this.target;
        }

        Block currentBlock = this.getLevelBlock();
        int currentId = currentBlock.getId();
        Block downBlock = this.level.getBlock(this.chunk, this.getFloorX(), this.getFloorY() - 1, this.getFloorZ(), false);
        int downId = downBlock.getId();
        boolean inWater = currentId == BlockID.WATER || currentId == BlockID.STILL_WATER;

        if (inWater && (downId == 0 || downId == BlockID.WATER || downId == BlockID.STILL_WATER ||
                downId == BlockID.LAVA || downId == BlockID.STILL_LAVA ||
                downId == BlockID.SIGN_POST || downId == BlockID.WALL_SIGN)) {
            this.onGround = false;
        }
        if (downId == 0 || downId == BlockID.SIGN_POST || downId == BlockID.WALL_SIGN) {
            this.onGround = false;
        }

        if (this.getServer().getMobAiEnabled()) {
            if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive() &&
                    this.target != null && this.followTarget.canBeFollowed()) {
                this.processTargetMovement(this.followTarget, currentBlock, currentId, inWater, true);
            }

            if (this.isLookupForTarget() && ++this.targetCheckTick >= 3) {
                this.targetCheckTick = 0;
                this.checkTarget();
            }

            if (this.target != null) {
                this.processTargetMovement(this.target, currentBlock, currentId, inWater, false);
            }
        }

        double dx = this.motionX;
        double dz = this.motionZ;
        boolean isJump = this.checkJump(dx, dz);

        if (this.stayTime > 0 && !inWater) {
            this.stayTime -= tickDiff;
            this.move(0, this.motionY, 0);
        } else {
            if (this.onGround) {
                double friction = (1.0 - this.getDrag()) * downBlock.getFrictionFactor();
                this.motionX *= friction;
                this.motionZ *= friction;
            }
            Vector2 before = new Vector2(this.x + this.motionX, this.z + this.motionZ);
            this.move(this.motionX, this.motionY, this.motionZ);
            Vector2 after = new Vector2(this.x, this.z);

            if ((before.x != after.x || before.y != after.y) && !isJump) {
                this.moveTime -= 90;
            }
        }

        this.applyGravity(isJump, inWater);
        this.updateMovement();

        if (this.route != null && this.route.hasCurrentNode() && this.route.hasArrivedNode(this) && this.route.hasNext()) {
            this.target = this.route.next();
        }

        return this.followTarget != null ? this.followTarget : this.target;
    }

    private void processTargetMovement(Vector3 target, Block currentBlock, int currentId, boolean inWater, boolean isFollow) {
        double x = target.x - this.x;
        double z = target.z - this.z;
        double diff = Math.abs(x) + Math.abs(z);

        if (this.riding != null || diff <= 0.001 || (!inWater && (this.stayTime > 0 ||
                this.distance(target) <= (this.getWidth() / 2.0 + 0.3) * this.nearbyDistanceMultiplier()))) {
            if (!this.isInsideOfWater()) {
                this.motionX = 0.0;
                this.motionZ = 0.0;
            }
            return;
        }

        // Используем switch для оптимизации
        switch (currentId) {
            case BlockID.WATER -> {
                if (currentBlock instanceof BlockWater blockWater) {
                    Vector3 flowVector = blockWater.getFlowVector();
                    this.motionX = flowVector.getX() * FLOW_MULTIPLIER;
                    this.motionZ = flowVector.getZ() * FLOW_MULTIPLIER;
                }
            }
            case BlockID.STILL_WATER -> {
                double speed = this.getSpeed() * this.moveMultiplier * 0.05;
                this.motionX = speed * (x / diff);
                this.motionZ = speed * (z / diff);

                if (!(this instanceof EntityDrowned || this instanceof EntityIronGolem || this instanceof EntitySkeletonHorse) &&
                        Utils.rand(1, 10) == 1) {
                    this.level.addParticle(new BubbleParticle(this.add(
                            Utils.rand(-2.0, 2.0), Utils.rand(-0.5, 0.0), Utils.rand(-2.0, 2.0))));
                }

                if (isFollow && this.followTarget != null) {
                    double y = this.followTarget.y - this.y;
                    this.motionY = speed * (y / (diff + Math.abs(y)));
                }
            }
            default -> {
                double speed = this.getSpeed() * this.moveMultiplier * (isFollow ? 0.1 : 0.15);
                this.motionX = speed * (x / diff);
                this.motionZ = speed * (z / diff);
            }
        }

        if (this.noRotateTicks <= 0 && (this.passengers.isEmpty() || this instanceof EntityLlama || this instanceof EntityPig) &&
                (this.stayTime <= 0 || Utils.rand()) && diff > 0.001) {
            this.setBothYaw(FastMath.toDegrees(-FastMath.atan2(x / diff, z / diff)));
        }
    }

    private void applyGravity(boolean isJump, boolean inWater) {
        if (isJump) return;

        if (this.onGround && !inWater) {
            this.motionY = 0.0;
        } else if (this.motionY > -this.getGravity() * 4.0) {
            Block blockAbove = this.level.getBlock(
                    NukkitMath.floorDouble(this.x),
                    (int) (this.y + 0.8),
                    NukkitMath.floorDouble(this.z)
            );
            if (!(blockAbove instanceof BlockLiquid)) {
                this.motionY -= this.getGravity();
            }
        } else {
            if ((this instanceof EntityDrowned || this instanceof EntityIronGolem || this instanceof EntitySkeletonHorse) &&
                    inWater && this.motionY < 0.0) {
                this.motionY = this.getGravity() * -0.3;
                this.stayTime = 40;
            } else {
                this.motionY -= this.getGravity();
            }
        }
    }
}