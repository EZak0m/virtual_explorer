package com.example.virtualexplorer.event;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.util.StructureTrackerWSD;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = VirtualExplorer.MODID)
public class WorldProtectionEvents {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            StructureTrackerWSD data = StructureTrackerWSD.get(serverLevel);
            if (data.isAnyExplored(event.getChunk().getPos())) {
                // 探索済みチャンク内のチェストをクリア
                for (BlockPos pos : event.getChunk().getBlockEntitiesPos()) {
                    BlockEntity be = serverLevel.getBlockEntity(pos);
                    if (be instanceof RandomizableContainerBlockEntity container) {
                        // バニラの戦利品テーブルをクリアするか、中身を空にする
                        container.setLootTable(null, 0);
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            container.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                        }
                    }
                }
            }
        }
    }
}
