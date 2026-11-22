package com.xiaoyu.gocstrike.client;

import java.io.IOException;

import com.xiaoyu.gocstrike.CommonProxy;
import com.xiaoyu.gocstrike.GOCStrike;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import paulscode.sound.SoundSystemException;

public final class ClientProxy extends CommonProxy {
    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        try {
            DamoclesMesh sword = new DamoclesMesh();
            GOCStrike.DAMOCLES_ITEM.setTileEntityItemStackRenderer(new DamoclesItemRenderer(sword));
            MinecraftForge.EVENT_BUS.register(new LegacyBloom(sword));
            MinecraftForge.EVENT_BUS.register(new LegacySubtitles());
            LegacyAudio audio = new LegacyAudio();
            MinecraftForge.EVENT_BUS.register(audio);
        } catch (IOException | SoundSystemException exception) {
            throw new IllegalStateException("Cannot initialize GOC Strike client effects", exception);
        }
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (Item item : new Item[]{GOCStrike.STRIKE_ITEM, GOCStrike.DAMOCLES_ITEM}) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }
}
