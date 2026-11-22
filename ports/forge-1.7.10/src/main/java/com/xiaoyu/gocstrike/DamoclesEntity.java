package com.xiaoyu.gocstrike;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;

public final class DamoclesEntity extends TimedEntity {
    private static final int LIFT = 22;
    private static final int HEIGHT = 23;
    private static final int TARGET_ID = 24;
    private static final int IMPACT_TICK = 25;
    private static final int IMPACTED = 26;
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
        dataWatcher.addObject(LIFT, 0F);
        dataWatcher.addObject(HEIGHT, 0F);
        dataWatcher.addObject(TARGET_ID, -1);
        dataWatcher.addObject(IMPACT_TICK, 0);
        dataWatcher.addObject(IMPACTED, (byte) 0);
    }

    void begin(EntityPlayer player) {
        owner = player.getUniqueID();
        caster = player;
        setPositionAndRotation(player.posX, player.boundingBox.minY + player.getEyeHeight() - 0.12,
                player.posZ, player.rotationYaw, player.rotationPitch);
        Vec3 direction = player.getLookVec();
        motionX = direction.xCoord * FLIGHT_SPEED;
        motionY = direction.yCoord * FLIGHT_SPEED;
        motionZ = direction.zCoord * FLIGHT_SPEED;
    }

    public boolean hasLocked() {
        return hasStarted();
    }

    private boolean impacted() {
        return dataWatcher.getWatchableObjectByte(IMPACTED) != 0;
    }

    public float renderTime(float partialTick) {
        if (impacted()) {
            return DamoclesTimeline.IMPACT + secondsSince(dataWatcher.getWatchableObjectInt(IMPACT_TICK), partialTick);
        }
        return Math.min(elapsed(partialTick), Math.nextDown(DamoclesTimeline.IMPACT));
    }

    public float lift(float time) {
        return dataWatcher.getWatchableObjectFloat(LIFT) * DamoclesTimeline.ease(time, DamoclesTimeline.LIFT_START, DamoclesTimeline.LIFT_END);
    }

    public float captiveHeight() {
        return dataWatcher.getWatchableObjectFloat(HEIGHT);
    }

    public Vec3 captiveFeet(float partialTick) {
        Entity target = worldObj.getEntityByID(dataWatcher.getWatchableObjectInt(TARGET_ID));
        if (target != null && target.isEntityAlive()) {
            return Vec3.createVectorHelper(
                    target.prevPosX + (target.posX - target.prevPosX) * partialTick,
                    target.prevPosY + (target.posY - target.prevPosY) * partialTick + target.boundingBox.minY - target.posY,
                    target.prevPosZ + (target.posZ - target.prevPosZ) * partialTick);
        }
        return renderPosition(partialTick).addVector(0, lift(elapsed(partialTick)), 0);
    }

    public Vec3 flightDirection(float partialTick) {
        float pitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * partialTick;
        float yaw = prevRotationYaw + MathHelper.wrapAngleTo180_float(rotationYaw - prevRotationYaw) * partialTick;
        double p = Math.toRadians(pitch);
        double y = Math.toRadians(yaw);
        return Vec3.createVectorHelper(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (worldObj.isRemote) {
            interpolateFlight();
            return;
        }
        if (!impacted() && (caster == null || !caster.isEntityAlive() || caster.worldObj != worldObj
                || !worldObj.playerEntities.contains(caster))) {
            cancel();
        } else if (hasStarted()) {
            tickLocked();
        } else {
            tickFlight();
        }
    }

    @Override
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int ticks) {
        if (!worldObj.isRemote || hasStarted() || ticks <= 0) {
            super.setPositionAndRotation2(x, y, z, yaw, pitch, ticks);
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
            rotationYaw += MathHelper.wrapAngleTo180_float(lerpYaw - rotationYaw) / lerpTicks;
            rotationPitch += (lerpPitch - rotationPitch) / lerpTicks;
            setPosition(x, y, z);
            lerpTicks--;
        }
    }

    private void tickFlight() {
        Vec3 from = Vec3.createVectorHelper(posX, posY, posZ);
        Vec3 to = from.addVector(motionX, motionY, motionZ);
        if (ticksExisted > FLIGHT_TICKS || !worldObj.blockExists(MathHelper.floor_double(to.xCoord),
                MathHelper.floor_double(to.yCoord), MathHelper.floor_double(to.zCoord))) {
            setDead();
            return;
        }
        // 旧版方块射线会改写起点，传入副本以保留完整的生物碰撞区间。
        MovingObjectPosition wall = worldObj.func_147447_a(from.addVector(0, 0, 0), to, false, true, false);
        Vec3 end = wall == null ? to : wall.hitVec;
        AxisAlignedBB search = boundingBox.addCoord(end.xCoord - posX, end.yCoord - posY, end.zCoord - posZ).expand(0.3, 0.3, 0.3);
        EntityLivingBase nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (Object entry : worldObj.getEntitiesWithinAABBExcludingEntity(this, search)) {
            if (!(entry instanceof EntityLivingBase)) {
                continue;
            }
            EntityLivingBase candidate = (EntityLivingBase) entry;
            if (!candidate.isEntityAlive() || !candidate.canBeCollidedWith() || candidate.getUniqueID().equals(owner)) {
                continue;
            }
            AxisAlignedBB bounds = candidate.boundingBox.expand(0.3, 0.3, 0.3);
            MovingObjectPosition hit = bounds.calculateIntercept(from, end);
            double distance = bounds.isVecInside(from) ? 0 : hit == null ? Double.POSITIVE_INFINITY : hit.hitVec.squareDistanceTo(from);
            if (distance < best) {
                nearest = candidate;
                best = distance;
            }
        }
        if (nearest != null) {
            for (Object entity : worldObj.loadedEntityList) {
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
            setPosition(to.xCoord, to.yCoord, to.zCoord);
        }
    }

    private void lock(EntityLivingBase target) {
        captive = target;
        stopMoving(target);
        setPosition(target.posX, target.boundingBox.minY, target.posZ);
        motionX = motionY = motionZ = 0;

        // 用生物本身的碰撞箱量出可用高度。
        float lift = 0;
        for (float step = 0.25F; step <= MAX_LIFT; step += 0.25F) {
            if (target.boundingBox.maxY + step >= worldObj.provider.getActualHeight()
                    || !worldObj.getCollidingBoundingBoxes(target, target.boundingBox.getOffsetBoundingBox(0, step, 0)).isEmpty()) {
                break;
            }
            lift = step;
        }
        dataWatcher.updateObject(LIFT, lift);
        dataWatcher.updateObject(HEIGHT, target.height);
        dataWatcher.updateObject(TARGET_ID, target.getEntityId());
        ticket = ForgeChunkManager.requestTicket(GOCStrike.instance, worldObj, ForgeChunkManager.Type.NORMAL);
        if (ticket != null) {
            ForgeChunkManager.forceChunk(ticket, new ChunkCoordIntPair(MathHelper.floor_double(posX) >> 4, MathHelper.floor_double(posZ) >> 4));
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
                dataWatcher.updateObject(LIFT, safeLift / DamoclesTimeline.ease(time, DamoclesTimeline.LIFT_START, DamoclesTimeline.LIFT_END));
            }
        } else if (captive != null) {
            captive = null;
            dataWatcher.updateObject(TARGET_ID, -1);
        }
        if (time >= DamoclesTimeline.IMPACT) {
            dataWatcher.updateObject(IMPACT_TICK, (int) worldObj.getTotalWorldTime());
            dataWatcher.updateObject(IMPACTED, (byte) 1);
            double contactY = posY + lift(time) + captiveHeight();
            worldObj.playSoundEffect(posX, contactY, posZ, "random.explode", 10, 0.66F);
            worldObj.playSoundEffect(posX, contactY, posZ, "dig.glass", 9, 1.16F);
            EntityLivingBase target = captive;
            captive = null;
            dataWatcher.updateObject(TARGET_ID, -1);
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
        return captive != null && captive.worldObj == worldObj && captive.isEntityAlive()
                && worldObj.getEntityByID(captive.getEntityId()) == captive;
    }

    private boolean spaceForCaptive(double feetY) {
        return worldObj.getCollidingBoundingBoxes(captive, captive.boundingBox.getOffsetBoundingBox(
                posX - captive.posX, feetY - captive.boundingBox.minY, posZ - captive.posZ)).isEmpty();
    }

    private static void stopMoving(EntityLivingBase entity) {
        if (entity.ridingEntity != null) {
            entity.mountEntity(null);
        }
        entity.motionX = entity.motionY = entity.motionZ = 0;
        entity.fallDistance = 0;
    }

    private void moveCaptive(double feetY) {
        stopMoving(captive);
        double y = feetY + captive.posY - captive.boundingBox.minY;
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
