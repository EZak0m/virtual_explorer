package com.example.virtualexplorer.logic;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class LootGenerationHelper {

    /**
     * 指定された LootTable からアイテムを生成します。
     * @param level サーバーレベル
     * @param position 実行位置
     * @param lootTableId LootTableのID (例: "minecraft:chests/simple_dungeon")
     * @return 生成されたアイテムのリスト
     */
    public static List<ItemStack> generateLoot(ServerLevel level, Vec3 position, ResourceLocation lootTableId) {
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, lootTableId));
        
        if (lootTable == LootTable.EMPTY) {
            return List.of();
        }

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, position)
                .create(LootContextParamSets.CHEST);

        return lootTable.getRandomItems(params);
    }
}
