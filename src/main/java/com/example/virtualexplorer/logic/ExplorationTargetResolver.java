package com.example.virtualexplorer.logic;

import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/**
 * 探索モジュールに基づいて、対象となるLootTableを決定する純粋関数クラス。
 */
public class ExplorationTargetResolver {

    /**
     * モジュールIDと乱数に基づいて、探索先のLootTable IDを決定します。
     * @param moduleId モジュールの識別子
     * @param random 乱数生成器
     * @param hasRadar レーダーアップグレードの有無
     * @return LootTableのID
     */
    public static ResourceLocation resolveLootTable(String moduleId, net.minecraft.util.RandomSource random, boolean hasRadar) {
        if ("underground".equals(moduleId)) {
            // 地下探索モジュールの場合
            double chance = hasRadar ? 0.4 : 0.2; // レーダーがあればダンジョン発見率UP
            if (random.nextDouble() < chance) {
                return ResourceLocation.parse("minecraft:chests/simple_dungeon");
            } else {
                return ResourceLocation.parse("minecraft:chests/abandoned_mineshaft");
            }
        } else if ("nether".equals(moduleId)) {
            // ネザー探索モジュールの場合
            double chance = hasRadar ? 0.5 : 0.3; // レーダーがあれば砦の遺跡発見率UP
            if (random.nextDouble() < chance) {
                return ResourceLocation.parse("minecraft:chests/bastion_treasure");
            } else {
                return ResourceLocation.parse("minecraft:chests/nether_bridge");
            }
        } else if ("flora".equals(moduleId)) {
            // 生体探査モジュール: 動物や植物のルート
            return ResourceLocation.parse("minecraft:gameplay/fishing/fish"); // 暫定で釣りのルート
        } else if ("archeology".equals(moduleId)) {
            // 発掘調査の刷毛: 遺跡
            return ResourceLocation.parse("minecraft:archaeology/desert_pyramid");
        } else if ("demolition".equals(moduleId)) {
            // 解体業者モジュール: 建造物のブロック
            // 本来はブロックドロップですが、仮にイグルーのチェスト（建材多め）や適当な建材ルート
            return ResourceLocation.parse("minecraft:chests/igloo_chest");
        }
        
        // デフォルト
        return ResourceLocation.parse("minecraft:chests/village/village_plains_house");
    }
}
