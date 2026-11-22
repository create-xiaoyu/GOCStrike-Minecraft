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
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import net.minecraft.world.World;

final class RitualItem extends Item {
    private final Map<EntityPlayer, Integer> lastUse = new WeakHashMap<>();
    private final boolean sword;

    RitualItem(boolean sword) {
        this.sword = sword;
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.TOOLS);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.isSpectator()) {
            return new ActionResult<>(EnumActionResult.PASS, stack);
        }
        if (world.isRemote) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        Integer previousUse = lastUse.get(player);
        if (!player.isSneaking() && previousUse != null && player.ticksExisted - previousUse < 10) {
            return new ActionResult<>(EnumActionResult.SUCCESS, stack);
        }
        if (sword ? useSword(world, player) : useStrike(world, player)) {
            lastUse.put(player, player.ticksExisted);
        }
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
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
        if (!world.spawnEntity(launched)) {
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
            if (!world.spawnEntity(created)) {
                return false;
            }
            message(player, "summoned");
        }
        return true;
    }

    private static void message(EntityPlayer player, String key) {
        player.sendStatusMessage(new TextComponentTranslation("message.gocstrike." + key), true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> lines, ITooltipFlag flag) {
        if (sword) {
            tooltip(lines, "damocles_sword.use", TextFormatting.AQUA);
            tooltip(lines, "damocles_sword.lock", TextFormatting.GRAY);
            tooltip(lines, "damocles_sword.impact", TextFormatting.RED);
            tooltip(lines, "damocles_sword.cancel", TextFormatting.GRAY);
        } else {
            tooltip(lines, "use", TextFormatting.AQUA);
            tooltip(lines, "cancel", TextFormatting.GRAY);
            tooltip(lines, "impact", TextFormatting.RED);
        }
    }

    @SideOnly(Side.CLIENT)
    private static void tooltip(List<String> lines, String key, TextFormatting color) {
        lines.add(color + I18n.format("tooltip.gocstrike." + key));
    }
}
