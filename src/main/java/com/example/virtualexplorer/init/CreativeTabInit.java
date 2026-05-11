package com.example.virtualexplorer.init;

import com.example.virtualexplorer.VirtualExplorer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CreativeTabInit {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VirtualExplorer.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXPLORER_TAB = CREATIVE_MODE_TABS.register("explorer_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.virtualexplorer.explorer_tab"))
            .icon(() -> ItemInit.VIRTUAL_MAPPING_TABLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ItemInit.VIRTUAL_MAPPING_TABLE_ITEM.get());
                // モジュール
                output.accept(ItemInit.MODULE_SURFACE.get());
                output.accept(ItemInit.MODULE_UNDERGROUND.get());
                output.accept(ItemInit.MODULE_NETHER.get());
                output.accept(ItemInit.MODULE_END.get());
                // アップグレード
                output.accept(ItemInit.SPEED_UPGRADE.get());
                output.accept(ItemInit.RECYCLE_UPGRADE.get());
                output.accept(ItemInit.RADAR_UPGRADE.get());
                output.accept(ItemInit.PARALLEL_UPGRADE.get());
                output.accept(ItemInit.UPGRADE_FLUID.get());
                output.accept(ItemInit.UPGRADE_ARCHEOLOGY.get());
                output.accept(ItemInit.UPGRADE_DEMOLITION_MASTER.get());
                output.accept(ItemInit.UPGRADE_STRUCTURE_INTEREST.get());
                output.accept(ItemInit.UPGRADE_INFINITE.get());
                output.accept(ItemInit.UPGRADE_FORTUNE.get());
                output.accept(ItemInit.UPGRADE_SILK_TOUCH.get());
                output.accept(ItemInit.TARGET_PIN.get());
            }).build());
}
