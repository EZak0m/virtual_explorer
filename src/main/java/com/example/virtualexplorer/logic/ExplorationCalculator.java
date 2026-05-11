package com.example.virtualexplorer.logic;

import com.example.virtualexplorer.api.IExplorerModule;
import com.example.virtualexplorer.Config;

/**
 * 探索処理に関する各種パラメータを計算する純粋関数クラス。
 */
public class ExplorationCalculator {
    
    /**
     * Configとスピード・並列処理・レーダーアップグレードを考慮した実際のエネルギーコストを計算します。
     */
    public static int calculateEnergyCost(IExplorerModule module, int speedUpgrades, int parallelUpgrades, int radarUpgrades) {
        if (module == null) return 0;
        double baseCost = Config.BASE_FE_PER_TICK.get() * (module.getBaseEnergyCost() / 100.0);
        double energyMultiplier = Config.SPEED_UPGRADE_FE_MULTIPLIER.get();
        for (int i = 0; i < speedUpgrades; i++) {
            baseCost *= energyMultiplier;
        }
        // 並列処理コア1つにつきコストが倍加（またはベースを加算）
        baseCost *= (1 + parallelUpgrades);
        
        // レーダーアップグレード1つにつきエネルギー消費量が1.5倍
        for (int i = 0; i < radarUpgrades; i++) {
            baseCost *= 1.5;
        }

        return (int) Math.round(baseCost);
    }

    /**
     * Configとスピードアップグレードを考慮した実際の処理時間（Tick）を計算します。
     */
    public static int calculateProcessingTime(IExplorerModule module, int speedUpgrades) {
        if (module == null) return 0;
        double baseTime = Config.BASE_TICKS_PER_OPERATION.get() * (module.getBaseProcessingTime() / 100.0);
        double speedMultiplier = Config.SPEED_UPGRADE_TIME_MULTIPLIER.get();
        for (int i = 0; i < speedUpgrades; i++) {
            baseTime *= speedMultiplier;
        }
        return Math.max(1, (int) Math.round(baseTime));
    }

    /**
     * リサイクルアップグレード（製図家のコンパス）による地図消費確率を計算します。
     */
    public static double calculateMapConsumptionChance(int recycleUpgrades) {
        double baseChance = Config.MAP_CONSUMPTION_CHANCE.get();
        // 1アップグレードにつき消費確率を 20% (0.2) 減算する
        double finalChance = baseChance - (recycleUpgrades * 0.2);
        return Math.max(0.0, finalChance);
    }
}
