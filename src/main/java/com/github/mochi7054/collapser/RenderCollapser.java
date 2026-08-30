package com.github.mochi7054.collapser;

import com.github.mochi7054.fluid.SimpleMatterTank;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

import java.util.List;

public class RenderCollapser implements BlockEntityRenderer<CollapserBlockEntity> {

    public RenderCollapser(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CollapserBlockEntity tile, float partialTicks, PoseStack matrix, MultiBufferSource buffer, int light, int overlayLight) {
        List<SimpleMatterTank> tanks = tile.getMatterTanks();
        double totalStored = 0;
        double totalCapacity = 0;
        for (SimpleMatterTank tank : tanks) {
            totalStored += tank.getStored();
            totalCapacity += tank.getCapacity();
        }

        if (totalStored <= 0.001 || totalCapacity <= 0) {
            return;
        }

        float fillRatio = (float) Math.min(1.0, Math.max(0.0, totalStored / totalCapacity));
        float minHeight = 0.2f;
        float maxHeight = 0.8f;
        float currentHeight = minHeight + (maxHeight - minHeight) * fillRatio;

        VertexConsumer builder = buffer.getBuffer(RenderType.translucent());

        int color = 0xFF38FF70;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = 0.6f;

        matrix.pushPose();

        renderBox(matrix, builder, 0.2f, minHeight, 0.2f, 0.8f, currentHeight, 0.8f, r, g, b, a, light);

        matrix.popPose();
    }

    private static void renderBox(PoseStack matrix, VertexConsumer builder, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a, int light) {
        var mat = matrix.last().pose();

        // Top face
        builder.vertex(mat, minX, maxY, minZ).color(r, g, b, a).uv(0, 0).uv2(light).normal(0, 1, 0).endVertex();
        builder.vertex(mat, minX, maxY, maxZ).color(r, g, b, a).uv(0, 1).uv2(light).normal(0, 1, 0).endVertex();
        builder.vertex(mat, maxX, maxY, maxZ).color(r, g, b, a).uv(1, 1).uv2(light).normal(0, 1, 0).endVertex();
        builder.vertex(mat, maxX, maxY, minZ).color(r, g, b, a).uv(1, 0).uv2(light).normal(0, 1, 0).endVertex();

        // Bottom face
        builder.vertex(mat, minX, minY, minZ).color(r, g, b, a).uv(0, 0).uv2(light).normal(0, -1, 0).endVertex();
        builder.vertex(mat, maxX, minY, minZ).color(r, g, b, a).uv(1, 0).uv2(light).normal(0, -1, 0).endVertex();
        builder.vertex(mat, maxX, minY, maxZ).color(r, g, b, a).uv(1, 1).uv2(light).normal(0, -1, 0).endVertex();
        builder.vertex(mat, minX, minY, maxZ).color(r, g, b, a).uv(0, 1).uv2(light).normal(0, -1, 0).endVertex();

        // North
        builder.vertex(mat, minX, minY, minZ).color(r, g, b, a).uv(0, 0).uv2(light).normal(0, 0, -1).endVertex();
        builder.vertex(mat, minX, maxY, minZ).color(r, g, b, a).uv(0, 1).uv2(light).normal(0, 0, -1).endVertex();
        builder.vertex(mat, maxX, maxY, minZ).color(r, g, b, a).uv(1, 1).uv2(light).normal(0, 0, -1).endVertex();
        builder.vertex(mat, maxX, minY, minZ).color(r, g, b, a).uv(1, 0).uv2(light).normal(0, 0, -1).endVertex();

        // South
        builder.vertex(mat, minX, minY, maxZ).color(r, g, b, a).uv(0, 0).uv2(light).normal(0, 0, 1).endVertex();
        builder.vertex(mat, maxX, minY, maxZ).color(r, g, b, a).uv(1, 0).uv2(light).normal(0, 0, 1).endVertex();
        builder.vertex(mat, maxX, maxY, maxZ).color(r, g, b, a).uv(1, 1).uv2(light).normal(0, 0, 1).endVertex();
        builder.vertex(mat, minX, maxY, maxZ).color(r, g, b, a).uv(0, 1).uv2(light).normal(0, 0, 1).endVertex();

        // West
        builder.vertex(mat, minX, minY, minZ).color(r, g, b, a).uv(0, 0).uv2(light).normal(-1, 0, 0).endVertex();
        builder.vertex(mat, minX, minY, maxZ).color(r, g, b, a).uv(1, 0).uv2(light).normal(-1, 0, 0).endVertex();
        builder.vertex(mat, minX, maxY, maxZ).color(r, g, b, a).uv(1, 1).uv2(light).normal(-1, 0, 0).endVertex();
        builder.vertex(mat, minX, maxY, minZ).color(r, g, b, a).uv(0, 1).uv2(light).normal(-1, 0, 0).endVertex();

        // East
        builder.vertex(mat, maxX, minY, minZ).color(r, g, b, a).uv(0, 0).uv2(light).normal(1, 0, 0).endVertex();
        builder.vertex(mat, maxX, maxY, minZ).color(r, g, b, a).uv(0, 1).uv2(light).normal(1, 0, 0).endVertex();
        builder.vertex(mat, maxX, maxY, maxZ).color(r, g, b, a).uv(1, 1).uv2(light).normal(1, 0, 0).endVertex();
        builder.vertex(mat, maxX, minY, maxZ).color(r, g, b, a).uv(1, 0).uv2(light).normal(1, 0, 0).endVertex();
    }
}