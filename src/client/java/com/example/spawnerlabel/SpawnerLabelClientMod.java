package com.example.spawnerlabel;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class SpawnerLabelClientMod implements ClientModInitializer {

    private static final double MAX_DISTANCE_SQ = 48.0 * 48.0;
    private static final int FULL_BRIGHT_LIGHT = 15728880;

    @Override
    public void onInitializeClient() {
        LevelRenderEvents.END_MAIN.register(this::onEndMainRender);
    }

    private void onEndMainRender(LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        LocalPlayer player = client.player;
        if (level == null || player == null) {
            return;
        }

        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        PoseStack poseStack = context.poseStack();
        MultiBufferSource bufferSource = context.bufferSource();
        Font font = client.font;

        ChunkPos playerChunk = player.chunkPosition();
        int chunkRadius = Math.min(client.options.renderDistance().get(), 6);

        for (int cx = playerChunk.x() - chunkRadius; cx <= playerChunk.x() + chunkRadius; cx++) {
            for (int cz = playerChunk.z() - chunkRadius; cz <= playerChunk.z() + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof SpawnerBlockEntity spawnerBe) {
                        renderSpawnerText(
                                client, level, spawnerBe,
                                poseStack, bufferSource, font,
                                camera, cameraPos
                        );
                    }
                }
            }
        }

        if (bufferSource instanceof MultiBufferSource.BufferSource immediate) {
            immediate.endBatch();
        }
    }

    private void renderSpawnerText(
            Minecraft client,
            ClientLevel level,
            SpawnerBlockEntity spawnerBe,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Font font,
            Camera camera,
            Vec3 cameraPos
    ) {
        BlockPos pos = spawnerBe.getBlockPos();
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 1.4;
        double centerZ = pos.getZ() + 0.5;

        if (cameraPos.distanceToSqr(centerX, centerY, centerZ) > MAX_DISTANCE_SQ) {
            return;
        }

        BaseSpawner spawner = spawnerBe.getSpawner();
        Entity displayEntity = spawner.getOrCreateDisplayEntity(level, pos);

        Component label = displayEntity != null
                ? displayEntity.getName()
                : Component.literal("Пустой спавнер");

        poseStack.pushPose();
        poseStack.translate(centerX - cameraPos.x, centerY - cameraPos.y, centerZ - cameraPos.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(0.025f, -0.025f, 0.025f);

        float xOffset = -font.width(label) / 2.0f;
        int bgColor = (int) (client.options.getBackgroundOpacity(0.25f) * 255.0f) << 24;
        Matrix4f matrix = poseStack.last().pose();

        font.drawInBatch(
                label,
                xOffset,
                0.0f,
                0xFFFFFFFF,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                bgColor,
                FULL_BRIGHT_LIGHT
        );

        poseStack.popPose();
    }
}
