package com.xiaoyu.gocstrike.client;

import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

final class DamoclesItemRenderer implements IItemRenderer {
    private final DamoclesMesh mesh;
    private final LegacyBatch batch = new LegacyBatch(GL11.GL_TRIANGLES, false, 6000);

    DamoclesItemRenderer(DamoclesMesh mesh) {
        this.mesh = mesh;
    }

    @Override
    public boolean handleRenderType(ItemStack stack, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack stack, ItemRendererHelper helper) {
        return type == ItemRenderType.ENTITY && helper == ItemRendererHelper.ENTITY_BOBBING;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack stack, Object... data) {
        Matrix4f pose = new Matrix4f();
        switch (type) {
            case INVENTORY:
                pose.translation(8, 8, 0).scale(16, -16, 16).rotateZ(-38 * LegacyMath.DEG_TO_RAD)
                        .scale(1.12F).translate(-0.5F, -0.5F, -0.5F);
                break;
            case ENTITY:
                pose.translation(0, 3 / 16F, 0).scale(0.64F).translate(-0.5F, -0.5F, -0.5F);
                break;
            case EQUIPPED_FIRST_PERSON:
                pose.translation(1.3F / 16F, 5.9F / 16F, 1.2F / 16F)
                        .rotateY(-60 * LegacyMath.DEG_TO_RAD).rotateZ(-15 * LegacyMath.DEG_TO_RAD).scale(0.72F);
                break;
            default:
                pose.translation(0.7F / 16F, 5.5F / 16F, 2.6F / 16F)
                        .rotateY(-75 * LegacyMath.DEG_TO_RAD).rotateZ(-28 * LegacyMath.DEG_TO_RAD).scale(0.85F);
                break;
        }
        batch.clear();
        mesh.draw(batch, pose, 1, 1, 0);
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
