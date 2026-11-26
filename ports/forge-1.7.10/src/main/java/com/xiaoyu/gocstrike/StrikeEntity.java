package com.xiaoyu.gocstrike;

import java.util.ArrayList;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

public final class StrikeEntity extends TimedEntity {
    public static final float DURATION = 118.698667F;
    public static final float WORLD_SCALE = 2.4F;
    private static final float FINAL_STRIKE_AT = 116.782F;
    private boolean applied;

    public StrikeEntity(World world) {
        super(world, 1);
    }

    void begin(EntityPlayer player) {
        owner = player.getUniqueID();
        setPosition(player.posX, player.boundingBox.minY, player.posZ);
        start();
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!worldObj.isRemote) {
            if (!hasStarted()) {
                setDead();
                return;
            }
            float time = elapsed(0);
            if (!applied && time >= FINAL_STRIKE_AT) {
                applied = true;
                eraseDimension();
            }
            if (time >= DURATION) {
                setDead();
            }
        }
    }

    private void eraseDimension() {
        // 死亡和断开连接会改变实体列表，先收集当时已加载且存活的生物。
        ArrayList<EntityLivingBase> targets = new ArrayList<>();
        for (Object entity : worldObj.loadedEntityList) {
            if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isEntityAlive()) {
                targets.add((EntityLivingBase) entity);
            }
        }
        for (EntityLivingBase target : targets) {
            if (!target.isEntityAlive()) {
                continue;
            }
            try {
                eraseTarget(target);
            } catch (RuntimeException exception) {
                GOCStrike.LOGGER.error("Thaumaturgic strike failed for {}", target.getUniqueID(), exception);
            }
        }
    }

    static void eraseTarget(EntityLivingBase target) {
        if (target instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) target;
            player.inventory.setItemStack(null);
            player.inventory.clearInventory(null, -1);
            // 关闭合成界面会退回材料。退回后再清背包，随后走原版玩家死亡流程。
            player.closeScreen();
            if (player.inventoryContainer instanceof ContainerPlayer) {
                InventoryCrafting crafting = ((ContainerPlayer) player.inventoryContainer).craftMatrix;
                for (int slot = 0; slot < crafting.getSizeInventory(); slot++) {
                    crafting.setInventorySlotContents(slot, null);
                }
            }
            player.inventory.clearInventory(null, -1);
            player.inventory.markDirty();
            player.setHealth(0);
            player.onDeath(DamageSource.outOfWorld);
            player.playerNetServerHandler.kickPlayerFromServer(StatCollector.translateToLocal("message.gocstrike.erased"));
        } else {
            target.setDead();
        }
    }
}
