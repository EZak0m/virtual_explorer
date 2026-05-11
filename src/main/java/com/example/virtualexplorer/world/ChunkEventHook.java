package com.example.virtualexplorer.world;

import com.example.virtualexplorer.VirtualExplorer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = VirtualExplorer.MODID)
public class ChunkEventHook {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel && event.getChunk() instanceof LevelChunk levelChunk) {
            VirtualExplorerSavedData data = VirtualExplorerSavedData.get(serverLevel);
            
            // もしこのチャンクが既に「仮想探索」されていれば
            if (data.isExplored(levelChunk.getPos())) {
                // チャンク内のすべてのBlockEntityを走査し、チェストなどのルートテーブルをクリアする
                for (BlockEntity be : levelChunk.getBlockEntities().values()) {
                    if (be instanceof RandomizableContainerBlockEntity container) {
                        // ルートテーブル設定を解除（すでに探索済みとする）
                        container.setLootTable(null);
                        container.clearContent();
                    }
                }
            }
        }
    }
}
