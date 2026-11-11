package com.xiaoyu.gocstrike;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
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
    // Original choreography: WhiteoutAt (116.032) + 0.75 seconds.
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
        if (level() instanceof ServerLevel serverLevel) {
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
        // Death, logout and mod callbacks can change entity collections. Snapshot only
        // the loaded living entities in this dimension before applying any removal.
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
                if (target instanceof ServerPlayer player) {
                    clearInventory(player);
                    player.kill();
                    player.connection.disconnect(Component.translatable("message.gocstrike.erased"));
                    players++;
                } else {
                    // No damage, death events, loot, experience or death animation.
                    target.discard();
                    removed++;
                }
            } catch (RuntimeException exception) {
                GOCStrike.LOGGER.error("Final strike failed for entity {}", target.getUUID(), exception);
            }
        }
        GOCStrike.LOGGER.info("Final strike in {}: removed {} living entities, processed {} players",
                level.dimension().location(), removed, players);
    }

    private static void clearInventory(ServerPlayer player) {
        player.getInventory().clearContent();
        player.containerMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.setCarried(ItemStack.EMPTY);
        player.inventoryMenu.getCraftSlots().clearContent();
        // Closing a crafting menu returns its ingredients to the inventory. Make room
        // first, close it while the player is alive, then clear those returned stacks.
        // Container storage and ender chests are not part of the player's inventory.
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
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
