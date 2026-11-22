package com.xiaoyu.gocstrike;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public abstract class TimedEntity extends Entity {
    private static final int START_TICK = 20;
    private static final int STARTED = 21;
    protected UUID owner;

    protected TimedEntity(World world, float size) {
        super(world);
        setSize(size, size);
        noClip = true;
        isImmuneToFire = true;
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(START_TICK, 0);
        dataWatcher.addObject(STARTED, (byte) 0);
    }

    protected final void start() {
        dataWatcher.updateObject(START_TICK, (int) worldObj.getTotalWorldTime());
        dataWatcher.updateObject(STARTED, (byte) 1);
    }

    public final boolean hasStarted() {
        return dataWatcher.getWatchableObjectByte(STARTED) != 0;
    }

    public final float elapsed(float partialTick) {
        return hasStarted() ? secondsSince(dataWatcher.getWatchableObjectInt(START_TICK), partialTick) : 0;
    }

    protected final float secondsSince(int tick, float partialTick) {
        // 1.7.10 的 DataWatcher 没有长整数。用有符号差值保留短时间轴跨越溢出边界的行为。
        int ticks = (int) worldObj.getTotalWorldTime() - tick;
        return Math.max(0F, (ticks + partialTick) / 20F);
    }

    public final boolean ownedBy(EntityPlayer player) {
        return player.getUniqueID().equals(owner);
    }

    public Vec3 renderPosition(float partialTick) {
        return Vec3.createVectorHelper(
                prevPosX + (posX - prevPosX) * partialTick,
                prevPosY + (posY - prevPosY) * partialTick,
                prevPosZ + (posZ - prevPosZ) * partialTick);
    }

    @Override
    public final boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }

    @Override
    public final boolean writeMountToNBT(NBTTagCompound tag) {
        return false;
    }

    @Override
    protected final void readEntityFromNBT(NBTTagCompound tag) {
    }

    @Override
    protected final void writeEntityToNBT(NBTTagCompound tag) {
    }
}
