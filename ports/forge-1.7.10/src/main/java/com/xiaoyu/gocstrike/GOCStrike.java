package com.xiaoyu.gocstrike;

import java.util.List;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.item.Item;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = GOCStrike.MODID, name = "GOC Strike", useMetadata = true, acceptedMinecraftVersions = "[1.7.10]")
public final class GOCStrike {
    public static final String MODID = "gocstrike";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "com.xiaoyu.gocstrike.client.ClientProxy", serverSide = "com.xiaoyu.gocstrike.CommonProxy")
    public static CommonProxy proxy;
    @Mod.Instance(MODID)
    public static GOCStrike instance;
    public static Item strikeItem;
    public static Item damoclesItem;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        strikeItem = new RitualItem(false);
        damoclesItem = new RitualItem(true);
        GameRegistry.registerItem(strikeItem, "thaumaturgic_strike");
        GameRegistry.registerItem(damoclesItem, "damocles_sword");
        EntityRegistry.registerModEntity(StrikeEntity.class, "thaumaturgic_strike", 0, this, 512, 20, false);
        EntityRegistry.registerModEntity(DamoclesEntity.class, "damocles_sword", 1, this, 256, 1, true);

        // 演出不写入存档，重载的旧票据也不重新强制加载。
        ForgeChunkManager.setForcedChunkLoadingCallback(this, (List<ForgeChunkManager.Ticket> tickets, net.minecraft.world.World world) -> {
            for (ForgeChunkManager.Ticket ticket : tickets) {
                ForgeChunkManager.releaseTicket(ticket);
            }
        });
        MinecraftForge.EVENT_BUS.register(this);
        proxy.init();
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (!event.world.isRemote) {
            for (Object entity : event.world.loadedEntityList) {
                if (entity instanceof DamoclesEntity) {
                    ((DamoclesEntity) entity).setDead();
                }
            }
        }
    }
}
