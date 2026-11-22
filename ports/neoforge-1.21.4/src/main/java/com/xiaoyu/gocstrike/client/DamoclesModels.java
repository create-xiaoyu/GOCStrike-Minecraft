package com.xiaoyu.gocstrike.client;

import com.xiaoyu.gocstrike.DamoclesEntity;
import com.xiaoyu.gocstrike.GOCStrike;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = GOCStrike.MODID, value = Dist.CLIENT)
public final class DamoclesModels {
    private static final ResourceLocation SWORD = ResourceLocation.fromNamespaceAndPath(GOCStrike.MODID, "item/damocles_sword");
    private static BakedModel swordModel;

    private DamoclesModels() {
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        // 物品渲染器只暴露渲染状态，另注册同一个 OBJ 模型供逐段铸剑和碎片计算读取。
        event.register(SWORD);
    }

    @SubscribeEvent
    public static void load(ModelEvent.BakingCompleted event) {
        swordModel = event.getBakingResult().standaloneModels().get(SWORD);
        if (swordModel == null) {
            GOCStrike.LOGGER.error("Missing baked Damocles sword model {}", SWORD);
        }
    }

    static BakedModel getModel(ItemRenderer renderer, ItemStack sword, DamoclesEntity entity) {
        return swordModel;
    }
}
