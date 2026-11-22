package com.xiaoyu.gocstrike;

import java.util.Objects;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class GOCStrike {
    public static final String MODID = "gocstrike";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final RegistryObject<Item> THAUMATURGIC_STRIKE = ITEMS.register("thaumaturgic_strike",
            () -> new StrikeItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final RegistryObject<Item> DAMOCLES_SWORD = ITEMS.register("damocles_sword",
            () -> new DamoclesItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<EntityType<StrikeEntity>> STRIKE = ENTITIES.register("thaumaturgic_strike",
            () -> EntityType.Builder.<StrikeEntity>of(StrikeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(32).updateInterval(20)
                    .noSave().noSummon().fireImmune().build(MODID + ":thaumaturgic_strike"));
    public static final RegistryObject<EntityType<DamoclesEntity>> DAMOCLES = ENTITIES.register("damocles_sword",
            () -> EntityType.Builder.<DamoclesEntity>of(DamoclesEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(16).updateInterval(1)
                    .noSave().noSummon().fireImmune().build(MODID + ":damocles_sword"));

    private GOCStrike() {
    }

    static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
    }


    public static ResourceLocation id(String path) {
        return Objects.requireNonNull(ResourceLocation.tryBuild(MODID, path), path);
    }

}
