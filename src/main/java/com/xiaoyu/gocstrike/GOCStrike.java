package com.xiaoyu.gocstrike;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(GOCStrike.MODID)
public class GOCStrike {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "gocstrike";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);
    public static final DeferredItem<Item> THAUMATURGIC_STRIKE = ITEMS.register("thaumaturgic_strike",
            () -> new StrikeItem(new Item.Properties().stacksTo(1).fireResistant()));
    public static final DeferredHolder<EntityType<?>, EntityType<StrikeEntity>> STRIKE = ENTITIES.register("thaumaturgic_strike",
            () -> EntityType.Builder.<StrikeEntity>of(StrikeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(32).updateInterval(20)
                    .noSave().noSummon().fireImmune().build(MODID + ":thaumaturgic_strike"));

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public GOCStrike(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        modEventBus.addListener(this::addCreativeItems);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(THAUMATURGIC_STRIKE);
        }
    }
}
