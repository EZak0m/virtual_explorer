package com.example.virtualexplorer.logic;

import com.example.virtualexplorer.api.IExplorerModule;
import com.example.virtualexplorer.Config;

/**
 * 探索処理に関する各種パラメータを計算する純粋関数クラス。
 */
public class ExplorationCalculator {
    
    public static int calculateEnergyCost(
        IExplorerModule module, 
        int speedUpgrades, 
        int parallelUpgrades, 
        int radarUpgrades, 
        int recycleUpgrades, 
        int fluidUpgrades, 
        int structureInterestUpgrades, 
        int silkTouchUpgrades, 
        int demolitionUpgrades, 
        int infiniteUpgrades
    ) {
        if (module == null) return 0;
        double baseCost = Config.BASE_FE_PER_TICK.get() * (module.getBaseEnergyCost() / 100.0);
        
        // (基礎値) × (アップグレード1枚当たりの上昇倍率)^(アップグレードの枚数)
        baseCost *= Math.pow(1.01, speedUpgrades);
        baseCost *= Math.pow(1.1, parallelUpgrades);
        baseCost *= Math.pow(1.01, Math.min(5, recycleUpgrades)); // 製図家のコンパスは上限5枚
        baseCost *= Math.pow(1.0, fluidUpgrades); // 流体サンプラーは1倍
        baseCost *= Math.pow(5.0, structureInterestUpgrades); // 構造物への興味は5倍
        baseCost *= Math.pow(2.0, silkTouchUpgrades); // 精密採取は2倍
        baseCost *= Math.pow(5.0, demolitionUpgrades); // 解体は5倍
        baseCost *= Math.pow(10.0, infiniteUpgrades); // 無限の可能性は10倍
        baseCost *= Math.pow(1.0, radarUpgrades); // レーダー/幸運は影響なし

        return (int) Math.round(baseCost);
    }

    public static int calculateProcessingTime(IExplorerModule module, int speedUpgrades) {
        if (module == null) return 0;
        double baseTime = Config.BASE_TICKS_PER_OPERATION.get() * (module.getBaseProcessingTime() / 100.0);
        
        // スピードアップグレード1つにつき処理時間が反比例で短縮される (y = a / (1 + b*x))
        // configの倍率(0.8など)を使ってスケールを決定する
        double speedTimeEffect = Math.max(0.01, (1.0 - Config.SPEED_UPGRADE_TIME_MULTIPLIER.get()) / 5.0); // 1個あたり約4%の効率向上
        
        double finalTime = baseTime / (1.0 + speedUpgrades * speedTimeEffect);
        
        return Math.max(1, (int) Math.round(finalTime));
    }

    public static double calculateMapConsumptionChance(int recycleUpgrades) {
        double baseChance = Config.MAP_CONSUMPTION_CHANCE.get();
        // リサイクルアップグレード1つにつき消費確率が漸近的に減少する (y = a / (1 + b*x))
        double recycleEffect = Config.RECYCLE_UPGRADE_CHANCE.get(); // 0.1 なら 1個で確率が 1/1.1 (約90%)に
        
        double finalChance = baseChance / (1.0 + recycleUpgrades * recycleEffect);
        return Math.max(0.0, finalChance);
    }
}
