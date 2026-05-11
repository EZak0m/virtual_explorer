package com.example.virtualexplorer.init;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CapabilityInit {

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                BlockEntityInit.VIRTUAL_MAPPING_TABLE_BE.get(),
                (be, side) -> be.getHopperHandler()
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                BlockEntityInit.VIRTUAL_MAPPING_TABLE_BE.get(),
                (be, side) -> be.getEnergyStorage()
        );

        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityInit.VIRTUAL_MAPPING_TABLE_BE.get(),
                (be, side) -> be.getFluidTank()
        );
    }
}
