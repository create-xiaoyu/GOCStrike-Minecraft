package com.xiaoyu.gocstrike;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

public class StrikeItem extends Item {
    public StrikeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel serverLevel) {
            // 每个维度只允许一场演出，避免多个法阵同时绘制造成负担。
            StrikeEntity active = null;
            for (Entity entity : serverLevel.getAllEntities()) {
                if (entity instanceof StrikeEntity strike && strike.isAlive()) {
                    active = strike;
                    break;
                }
            }
            if (player.isShiftKeyDown()) {
                if (active != null && active.isOwnedBy(player)) {
                    active.discard();
                    ((ServerPlayer) player).sendOverlayMessage(Component.translatable("message.gocstrike.cancelled"));
                } else {
                    ((ServerPlayer) player).sendOverlayMessage(Component.translatable("message.gocstrike.nothing_to_cancel"));
                }
            } else if (active != null) {
                ((ServerPlayer) player).sendOverlayMessage(Component.translatable("message.gocstrike.already_active"));
            } else {
                StrikeEntity strike = new StrikeEntity(GOCStrike.STRIKE.get(), level);
                strike.begin(player);
                if (!serverLevel.addFreshEntity(strike)) {
                    return InteractionResult.FAIL;
                }
                ((ServerPlayer) player).sendOverlayMessage(Component.translatable("message.gocstrike.summoned"));
            }
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 10);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.gocstrike.use").withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.gocstrike.cancel").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.gocstrike.impact").withStyle(ChatFormatting.RED));
    }
}
