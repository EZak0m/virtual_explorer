package com.example.virtualexplorer.init;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.block.VirtualMappingTableBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockInit {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(VirtualExplorer.MODID);

    public static final DeferredBlock<VirtualMappingTableBlock> VIRTUAL_MAPPING_TABLE = BLOCKS.register("virtual_mapping_table", VirtualMappingTableBlock::new);
}
