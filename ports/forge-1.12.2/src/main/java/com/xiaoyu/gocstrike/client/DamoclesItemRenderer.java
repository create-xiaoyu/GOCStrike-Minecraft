package com.xiaoyu.gocstrike.client;

import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

final class DamoclesItemRenderer extends TileEntityItemStackRenderer {
    private final DamoclesMesh mesh;
    private final LegacyBatch batch = new LegacyBatch(GL11.GL_TRIANGLES, false, 6000);

    DamoclesItemRenderer(DamoclesMesh mesh) {
        this.mesh = mesh;
    }

    @Override
    public void renderByItem(ItemStack stack) {
        // 物品模型的 display 变换由 RenderItem 应用，网格保持原有的物品坐标。
        batch.clear();
        mesh.draw(batch, new Matrix4f(), 1, 1, 0);
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        try {
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            batch.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }
}
