package com.example.virtualexplorer.init;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.item.ExplorerModuleItem;
import com.example.virtualexplorer.item.UpgradeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ItemInit {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(VirtualExplorer.MODID);

    public static final DeferredItem<BlockItem> VIRTUAL_MAPPING_TABLE_ITEM = ITEMS.registerSimpleBlockItem("virtual_mapping_table", BlockInit.VIRTUAL_MAPPING_TABLE);

    // --- 探索モジュール (場所・ディメンションを指定) ---
    public static final DeferredItem<ExplorerModuleItem> MODULE_SURFACE = ITEMS.register("module_surface", () -> new ExplorerModuleItem("surface", 100, 200));
    public static final DeferredItem<ExplorerModuleItem> MODULE_UNDERGROUND = ITEMS.register("module_underground", () -> new ExplorerModuleItem("underground", 120, 250));
    public static final DeferredItem<ExplorerModuleItem> MODULE_NETHER = ITEMS.register("module_nether", () -> new ExplorerModuleItem("nether", 250, 400));
    public static final DeferredItem<ExplorerModuleItem> MODULE_END = ITEMS.register("module_end", () -> new ExplorerModuleItem("end", 500, 600));

    // --- アップグレード (機能・動作を拡張) ---
    public static final DeferredItem<UpgradeItem> SPEED_UPGRADE = ITEMS.register("speed_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<UpgradeItem> RECYCLE_UPGRADE = ITEMS.register("recycle_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<UpgradeItem> RADAR_UPGRADE = ITEMS.register("radar_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(64)));
    public static final DeferredItem<UpgradeItem> PARALLEL_UPGRADE = ITEMS.register("parallel_upgrade", () -> new UpgradeItem(new Item.Properties().stacksTo(64)));
    
    // 機能拡張系アップグレード
    public static final DeferredItem<UpgradeItem> UPGRADE_FLUID = ITEMS.register("upgrade_fluid", () -> new UpgradeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> UPGRADE_ARCHEOLOGY = ITEMS.register("upgrade_archeology", () -> new UpgradeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> UPGRADE_DEMOLITION_MASTER = ITEMS.register("upgrade_demolition_master", () -> new UpgradeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> UPGRADE_STRUCTURE_INTEREST = ITEMS.register("upgrade_structure_interest", () -> new UpgradeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> UPGRADE_INFINITE = ITEMS.register("upgrade_infinite", () -> new UpgradeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> UPGRADE_FORTUNE = ITEMS.register("upgrade_fortune", () -> new UpgradeItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<UpgradeItem> UPGRADE_SILK_TOUCH = ITEMS.register("upgrade_silk_touch", () -> new UpgradeItem(new Item.Properties().stacksTo(1)));

    // ターゲットピン (NBT処理が必要な特殊アップグレード)
    public static final DeferredItem<com.example.virtualexplorer.item.TargetPinItem> TARGET_PIN = ITEMS.register("target_pin", () -> new com.example.virtualexplorer.item.TargetPinItem(new Item.Properties().stacksTo(1)));
}
