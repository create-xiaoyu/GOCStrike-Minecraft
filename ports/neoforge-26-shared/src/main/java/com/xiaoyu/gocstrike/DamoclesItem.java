package com.xiaoyu.gocstrike;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class DamoclesItem extends Item {
    public DamoclesItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel) {
            var type = GOCStrike.DAMOCLES.get();
            var previous = serverLevel.getEntities(type, sword -> sword.isOwnedBy(player));
            if (player.isShiftKeyDown()) {
                previous.forEach(DamoclesEntity::cancel);
                return InteractionResult.SUCCESS_SERVER;
            }
            var sword = new DamoclesEntity(type, serverLevel);
            sword.begin(player);
            if (!serverLevel.addFreshEntity(sword)) {
                return InteractionResult.FAIL;
            }
            // 新的剑成功生成后再收起旧剑，避免生成被拦截时丢失正在展示的效果。
            previous.forEach(DamoclesEntity::cancel);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 10);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.gocstrike.damocles_sword.use").withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.gocstrike.damocles_sword.lock").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.gocstrike.damocles_sword.impact").withStyle(ChatFormatting.RED));
        tooltip.accept(Component.translatable("tooltip.gocstrike.damocles_sword.cancel").withStyle(ChatFormatting.GRAY));
    }
}
