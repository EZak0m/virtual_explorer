package com.example.virtualexplorer.logic;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class LootTableAnalyzerTest {

    @Test
    void testRollRandomItem() {
        List<LootTableAnalyzer.DropCandidate> candidates = Arrays.asList(
                new LootTableAnalyzer.DropCandidate("minecraft:stone", 50),
                new LootTableAnalyzer.DropCandidate("minecraft:diamond", 10)
        );

        // シードを固定して、結果が決定的にテストできるようにする
        Random random = new Random(12345L);
        
        String result = LootTableAnalyzer.rollRandomItem(candidates, random);
        assertNotNull(result);
        assertTrue(result.equals("minecraft:stone") || result.equals("minecraft:diamond"));
    }

    @Test
    void testFilterByNamespace() {
        List<LootTableAnalyzer.DropCandidate> candidates = Arrays.asList(
                new LootTableAnalyzer.DropCandidate("minecraft:stone", 50),
                new LootTableAnalyzer.DropCandidate("mekanism:osmium_ore", 10),
                new LootTableAnalyzer.DropCandidate("virtualexplorer:module", 5)
        );

        List<LootTableAnalyzer.DropCandidate> filtered = LootTableAnalyzer.filterByNamespace(candidates, "mekanism");
        assertEquals(1, filtered.size());
        assertEquals("mekanism:osmium_ore", filtered.get(0).itemId);
    }
}
