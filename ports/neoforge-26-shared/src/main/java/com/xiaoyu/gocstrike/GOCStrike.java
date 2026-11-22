package com.xiaoyu.gocstrike;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(GOCStrike.MODID)
public class GOCStrike {
    public static final String MODID = "gocstrike";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredItem<Item> THAUMATURGIC_STRIKE = ITEMS.registerItem("thaumaturgic_strike",
            StrikeItem::new, properties -> properties.stacksTo(1).fireResistant());
    public static final DeferredItem<Item> DAMOCLES_SWORD = ITEMS.registerItem("damocles_sword",
            DamoclesItem::new, properties -> properties.stacksTo(1));
    public static final DeferredHolder<EntityType<?>, EntityType<StrikeEntity>> STRIKE = ENTITIES.register("thaumaturgic_strike",
            () -> EntityType.Builder.<StrikeEntity>of(StrikeEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F).clientTrackingRange(32).updateInterval(20)
                    .noSave().noSummon().fireImmune().build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "thaumaturgic_strike"))));
    public static final DeferredHolder<EntityType<?>, EntityType<DamoclesEntity>> DAMOCLES = ENTITIES.register("damocles_sword",
            () -> EntityType.Builder.<DamoclesEntity>of(DamoclesEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(16).updateInterval(1)
                    .noSave().noSummon().fireImmune().build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "damocles_sword"))));

    private static final DeferredRegister<TicketType> TICKETS = DeferredRegister.create(Registries.TICKET_TYPE, MODID);
    static final DeferredHolder<TicketType, TicketType> SHOW_TICKET = TICKETS.register("damocles_show",
            () -> new TicketType(20 * 90, TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION
                    | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));

    public GOCStrike(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        TICKETS.register(modEventBus);
        modEventBus.addListener(this::addCreativeItems);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(THAUMATURGIC_STRIKE);
            event.accept(DAMOCLES_SWORD);
        }
    }
}
