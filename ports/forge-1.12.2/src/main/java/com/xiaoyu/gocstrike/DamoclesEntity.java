package com.xiaoyu.gocstrike;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

public final class DamoclesEntity extends TimedEntity {
    private static final DataParameter<Float> LIFT = EntityDataManager.createKey(DamoclesEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> HEIGHT = EntityDataManager.createKey(DamoclesEntity.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> TARGET_ID = EntityDataManager.createKey(DamoclesEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> IMPACT_TICK = EntityDataManager.createKey(DamoclesEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> IMPACTED = EntityDataManager.createKey(DamoclesEntity.class, DataSerializers.BOOLEAN);
    private static final double FLIGHT_SPEED = 1.8;
    private static final int FLIGHT_TICKS = 36;
    private static final float MAX_LIFT = 9;

    private EntityLivingBase captive;
    private EntityPlayer caster;
    private ForgeChunkManager.Ticket ticket;
    private float safeLift;
    private int lerpTicks;
    private double lerpX, lerpY, lerpZ;
    private float lerpYaw, lerpPitch;

    public DamoclesEntity(World world) {
        super(world, 0.1F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(LIFT, 0F);
        dataManager.register(HEIGHT, 0F);
        dataManager.register(TARGET_ID, -1);
        dataManager.register(IMPACT_TICK, 0);
        dataManager.register(IMPACTED, false);
    }

    void begin(EntityPlayer player) {
        owner = player.getUniqueID();
        caster = player;
        setPositionAndRotation(player.posX, player.getEntityBoundingBox().minY + player.getEyeHeight() - 0.12,
                player.posZ, player.rotationYaw, player.rotationPitch);
        Vec3d direction = player.getLookVec();
        motionX = direction.x * FLIGHT_SPEED;
        motionY = direction.y * FLIGHT_SPEED;
        motionZ = direction.z * FLIGHT_SPEED;
    }

    public boolean hasLocked() {
        return hasStarted();
    }

    private boolean impacted() {
        return dataManager.get(IMPACTED);
    }

    public float renderTime(float partialTick) {
        if (impacted()) {
            return DamoclesTimeline.IMPACT + secondsSince(dataManager.get(IMPACT_TICK), partialTick);
        }
        return Math.min(elapsed(partialTick), Math.nextDown(DamoclesTimeline.IMPACT));
    }

    public float lift(float time) {
        return dataManager.get(LIFT) * DamoclesTimeline.ease(time, DamoclesTimeline.LIFT_START, DamoclesTimeline.LIFT_END);
    }

    public float captiveHeight() {
        return dataManager.get(HEIGHT);
    }

    public Vec3d captiveFeet(float partialTick) {
        Entity target = world.getEntityByID(dataManager.get(TARGET_ID));
        if (target != null && target.isEntityAlive()) {
            return new Vec3d(
                    target.prevPosX + (target.posX - target.prevPosX) * partialTick,
                    target.prevPosY + (target.posY - target.prevPosY) * partialTick + target.getEntityBoundingBox().minY - target.posY,
                    target.prevPosZ + (target.posZ - target.prevPosZ) * partialTick);
        }
        return renderPosition(partialTick).add(0, lift(elapsed(partialTick)), 0);
    }

    public Vec3d flightDirection(float partialTick) {
        float pitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * partialTick;
        float yaw = prevRotationYaw + MathHelper.wrapDegrees(rotationYaw - prevRotationYaw) * partialTick;
        double p = Math.toRadians(pitch);
        double y = Math.toRadians(yaw);
        return new Vec3d(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            interpolateFlight();
            return;
        }
        if (!impacted() && (caster == null || !caster.isEntityAlive() || caster.world != world
                || !world.playerEntities.contains(caster))) {
            cancel();
        } else if (hasStarted()) {
            tickLocked();
        } else {
            tickFlight();
        }
    }

    @Override
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int ticks, boolean teleport) {
        if (!world.isRemote || hasStarted() || ticks <= 0) {
            super.setPositionAndRotationDirect(x, y, z, yaw, pitch, ticks, teleport);
            lerpTicks = 0;
            return;
        }
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYaw = yaw;
        lerpPitch = pitch;
        lerpTicks = ticks;
    }

    private void interpolateFlight() {
        if (lerpTicks > 0) {
            double x = posX + (lerpX - posX) / lerpTicks;
            double y = posY + (lerpY - posY) / lerpTicks;
            double z = posZ + (lerpZ - posZ) / lerpTicks;
            rotationYaw += MathHelper.wrapDegrees(lerpYaw - rotationYaw) / lerpTicks;
            rotationPitch += (lerpPitch - rotationPitch) / lerpTicks;
            setPosition(x, y, z);
            lerpTicks--;
        }
    }

    private void tickFlight() {
        Vec3d from = new Vec3d(posX, posY, posZ);
        Vec3d to = from.add(motionX, motionY, motionZ);
        if (ticksExisted > FLIGHT_TICKS || !world.isBlockLoaded(new BlockPos(to))) {
            setDead();
            return;
        }
        // 先截断到实心方块处，再从同一段路径中选择第一个生物。
        RayTraceResult wall = world.rayTraceBlocks(from, to, false, true, false);
        Vec3d end = wall == null ? to : wall.hitVec;
        AxisAlignedBB search = getEntityBoundingBox().expand(end.x - posX, end.y - posY, end.z - posZ).grow(0.3);
        EntityLivingBase nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (Object entry : world.getEntitiesWithinAABBExcludingEntity(this, search)) {
            if (!(entry instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase candidate = (EntityLivingBase) entry;
            if (!candidate.isEntityAlive() || !candidate.canBeCollidedWith() || candidate.getUniqueID().equals(owner)
                    || candidate instanceof EntityPlayer && ((EntityPlayer) candidate).isSpectator()) {
                continue;
            }
            AxisAlignedBB bounds = candidate.getEntityBoundingBox().grow(0.3);
            RayTraceResult hit = bounds.calculateIntercept(from, end);
            double distance = bounds.contains(from) ? 0 : hit == null ? Double.POSITIVE_INFINITY : hit.hitVec.squareDistanceTo(from);
            if (distance < best) {
                nearest = candidate;
                best = distance;
            }
        }
        if (nearest != null) {
            for (Object entity : world.loadedEntityList) {
                if (entity instanceof DamoclesEntity && !((DamoclesEntity) entity).isDead
                        && ((DamoclesEntity) entity).captive == nearest) {
                    setDead();
                    return;
                }
            }
            lock(nearest);
        } else if (wall != null) {
            setDead();
        } else {
            setPosition(to.x, to.y, to.z);
        }
    }

    private void lock(EntityLivingBase target) {
        captive = target;
        stopMoving(target);
        setPosition(target.posX, target.getEntityBoundingBox().minY, target.posZ);
        motionX = motionY = motionZ = 0;

        // 用生物本身的碰撞箱量出可用高度。
        float lift = 0;
        for (float step = 0.25F; step <= MAX_LIFT; step += 0.25F) {
            if (target.getEntityBoundingBox().maxY + step >= world.provider.getActualHeight()
                    || !world.getCollisionBoxes(target, target.getEntityBoundingBox().offset(0, step, 0)).isEmpty()) {
                break;
            }
            lift = step;
        }
        dataManager.set(LIFT, lift);
        dataManager.set(HEIGHT, target.height);
        dataManager.set(TARGET_ID, target.getEntityId());
        ticket = ForgeChunkManager.requestTicket(GOCStrike.instance, world, ForgeChunkManager.Type.NORMAL);
        if (ticket != null) {
            ForgeChunkManager.forceChunk(ticket, new ChunkPos(MathHelper.floor(posX) >> 4, MathHelper.floor(posZ) >> 4));
        }
        start();
    }

    private void tickLocked() {
        float time = elapsed(0);
        if (impacted()) {
            if (renderTime(0) >= DamoclesTimeline.END) {
                setDead();
            }
            return;
        }

        if (validCaptive()) {
            float currentLift = lift(time);
            if (spaceForCaptive(posY + currentLift)) {
                moveCaptive(posY + currentLift);
                safeLift = currentLift;
            } else if (currentLift > safeLift) {
                dataManager.set(LIFT, safeLift / DamoclesTimeline.ease(time, DamoclesTimeline.LIFT_START, DamoclesTimeline.LIFT_END));
            }
        } else if (captive != null) {
            captive = null;
            dataManager.set(TARGET_ID, -1);
        }
        if (time >= DamoclesTimeline.IMPACT) {
            dataManager.set(IMPACT_TICK, (int) world.getTotalWorldTime());
            dataManager.set(IMPACTED, true);
            double contactY = posY + lift(time) + captiveHeight();
            world.playSound(null, posX, contactY, posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 10, 0.66F);
            world.playSound(null, posX, contactY, posZ, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 9, 1.16F);
            EntityLivingBase target = captive;
            captive = null;
            dataManager.set(TARGET_ID, -1);
            if (target != null) {
                try {
                    StrikeEntity.eraseTarget(target);
                } catch (RuntimeException exception) {
                    GOCStrike.LOGGER.error("Damocles strike failed for {}", target.getUniqueID(), exception);
                }
            }
        }
    }

    private boolean validCaptive() {
        return captive != null && captive.world == world && captive.isEntityAlive()
                && (!(captive instanceof EntityPlayer) || !((EntityPlayer) captive).isSpectator())
                && world.getEntityByID(captive.getEntityId()) == captive;
    }

    private boolean spaceForCaptive(double feetY) {
        return world.getCollisionBoxes(captive, captive.getEntityBoundingBox().offset(
                posX - captive.posX, feetY - captive.getEntityBoundingBox().minY, posZ - captive.posZ)).isEmpty();
    }

    private static void stopMoving(EntityLivingBase entity) {
        if (entity.isRiding()) {
            entity.dismountRidingEntity();
        }
        entity.motionX = entity.motionY = entity.motionZ = 0;
        entity.fallDistance = 0;
    }

    private void moveCaptive(double feetY) {
        stopMoving(captive);
        double y = feetY + captive.posY - captive.getEntityBoundingBox().minY;
        if (captive.getDistanceSq(posX, y, posZ) > 0.0004) {
            if (captive instanceof EntityPlayerMP) {
                ((EntityPlayerMP) captive).setPositionAndUpdate(posX, y, posZ);
            } else {
                captive.setPosition(posX, y, posZ);
            }
        }
    }

    public void cancel() {
        if (validCaptive() && spaceForCaptive(posY)) {
            moveCaptive(posY);
        }
        captive = null;
        setDead();
    }

    @Override
    public void setDead() {
        if (ticket != null) {
            ForgeChunkManager.releaseTicket(ticket);
            ticket = null;
        }
        super.setDead();
    }
}
