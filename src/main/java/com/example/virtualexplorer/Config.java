package com.example.virtualexplorer;

import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 1. Machine & Energy
    public static final ModConfigSpec.IntValue MAX_FE_CAPACITY;
    public static final ModConfigSpec.IntValue BASE_FE_PER_TICK;
    public static final ModConfigSpec.IntValue MAX_FE_TRANSFER_RATE;
    public static final ModConfigSpec.IntValue STANDBY_FE_DRAIN;

    // 2. Cost & Speed
    public static final ModConfigSpec.IntValue BASE_TICKS_PER_OPERATION;
    public static final ModConfigSpec.BooleanValue CONSUME_MAP_PER_CHUNK;
    public static final ModConfigSpec.DoubleValue MAP_CONSUMPTION_CHANCE;
    public static final ModConfigSpec.ConfigValue<String> ALTERNATIVE_MAP_ITEM;

    // 3. Upgrades
    public static final ModConfigSpec.IntValue MAX_UPGRADES_PER_SLOT;
    public static final ModConfigSpec.DoubleValue SPEED_UPGRADE_TIME_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SPEED_UPGRADE_FE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue RECYCLE_UPGRADE_CHANCE;
    public static final ModConfigSpec.IntValue PARALLEL_UPGRADE_AMOUNT;

    // 4. Biome Exploration
    public static final ModConfigSpec.BooleanValue ENABLE_MODDED_BIOMES;
    public static final ModConfigSpec.IntValue BIOME_EXTRACTION_ROLLS;
    public static final ModConfigSpec.BooleanValue ALLOW_FLUID_EXTRACTION;
    public static final ModConfigSpec.IntValue FORTUNE_MODULE_MULTIPLIER;

    // 5. Structure & Loot
    public static final ModConfigSpec.BooleanValue ENABLE_MODDED_STRUCTURES;
    public static final ModConfigSpec.DoubleValue WALL_BLOCK_EXTRACT_CHANCE;
    public static final ModConfigSpec.IntValue CHEST_LOOT_MULTIPLIER;
    public static final ModConfigSpec.IntValue INFINITE_MODULE_COOLDOWN;
    public static final ModConfigSpec.BooleanValue ENABLE_ARCHEOLOGY_LOOT;

    // 6. Server Performance & Protection
    public static final ModConfigSpec.IntValue ASYNC_LOCATE_TIMEOUT;
    public static final ModConfigSpec.IntValue MAX_OPERATIONS_PER_TICK;
    public static final ModConfigSpec.BooleanValue ENABLE_WSD_SAVING;
    public static final ModConfigSpec.BooleanValue DELETE_EXPLORED_CHESTS;
    public static final ModConfigSpec.BooleanValue REPLACE_CHEST_WITH_AIR;

    // 7. Blacklists & Limits
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DIMENSION_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STRUCTURE_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_BLACKLIST;

    // 8. Visual
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BIOME_COLORS;

    static {
        BUILDER.push("Machine & Energy");
        MAX_FE_CAPACITY = BUILDER.comment("機械の内部FEタンク最大容量")
                .defineInRange("max_fe_capacity", 2100000000, 1000, Integer.MAX_VALUE);
        BASE_FE_PER_TICK = BUILDER.comment("基本稼働時の1Tickあたりの消費FE")
                .defineInRange("base_fe_per_tick", 100, 0, Integer.MAX_VALUE);
        MAX_FE_TRANSFER_RATE = BUILDER.comment("1Tickあたりに外部から搬入できる最大FE量")
                .defineInRange("max_fe_transfer_rate", 100000, 0, Integer.MAX_VALUE);
        STANDBY_FE_DRAIN = BUILDER.comment("インベントリ満杯時など、待機状態の時に消費する維持FE")
                .defineInRange("standby_fe_drain", 0, 0, Integer.MAX_VALUE);
        BUILDER.pop();

        BUILDER.push("Cost & Speed");
        BASE_TICKS_PER_OPERATION = BUILDER.comment("1回の探索（1チャンク移動や1チェスト開封）にかかる基本Tick数")
                .defineInRange("base_ticks_per_operation", 100, 1, Integer.MAX_VALUE);
        CONSUME_MAP_PER_CHUNK = BUILDER.comment("1チャンク移動ごとに白紙の地図を消費するか")
                .define("consume_map_per_chunk", true);
        MAP_CONSUMPTION_CHANCE = BUILDER.comment("地図を消費する基本確率 (1.0 = 100%)")
                .defineInRange("map_consumption_chance", 1.0, 0.0, 1.0);
        ALTERNATIVE_MAP_ITEM = BUILDER.comment("地図の代わりに消費できるアイテムID（序盤緩和用など）")
                .define("alternative_map_item", "");
        BUILDER.pop();

        BUILDER.push("Upgrades");
        MAX_UPGRADES_PER_SLOT = BUILDER.comment("1スロットにつき反映されるアップグレードの最大数")
                .defineInRange("max_upgrades_per_slot", 8, 1, 64);
        SPEED_UPGRADE_TIME_MULTIPLIER = BUILDER.comment("スピードUP1個あたりの処理時間短縮率 (デフォルト: 0.8 = 20%短縮)")
                .defineInRange("speed_upgrade_time_multiplier", 0.8, 0.01, 1.0);
        SPEED_UPGRADE_FE_MULTIPLIER = BUILDER.comment("スピードUP1個あたりのFE消費増加率 (デフォルト: 1.5 = 50%増加)")
                .defineInRange("speed_upgrade_fe_multiplier", 1.5, 1.0, 10.0);
        RECYCLE_UPGRADE_CHANCE = BUILDER.comment("コンパスUP1個あたりの地図消費無効化確率")
                .defineInRange("recycle_upgrade_chance", 0.1, 0.0, 1.0);
        PARALLEL_UPGRADE_AMOUNT = BUILDER.comment("並列処理UP1個あたりの同時処理数増加量")
                .defineInRange("parallel_upgrade_amount", 1, 1, 64);
        BUILDER.pop();

        BUILDER.push("Biome Exploration");
        ENABLE_MODDED_BIOMES = BUILDER.comment("他modの追加バイオームを探索対象に含めるか")
                .define("enable_modded_biomes", true);
        BIOME_EXTRACTION_ROLLS = BUILDER.comment("1チャンク到達時にアイテムを抽選する基本回数")
                .defineInRange("biome_extraction_rolls", 3, 1, 100);
        ALLOW_FLUID_EXTRACTION = BUILDER.comment("流体サンプラー稼働時、液体を取得許可するか")
                .define("allow_fluid_extraction", true);
        FORTUNE_MODULE_MULTIPLIER = BUILDER.comment("幸運モジュール適用時のドロップ増加ボーナス上限")
                .defineInRange("fortune_module_multiplier", 3, 0, 10);
        BUILDER.pop();

        BUILDER.push("Structure & Loot");
        ENABLE_MODDED_STRUCTURES = BUILDER.comment("他modの追加構造物を探索対象に含めるか")
                .define("enable_modded_structures", true);
        WALL_BLOCK_EXTRACT_CHANCE = BUILDER.comment("構造物探索時、チェストとは別に外壁ブロックを取得できる確率")
                .defineInRange("wall_block_extract_chance", 0.05, 0.0, 1.0);
        CHEST_LOOT_MULTIPLIER = BUILDER.comment("チェスト開封1回あたりのルートテーブル試行回数")
                .defineInRange("chest_loot_multiplier", 1, 1, 100);
        INFINITE_MODULE_COOLDOWN = BUILDER.comment("無限化モジュール使用時、同じ構造物を再探索するまでのクールダウンTick")
                .defineInRange("infinite_module_cooldown", 1200, 0, Integer.MAX_VALUE);
        ENABLE_ARCHEOLOGY_LOOT = BUILDER.comment("遺跡系構造物で「怪しげな砂/砂利」のルートテーブルを回すことを許可するか")
                .define("enable_archeology_loot", true);
        BUILDER.pop();

        BUILDER.push("Server Performance & Protection");
        ASYNC_LOCATE_TIMEOUT = BUILDER.comment("構造物検索(locate)の最大許容時間(秒)")
                .defineInRange("async_locate_timeout", 5, 1, 60);
        MAX_OPERATIONS_PER_TICK = BUILDER.comment("1Tickに処理できる最大アクション数")
                .defineInRange("max_operations_per_tick", 1, 1, 100);
        ENABLE_WSD_SAVING = BUILDER.comment("探索済み構造物をセーブデータに記録するか")
                .define("enable_wsd_saving", true);
        DELETE_EXPLORED_CHESTS = BUILDER.comment("チャンクが生成された際、探索済みチェストを削除するか")
                .define("delete_explored_chests", true);
        REPLACE_CHEST_WITH_AIR = BUILDER.comment("削除する際、空箱として残すか空気にするか (false=空箱)")
                .define("replace_chest_with_air", false);
        BUILDER.pop();

        BUILDER.push("Blacklists & Limits");
        DIMENSION_BLACKLIST = BUILDER.comment("探索機の設置・稼働を禁止するディメンションIDのリスト")
                .defineListAllowEmpty("dimension_blacklist", List.of(), obj -> obj instanceof String);
        STRUCTURE_BLACKLIST = BUILDER.comment("探索対象から除外する構造物IDのリスト")
                .defineListAllowEmpty("structure_blacklist", List.of(), obj -> obj instanceof String);
        ITEM_BLACKLIST = BUILDER.comment("探索で絶対に取得・排出させないアイテムIDのリスト")
                .defineListAllowEmpty("item_blacklist", List.of(), obj -> obj instanceof String);
        BUILDER.pop();

        BUILDER.push("Visual");
        BIOME_COLORS = BUILDER.comment("バイオームIDと色のマッピング (format: 'biome_id:#RRGGBB')")
                .defineListAllowEmpty("biome_colors", List.of(
                    "minecraft:plains:#7fb238",
                    "minecraft:desert:#fa9418",
                    "minecraft:forest:#056621",
                    "minecraft:taiga:#0b6659",
                    "minecraft:swamp:#07f9b2",
                    "minecraft:ocean:#0000ff",
                    "minecraft:nether_wastes:#8b0000",
                    "minecraft:the_end:#ffffd1"
                ), obj -> obj instanceof String);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
