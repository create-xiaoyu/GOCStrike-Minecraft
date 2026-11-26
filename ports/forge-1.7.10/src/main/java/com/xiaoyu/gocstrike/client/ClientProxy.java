package com.xiaoyu.gocstrike.client;

import java.io.IOException;

import com.xiaoyu.gocstrike.CommonProxy;
import com.xiaoyu.gocstrike.GOCStrike;
import com.xiaoyu.gocstrike.TimedEntity;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import paulscode.sound.SoundSystemException;

public final class ClientProxy extends CommonProxy {
    @Override
    public void init() {
        // 同步实体只提供坐标和时间，实际画面由 LegacyBloom 在世界渲染阶段绘制。
        RenderingRegistry.registerEntityRenderingHandler(TimedEntity.class, new EffectEntityRenderer());
        try {
            DamoclesMesh sword = new DamoclesMesh();
            MinecraftForgeClient.registerItemRenderer(GOCStrike.damoclesItem, new DamoclesItemRenderer(sword));
            MinecraftForge.EVENT_BUS.register(new LegacyBloom(sword));
            MinecraftForge.EVENT_BUS.register(new LegacySubtitles());
            LegacyAudio audio = new LegacyAudio();
            MinecraftForge.EVENT_BUS.register(audio);
            FMLCommonHandler.instance().bus().register(audio);
        } catch (IOException | SoundSystemException exception) {
            throw new IllegalStateException("Cannot initialize GOC Strike client effects", exception);
        }
    }
}
