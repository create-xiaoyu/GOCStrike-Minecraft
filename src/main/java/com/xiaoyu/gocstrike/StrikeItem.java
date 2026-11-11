package com.xiaoyu.gocstrike;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class StrikeItem extends Item {
    public StrikeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            // One active show per dimension bounds rendering and prevents accidental stacking.
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
                    player.displayClientMessage(Component.translatable("message.gocstrike.cancelled"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.gocstrike.nothing_to_cancel"), true);
                }
            } else if (active != null) {
                player.displayClientMessage(Component.translatable("message.gocstrike.already_active"), true);
            } else {
                StrikeEntity strike = new StrikeEntity(GOCStrike.STRIKE.get(), level);
                strike.begin(player);
                if (!serverLevel.addFreshEntity(strike)) {
                    return InteractionResultHolder.fail(stack);
                }
                player.displayClientMessage(Component.translatable("message.gocstrike.summoned"), true);
            }
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gocstrike.use").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.gocstrike.cancel").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gocstrike.impact").withStyle(ChatFormatting.RED));
    }
}
