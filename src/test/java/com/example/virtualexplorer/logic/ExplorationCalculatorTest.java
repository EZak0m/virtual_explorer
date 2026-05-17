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
    void testDummy() {
        // TODO: Update tests to mock Config values for new calculator logic
    }
}
