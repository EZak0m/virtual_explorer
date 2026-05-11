package com.example.virtualexplorer.init;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.inventory.VirtualMappingTableMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MenuInit {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, VirtualExplorer.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<VirtualMappingTableMenu>> VIRTUAL_MAPPING_TABLE_MENU = MENUS.register("virtual_mapping_table",
            () -> IMenuTypeExtension.create(VirtualMappingTableMenu::new));
}
