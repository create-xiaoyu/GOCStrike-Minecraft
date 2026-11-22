package com.xiaoyu.gocstrike;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.event.RegistryEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = GOCStrike.MODID, name = "GOC Strike", useMetadata = true, acceptedMinecraftVersions = "[1.12.2]")
public final class GOCStrike {
    public static final String MODID = "gocstrike";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "com.xiaoyu.gocstrike.client.ClientProxy", serverSide = "com.xiaoyu.gocstrike.CommonProxy")
    public static CommonProxy proxy;
    @Mod.Instance(MODID)
    public static GOCStrike instance;
    public static final Item STRIKE_ITEM = createItem(false, "thaumaturgic_strike");
    public static final Item DAMOCLES_ITEM = createItem(true, "damocles_sword");

    public static final SoundEvent EMPLACEMENT_SCORE = createSound("emplacement_score");
    public static final SoundEvent DAMOCLES_SCORE = createSound("damocles_score");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 演出不写入存档，重载的旧票据也不重新强制加载。
        ForgeChunkManager.setForcedChunkLoadingCallback(this, (tickets, world) -> {
            for (ForgeChunkManager.Ticket ticket : tickets) {
                ForgeChunkManager.releaseTicket(ticket);
            }
        });
        MinecraftForge.EVENT_BUS.register(this);
        proxy.init();
    }

    private static Item createItem(boolean sword, String name) {
        return new RitualItem(sword).setRegistryName(MODID, name).setTranslationKey(MODID + "." + name);
    }

    private static SoundEvent createSound(String name) {
        ResourceLocation id = new ResourceLocation(MODID, name);
        return new SoundEvent(id).setRegistryName(id);
    }

    @SubscribeEvent
    public void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(EMPLACEMENT_SCORE, DAMOCLES_SCORE);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(STRIKE_ITEM, DAMOCLES_ITEM);
    }

    @SubscribeEvent
    public void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        event.getRegistry().register(EntityEntryBuilder.create().entity(StrikeEntity.class)
                .id(new ResourceLocation(MODID, "thaumaturgic_strike"), 0).name("thaumaturgic_strike")
                .tracker(512, 20, false).build());
        event.getRegistry().register(EntityEntryBuilder.create().entity(DamoclesEntity.class)
                .id(new ResourceLocation(MODID, "damocles_sword"), 1).name("damocles_sword")
                .tracker(256, 1, true).build());
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) {
            for (Object entity : event.getWorld().loadedEntityList) {
                if (entity instanceof DamoclesEntity) {
                    ((DamoclesEntity) entity).setDead();
                }
            }
        }
    }
}
