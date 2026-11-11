package com.xiaoyu.gocstrike;

import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class StrikeEntity extends Entity {
    public static final float DURATION = 118.698667F;
    public static final float WORLD_SCALE = 2.4F;
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(StrikeEntity.class, EntityDataSerializers.LONG);
    private UUID owner;

    public StrikeEntity(EntityType<? extends StrikeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public void begin(Player player) {
        owner = player.getUUID();
        setPos(player.getX(), player.getY(), player.getZ());
        entityData.set(START, level().getGameTime());
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().equals(owner);
    }

    public float elapsed(float partialTick) {
        long start = entityData.get(START);
        return start < 0 ? 0 : Math.max(0, (level().getGameTime() - start + partialTick) / 20.0F);
    }

    public boolean hasStarted() {
        return entityData.get(START) >= 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(START, -1L);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && elapsed(0) >= DURATION) {
            discard();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512.0 * 512.0;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return new AABB(getX() - 110, getY() - 5, getZ() - 110, getX() + 110, getY() + 160, getZ() + 110);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
