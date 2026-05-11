package com.example.virtualexplorer.logic;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * LootTableの解析や、ドロップ候補からの抽選などを行う純粋なロジッククラス。
 * 実際のMinecraftのLootTableから「重み付きアイテム候補リスト」を生成したのち、
 * このクラスのメソッドに渡して処理を行うことで、テストを容易にします。
 */
public class LootTableAnalyzer {

    /**
     * 重み付きアイテムエントリを表現するクラス
     */
    public static class DropCandidate {
        public final String itemId;
        public final int weight;

        public DropCandidate(String itemId, int weight) {
            this.itemId = itemId;
            this.weight = weight;
        }
    }

    /**
     * 重みに基づいてランダムにアイテムを1つ抽選します。
     * @param candidates ドロップ候補のリスト
     * @param random 乱数生成器
     * @return 抽選されたアイテムID、候補がない場合は null
     */
    public static String rollRandomItem(List<DropCandidate> candidates, Random random) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        int totalWeight = candidates.stream().mapToInt(c -> c.weight).sum();
        if (totalWeight <= 0) {
            return null;
        }

        int value = random.nextInt(totalWeight);
        int current = 0;
        for (DropCandidate candidate : candidates) {
            current += candidate.weight;
            if (value < current) {
                return candidate.itemId;
            }
        }
        return null;
    }

    /**
     * 特定のネームスペース（例: "minecraft", "virtualexplorer"）を持つアイテムだけを抽出します。
     * @param candidates 元の候補リスト
     * @param namespace フィルタリングするネームスペース
     * @return フィルタリングされたリスト
     */
    public static List<DropCandidate> filterByNamespace(List<DropCandidate> candidates, String namespace) {
        if (candidates == null || namespace == null) return List.of();
        
        return candidates.stream()
                .filter(c -> c.itemId.startsWith(namespace + ":"))
                .collect(Collectors.toList());
    }
}
