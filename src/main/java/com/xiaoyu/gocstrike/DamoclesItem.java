package com.xiaoyu.gocstrike;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class DamoclesItem extends Item {
    public DamoclesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            var type = GOCStrike.DAMOCLES.get();
            var previous = serverLevel.getEntities(type, sword -> sword.isOwnedBy(player));
            if (player.isShiftKeyDown()) {
                previous.forEach(DamoclesEntity::cancel);
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
            var sword = new DamoclesEntity(type, serverLevel);
            sword.begin(player);
            if (!serverLevel.addFreshEntity(sword)) {
                return InteractionResultHolder.fail(stack);
            }
            // 新的剑成功生成后再收起旧剑，避免生成被拦截时丢失正在展示的效果。
            previous.forEach(DamoclesEntity::cancel);
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gocstrike.damocles_sword.use").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.gocstrike.damocles_sword.lock").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gocstrike.damocles_sword.impact").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.gocstrike.damocles_sword.cancel").withStyle(ChatFormatting.GRAY));
    }
}
