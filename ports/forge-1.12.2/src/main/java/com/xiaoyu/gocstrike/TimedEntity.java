package com.xiaoyu.gocstrike;

import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public abstract class TimedEntity extends Entity {
    private static final DataParameter<Integer> START_TICK = EntityDataManager.createKey(TimedEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> STARTED = EntityDataManager.createKey(TimedEntity.class, DataSerializers.BOOLEAN);
    protected UUID owner;

    protected TimedEntity(World world, float size) {
        super(world);
        setSize(size, size);
        noClip = true;
        isImmuneToFire = true;
    }

    @Override
    protected void entityInit() {
        dataManager.register(START_TICK, 0);
        dataManager.register(STARTED, false);
    }

    protected final void start() {
        dataManager.set(START_TICK, (int) world.getTotalWorldTime());
        dataManager.set(STARTED, true);
    }

    public final boolean hasStarted() {
        return dataManager.get(STARTED);
    }

    public final float elapsed(float partialTick) {
        return hasStarted() ? secondsSince(dataManager.get(START_TICK), partialTick) : 0;
    }

    protected final float secondsSince(int tick, float partialTick) {
        // 整数差值支持这段短时间轴跨越世界刻数的整数溢出边界。
        int ticks = (int) world.getTotalWorldTime() - tick;
        return Math.max(0F, (ticks + partialTick) / 20F);
    }

    public final boolean ownedBy(EntityPlayer player) {
        return player.getUniqueID().equals(owner);
    }

    public Vec3d renderPosition(float partialTick) {
        return new Vec3d(
                prevPosX + (posX - prevPosX) * partialTick,
                prevPosY + (posY - prevPosY) * partialTick,
                prevPosZ + (posZ - prevPosZ) * partialTick);
    }

    @Override
    public final boolean writeToNBTOptional(NBTTagCompound tag) {
        return false;
    }

    @Override
    public final boolean writeToNBTAtomically(NBTTagCompound tag) {
        return false;
    }

    @Override
    protected final void readEntityFromNBT(NBTTagCompound tag) {
    }

    @Override
    protected final void writeEntityToNBT(NBTTagCompound tag) {
    }
}
