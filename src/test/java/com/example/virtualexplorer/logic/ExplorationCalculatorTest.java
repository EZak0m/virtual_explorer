package com.example.virtualexplorer.logic;

import com.example.virtualexplorer.api.IExplorerModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExplorationCalculatorTest {

    // テスト用のダミーモジュール
    private static class DummyModule implements IExplorerModule {
        @Override
        public int getBaseEnergyCost() { return 100; }
        @Override
        public int getBaseProcessingTime() { return 200; }
        @Override
        public String getModuleId() { return "dummy"; }
    }

    @Test
    void testCalculateEnergyCost_NoUpgrades() {
        DummyModule module = new DummyModule();
        // アップグレード0個の場合、コストは変化しないはず
        int cost = ExplorationCalculator.calculateEnergyCost(module, 0, 1.5);
        assertEquals(100, cost);
    }

    @Test
    void testCalculateEnergyCost_WithUpgrades() {
        DummyModule module = new DummyModule();
        // 100 * 1.5 * 1.5 = 225
        int cost = ExplorationCalculator.calculateEnergyCost(module, 2, 1.5);
        assertEquals(225, cost);
    }

    @Test
    void testCalculateProcessingTime_WithUpgrades() {
        DummyModule module = new DummyModule();
        // 200 * 0.8 * 0.8 = 128
        int time = ExplorationCalculator.calculateProcessingTime(module, 2, 0.8);
        assertEquals(128, time);
    }
}
