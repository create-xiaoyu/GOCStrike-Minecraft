package com.xiaoyu.gocstrike;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(GOCStrike.MODID)
public final class ForgeEntrypoint {
    public ForgeEntrypoint() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        GOCStrike.register(bus);
        bus.addListener(ForgeEntrypoint::addCreativeItems);
    }

    private static void addCreativeItems(CreativeModeTabEvent.BuildContents event) {
        if (event.getTab() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(GOCStrike.THAUMATURGIC_STRIKE.get());
            event.accept(GOCStrike.DAMOCLES_SWORD.get());
        }
    }
}
