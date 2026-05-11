package com.example.virtualexplorer.init;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public class BlockEntityInit {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, VirtualExplorer.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VirtualMappingTableBlockEntity>> VIRTUAL_MAPPING_TABLE_BE = BLOCK_ENTITIES.register("virtual_mapping_table", 
            () -> BlockEntityType.Builder.of(VirtualMappingTableBlockEntity::new, BlockInit.VIRTUAL_MAPPING_TABLE.get()).build(null));
}
