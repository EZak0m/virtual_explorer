package com.example.virtualexplorer.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LootGenerator {

    private static final TagKey<Item> ORE_TAG = TagKey.create(BuiltInRegistries.ITEM.key(), ResourceLocation.fromNamespaceAndPath("c", "ores"));

    public static List<ItemStack> generateNatureLoot(ServerLevel level, String moduleId, RandomSource random, boolean fortune, boolean silkTouch, @org.jetbrains.annotations.Nullable String targetBiomeId) {
        List<ItemStack> loots = new ArrayList<>();
        
        Biome biome = null;
        if (targetBiomeId != null) {
            var biomeRegistry = level.registryAccess().registryOrThrow(Registries.BIOME);
            biome = biomeRegistry.get(ResourceLocation.parse(targetBiomeId));
        }

        // バイオームの動的解析を試みる
        if (biome != null) {
            List<Block> possibleBlocks = analyzeBiomeFeatures(biome, moduleId);
            if (!possibleBlocks.isEmpty()) {
                Block target = possibleBlocks.get(random.nextInt(possibleBlocks.size()));
                loots.addAll(getDropsForBlock(level, target, fortune, silkTouch));
                return loots;
            }
        }

        // 解析に失敗した場合やターゲットがない場合はフォールバック
        switch (moduleId) {
            case "surface":
                loots.add(new ItemStack(random.nextBoolean() ? Items.OAK_LOG : Items.OAK_SAPLING, 1 + (fortune ? random.nextInt(2) : 0)));
                break;
            case "underground":
                Item ore = getRandomItemWithTag(ORE_TAG, random, item -> {
                    String path = BuiltInRegistries.ITEM.getKey(item).getPath();
                    return !path.contains("nether") && !path.contains("end") && !path.contains("quartz");
                });
                if (ore != null) {
                    loots.addAll(getDropsForBlock(level, Block.byItem(ore), fortune, silkTouch));
                } else {
                    loots.add(new ItemStack(Items.COAL, 1));
                }
                break;
            case "nether":
                if (random.nextBoolean()) {
                    loots.addAll(getDropsForBlock(level, Blocks.NETHER_QUARTZ_ORE, fortune, silkTouch));
                } else {
                    loots.addAll(getDropsForBlock(level, Blocks.GLOWSTONE, fortune, silkTouch));
                }
                break;
            case "end":
                loots.add(new ItemStack(Items.CHORUS_FRUIT, 1 + (fortune ? random.nextInt(2) : 0)));
                break;
        }
        return loots;
    }

    private static List<Block> analyzeBiomeFeatures(Biome biome, String moduleId) {
        List<Block> blocks = new ArrayList<>();
        
        // 地下探索の場合は鉱石、地上探索の場合は木をターゲットにする
        for (var featureList : biome.getGenerationSettings().features()) {
            for (var featureHolder : featureList) {
                PlacedFeature placedFeature = featureHolder.value();
                ConfiguredFeature<?, ?> configuredFeature = placedFeature.feature().value();
                
                if (moduleId.equals("underground") && configuredFeature.feature() == Feature.ORE) {
                    if (configuredFeature.config() instanceof OreConfiguration oreConfig) {
                        for (var target : oreConfig.targetStates) {
                            blocks.add(target.state.getBlock());
                        }
                    }
                } else if (moduleId.equals("surface") && (configuredFeature.feature() == Feature.TREE)) {
                    if (configuredFeature.config() instanceof TreeConfiguration treeConfig) {
                        blocks.add(treeConfig.trunkProvider.getState(null, null).getBlock());
                    }
                }
            }
        }
        return blocks.stream().distinct().collect(Collectors.toList());
    }

    private static List<ItemStack> getDropsForBlock(ServerLevel level, Block block, boolean fortune, boolean silkTouch) {
        if (silkTouch && block.asItem() != Items.AIR) {
            return List.of(new ItemStack(block));
        }
        
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        if (fortune) {
            level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .get(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)
                .ifPresent(holder -> tool.enchant(holder, 3));
        }
        
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.EMPTY);
        
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        ResourceLocation lootTableId = blockId.withPrefix("blocks/");
        
        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, lootTableId));
        return table.getRandomItems(params);
    }

    public static List<ItemStack> generateStructureLoot(ServerLevel level, String structureId, RandomSource random) {
        ResourceLocation lootTableId = getLootTableForStructure(structureId);
        if (lootTableId == null) return List.of(new ItemStack(Items.GOLD_INGOT));

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.EMPTY);

        LootTable table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, lootTableId));
        return table.getRandomItems(params);
    }

    private static ResourceLocation getLootTableForStructure(String structureId) {
        if (structureId.contains("village")) return ResourceLocation.fromNamespaceAndPath("minecraft", "chests/village/village_plains_house");
        if (structureId.contains("mineshaft")) return ResourceLocation.fromNamespaceAndPath("minecraft", "chests/abandoned_mineshaft");
        if (structureId.contains("fortress")) return ResourceLocation.fromNamespaceAndPath("minecraft", "chests/nether_bridge");
        if (structureId.contains("end_city")) return ResourceLocation.fromNamespaceAndPath("minecraft", "chests/end_city_treasure");
        if (structureId.contains("pyramid")) return ResourceLocation.fromNamespaceAndPath("minecraft", "chests/desert_pyramid");
        if (structureId.contains("bastion")) return ResourceLocation.fromNamespaceAndPath("minecraft", "chests/bastion_treasure");
        if (structureId.contains("stronghold")) return ResourceLocation.fromNamespaceAndPath("minecraft", "chests/stronghold_corridor");
        return null;
    }

    public static List<ItemStack> generateDemolitionLoot(String moduleId, RandomSource random, boolean isHovering, boolean silkTouch) {
        List<ItemStack> loots = new ArrayList<>();
        if (isHovering) {
            loots.add(new ItemStack(random.nextBoolean() ? Items.STONE_BRICKS : Items.COBBLESTONE, 2));
        } else {
            switch (moduleId) {
                case "surface": loots.add(new ItemStack(Items.OAK_PLANKS, 4)); break;
                case "underground": loots.add(new ItemStack(silkTouch ? Items.DEEPSLATE : Items.COBBLED_DEEPSLATE, 8)); break;
                case "nether": loots.add(new ItemStack(Items.NETHERRACK, 16)); break;
                case "end": loots.add(new ItemStack(Items.END_STONE, 16)); break;
            }
        }
        return loots;
    }

    public static FluidStack generateFluidLoot(String moduleId, RandomSource random) {
        switch (moduleId) {
            case "surface": return new FluidStack(Fluids.WATER, 1000);
            case "underground": return new FluidStack(Fluids.LAVA, 250);
            case "nether": return new FluidStack(Fluids.LAVA, 1000);
            default: return FluidStack.EMPTY;
        }
    }

    private static Item getRandomItemWithTag(TagKey<Item> tag, RandomSource random, java.util.function.Predicate<Item> filter) {
        var entries = BuiltInRegistries.ITEM.getTag(tag);
        if (entries.isPresent()) {
            var list = entries.get().stream().map(net.minecraft.core.Holder::value).filter(filter).toList();
            if (!list.isEmpty()) {
                return list.get(random.nextInt(list.size()));
            }
        }
        return null;
    }
}
