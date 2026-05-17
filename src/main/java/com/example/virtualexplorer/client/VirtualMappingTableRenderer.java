package com.example.virtualexplorer.client;

import com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

/**
 * 仮想探索機（VirtualMappingTable）が稼働中であるとき、
 * ブロックの上空にふわふわと浮遊する半透明で発光する3Dホログラフィック・グリッドと、
 * その中心で逆回転するエネルギーコアをレンダリングするカスタム BlockEntityRenderer です。
 */
public class VirtualMappingTableRenderer implements BlockEntityRenderer<VirtualMappingTableBlockEntity> {

    public VirtualMappingTableRenderer(BlockEntityRendererProvider.Context context) {
        // コンストラクタ（追加のモデルパーツやテクスチャのロードが必要な場合はここで初期化）
    }

    @Override
    public void render(VirtualMappingTableBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        // マシンが稼働中（isActive == true）のときのみホログラムを描画
        if (!blockEntity.isActive() || blockEntity.getLevel() == null) {
            return;
        }

        poseStack.pushPose();
        
        // 浮遊アニメーションの計算 (サイン波)
        float gameTime = blockEntity.getLevel().getGameTime() + partialTick;
        float time = gameTime * 0.05f;
        float hoverY = 1.35f + (float) Math.sin(time * 2.0f) * 0.04f;
        
        // マシン上部の空中へ移動
        poseStack.translate(0.5D, hoverY, 0.5D);
        
        // ホログラム全体をゆっくりと時計回りに回転
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 8.0f));

        // 半透明で描画するため translucent レンダリングタイプを使用
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();

        // 1. 5x5 のグリッド線を描画
        float gridSize = 0.8f; // グリッドの全体サイズ
        int gridCount = 5;
        float halfSize = gridSize / 2.0f;
        float step = gridSize / gridCount;

        // グリッドの色 (シアン/水色、脈動パルス効果)
        float pulse = 0.7f + (float) Math.sin(time * 3.0f) * 0.3f;
        int r = (int) (60 * pulse);
        int g = (int) (210 * pulse);
        int b = 255;
        int a = 140; // 半透明アルファ

        // X軸およびZ軸に沿ってグリッドのスキャンラインを描画
        for (int i = 0; i <= gridCount; i++) {
            float coord = -halfSize + i * step;
            // X方向のライン
            drawHologramLine(vertexConsumer, matrix, -halfSize, coord, halfSize, coord, r, g, b, a);
            // Z方向のライン
            drawHologramLine(vertexConsumer, matrix, coord, -halfSize, coord, halfSize, r, g, b, a);
        }

        // 1.5. 各セルに対応する半透明のカラー平面（スキャン進行状況）を描画
        float margin = 0.006f; // セル同士の隙間
        float halfCellSize = (step / 2.0f) - margin;
        
        for (int row = 0; row < gridCount; row++) {
            for (int col = 0; col < gridCount; col++) {
                int index = row * 5 + col;
                int color = blockEntity.getGridColor(index);
                
                int cr, cg, cb, ca;
                if (index == 12) {
                    // 中心セル（現在位置）は白く強調発光
                    cr = 255;
                    cg = 255;
                    cb = 255;
                    ca = 140;
                } else if (color == 0) {
                    // 未スキャン領域は非常に薄いグレー
                    cr = 50;
                    cg = 50;
                    cb = 50;
                    ca = 25;
                } else {
                    // バイオームカラーを展開
                    cr = (color >> 16) & 0xFF;
                    cg = (color >> 8) & 0xFF;
                    cb = color & 0xFF;
                    ca = 90;
                }
                
                // 全体的な呼吸明滅効果を適用
                ca = (int) (ca * (0.8f + (float) Math.sin(time * 2.5f) * 0.2f));
                
                // セルの中心座標を算出
                float cellX = -halfSize + col * step + step / 2.0f;
                float cellZ = -halfSize + row * step + step / 2.0f;
                
                // 平面を描画 (少し高さを変えて重なりを防ぐ)
                drawPlane(vertexConsumer, matrix, cellX - halfCellSize, cellZ - halfCellSize, cellX + halfCellSize, cellZ + halfCellSize, 0.001f, cr, cg, cb, ca);
            }
        }

        // 2. 現在探索中の中心コア（逆回転・傾きのあるキューブ）を描画
        poseStack.pushPose();
        poseStack.scale(0.12f, 0.12f, 0.12f);
        
        // コアは全体とは逆方向に高速回転させ、斜めに傾ける
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 40.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(25.0f + (float) Math.sin(time * 1.5f) * 10.0f));
        
        Matrix4f coreMatrix = poseStack.last().pose();
        
        // コアの色（より明るく、発光度を強調）
        int cr = 80;
        int cg = 230;
        int cb = 255;
        int ca = 180;

        drawCube(vertexConsumer, coreMatrix, cr, cg, cb, ca);
        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * ホログラムの細い線を、薄い水平面の板として描画します。
     */
    private void drawHologramLine(VertexConsumer consumer, Matrix4f matrix, float x1, float z1, float x2, float z2, int r, int g, int b, int a) {
        float thickness = 0.008f; // 線の太さ
        if (x1 == x2) { // Z方向の線
            drawPlane(consumer, matrix, x1 - thickness, z1, x1 + thickness, z2, 0.0f, r, g, b, a);
        } else { // X方向の線
            drawPlane(consumer, matrix, x1, z1 - thickness, x2, z1 + thickness, 0.0f, r, g, b, a);
        }
    }

    /**
     * 指定された座標に半透明の平面を描画します。自発光（LightmapMax）を強制します。
     * 頂点フォーマット（DefaultVertexFormat.BLOCK）に準拠するため、UV0 と Normal も正しく指定します。
     */
    private void drawPlane(VertexConsumer consumer, Matrix4f matrix, float x1, float z1, float x2, float z2, float y, int r, int g, int b, int a) {
        int fullLight = 15728880; // 明るさの最大値 (暗闇でも光って見える)
        consumer.addVertex(matrix, x1, y, z1).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x1, y, z2).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x2, y, z2).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x2, y, z1).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 1.0f, 0.0f);
    }

    /**
     * コアとなる半透明のキューブ（6面）を描画します。自発光します。
     * 各面に対応した適切な法線（Normal）とダミーのテクスチャUV（UV0）を指定してクラッシュを解消します。
     */
    private void drawCube(VertexConsumer consumer, Matrix4f matrix, int r, int g, int b, int a) {
        float size = 0.5f;
        int fullLight = 15728880;

        // 上面 (Top) - 法線: (0, 1, 0)
        consumer.addVertex(matrix, -size, size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, -size, size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, size, size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, size, size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 1.0f, 0.0f);
        
        // 底面 (Bottom) - 法線: (0, -1, 0)
        consumer.addVertex(matrix, -size, -size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, -1.0f, 0.0f);
        consumer.addVertex(matrix, size, -size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, -1.0f, 0.0f);
        consumer.addVertex(matrix, size, -size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, -1.0f, 0.0f);
        consumer.addVertex(matrix, -size, -size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, -1.0f, 0.0f);
        
        // 前面 (Front) - 法線: (0, 0, 1)
        consumer.addVertex(matrix, -size, -size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, size, -size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, size, size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, -size, size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 0.0f, 1.0f);
        
        // 後面 (Back) - 法線: (0, 0, -1)
        consumer.addVertex(matrix, -size, -size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 0.0f, -1.0f);
        consumer.addVertex(matrix, -size, size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 0.0f, -1.0f);
        consumer.addVertex(matrix, size, size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 0.0f, -1.0f);
        consumer.addVertex(matrix, size, -size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(0.0f, 0.0f, -1.0f);
        
        // 左面 (Left) - 法線: (-1, 0, 0)
        consumer.addVertex(matrix, -size, -size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(-1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, -size, -size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(-1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, -size, size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(-1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, -size, size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(-1.0f, 0.0f, 0.0f);
        
        // 右面 (Right) - 法線: (1, 0, 0)
        consumer.addVertex(matrix, size, -size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, size, size, -size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, size, size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, size, -size, size).setColor(r, g, b, a).setUv(0.0f, 0.0f).setLight(fullLight).setNormal(1.0f, 0.0f, 0.0f);
    }
}
