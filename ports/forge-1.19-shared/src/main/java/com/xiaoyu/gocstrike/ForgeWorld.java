package com.xiaoyu.gocstrike;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ForgeWorld {
    private ForgeWorld() {
    }

    public static Level level(Entity entity) {
        return entity.getLevel();
    }
}
