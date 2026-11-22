package com.xiaoyu.gocstrike;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class StrikeEntity extends Entity {
    public static final float DURATION = 118.698667F;
    public static final float WORLD_SCALE = 2.4F;
    // 原编排在白光开始后 0.75 秒执行终击。
    private static final float FINAL_STRIKE_AT = 116.782F;
    private static final EntityDataAccessor<Long> START = SynchedEntityData.defineId(StrikeEntity.class, EntityDataSerializers.LONG);
    private UUID owner;
    private boolean finalStrikeApplied;

    public StrikeEntity(EntityType<? extends StrikeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        noCulling = true;
        setNoGravity(true);
    }

    public void begin(Player player) {
        owner = player.getUUID();
        setPos(player.getX(), player.getY(), player.getZ());
        entityData.set(START, ForgeWorld.level(this).getGameTime());
    }

    public boolean isOwnedBy(Player player) {
        return player.getUUID().equals(owner);
    }

    public float elapsed(float partialTick) {
        long start = entityData.get(START);
        return start < 0 ? 0 : Math.max(0, (ForgeWorld.level(this).getGameTime() - start + partialTick) / 20.0F);
    }

    public boolean hasStarted() {
        return entityData.get(START) >= 0;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(START, -1L);
    }

    @Override
    public void tick() {
        super.tick();
        if (ForgeWorld.level(this) instanceof ServerLevel serverLevel) {
            float time = elapsed(0);
            if (!finalStrikeApplied && hasStarted() && time >= FINAL_STRIKE_AT) {
                finalStrikeApplied = true;
                applyFinalStrike(serverLevel);
            }
            if (time >= DURATION) {
                discard();
            }
        }
    }

    private void applyFinalStrike(ServerLevel level) {
        // 死亡、登出和模组回调可能修改实体集合，先收集本维度中已加载的目标。
        var targets = new ArrayList<LivingEntity>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && !living.isRemoved()
                    && !(living instanceof Player player && player.isSpectator())) {
                targets.add(living);
            }
        }
        int removed = 0;
        int players = 0;
        for (LivingEntity target : targets) {
            if (target.isRemoved() || (target instanceof Player player && player.isSpectator())) {
                continue;
            }
            try {
                eraseTarget(target);
                if (target instanceof ServerPlayer) {
                    players++;
                } else {
                    removed++;
                }
            } catch (RuntimeException exception) {
                GOCStrike.LOGGER.error("Final strike failed for entity {}", target.getUUID(), exception);
            }
        }
        GOCStrike.LOGGER.info("Final strike in {}: removed {} living entities, processed {} players",
                level.dimension().location(), removed, players);
    }

    static void eraseTarget(LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            clearInventory(player);
            player.kill();
            player.connection.disconnect(Component.translatable("message.gocstrike.erased"));
        } else {
            // 普通生物直接移除，跳过伤害、死亡动画、掉落和经验。
            target.discard();
        }
    }

    private static void clearInventory(ServerPlayer player) {
        player.getInventory().clearContent();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.getCraftSlots().clearContent();
        // 关闭合成界面会退回材料，先清空背包腾出空间，等退回后再清理一次。
        // 容器存储和末影箱不属于玩家背包。
        player.closeContainer();
        player.getInventory().clearContent();
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
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
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
