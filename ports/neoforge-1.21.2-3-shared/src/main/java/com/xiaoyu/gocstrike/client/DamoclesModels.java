package com.xiaoyu.gocstrike.client;

import com.xiaoyu.gocstrike.DamoclesEntity;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;

final class DamoclesModels {
    private DamoclesModels() {
    }

    static BakedModel getModel(ItemRenderer renderer, ItemStack sword, DamoclesEntity entity) {
        return renderer.getModel(sword, entity.level(), null, entity.getId());
    }
}
