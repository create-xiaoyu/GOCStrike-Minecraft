package com.xiaoyu.gocstrike;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

final class RitualItem extends Item {
    private final Map<EntityPlayer, Integer> lastUse = new WeakHashMap<>();
    private final boolean sword;

    RitualItem(boolean sword) {
        this.sword = sword;
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
        setUnlocalizedName("gocstrike." + (sword ? "damocles_sword" : "thaumaturgic_strike"));
        setTextureName("gocstrike:thaumaturgic_strike");
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            return stack;
        }
        Integer previousUse = lastUse.get(player);
        if (!player.isSneaking() && previousUse != null && player.ticksExisted - previousUse < 10) {
            return stack;
        }
        if (sword ? useSword(world, player) : useStrike(world, player)) {
            lastUse.put(player, player.ticksExisted);
        }
        return stack;
    }

    private boolean useSword(World world, EntityPlayer player) {
        ArrayList<DamoclesEntity> previous = new ArrayList<>();
        for (Object entity : world.loadedEntityList) {
            if (entity instanceof DamoclesEntity && !((Entity) entity).isDead && ((DamoclesEntity) entity).ownedBy(player)) {
                previous.add((DamoclesEntity) entity);
            }
        }
        if (player.isSneaking()) {
            previous.forEach(DamoclesEntity::cancel);
            return false;
        }
        DamoclesEntity launched = new DamoclesEntity(world);
        launched.begin(player);
        if (!world.spawnEntityInWorld(launched)) {
            return false;
        }
        // 新剑成功生成后再取消旧演出，避免生成失败时误取消。
        previous.forEach(DamoclesEntity::cancel);
        return true;
    }

    private boolean useStrike(World world, EntityPlayer player) {
        StrikeEntity active = null;
        for (Object entity : world.loadedEntityList) {
            if (entity instanceof StrikeEntity && !((Entity) entity).isDead) {
                active = (StrikeEntity) entity;
                break;
            }
        }
        if (player.isSneaking()) {
            if (active != null && active.ownedBy(player)) {
                active.setDead();
                message(player, "cancelled");
            } else {
                message(player, "nothing_to_cancel");
            }
        } else if (active != null) {
            message(player, "already_active");
        } else {
            StrikeEntity created = new StrikeEntity(world);
            created.begin(player);
            if (!world.spawnEntityInWorld(created)) {
                return false;
            }
            message(player, "summoned");
        }
        return true;
    }

    private static void message(EntityPlayer player, String key) {
        player.addChatComponentMessage(new ChatComponentTranslation("message.gocstrike." + key));
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        if (sword) {
            tooltip(lines, "damocles_sword.use", EnumChatFormatting.AQUA);
            tooltip(lines, "damocles_sword.lock", EnumChatFormatting.GRAY);
            tooltip(lines, "damocles_sword.impact", EnumChatFormatting.RED);
            tooltip(lines, "damocles_sword.cancel", EnumChatFormatting.GRAY);
        } else {
            tooltip(lines, "use", EnumChatFormatting.AQUA);
            tooltip(lines, "cancel", EnumChatFormatting.GRAY);
            tooltip(lines, "impact", EnumChatFormatting.RED);
        }
    }

    private static void tooltip(List lines, String key, EnumChatFormatting color) {
        lines.add(color + StatCollector.translateToLocal("tooltip.gocstrike." + key));
    }
}
