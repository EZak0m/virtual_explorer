package com.example.virtualexplorer.item;

import com.example.virtualexplorer.api.IExplorerModule;
import net.minecraft.world.item.Item;

public class ExplorerModuleItem extends Item implements IExplorerModule {
    private final String moduleId;
    private final int baseEnergyCost;
    private final int baseProcessingTime;

    public ExplorerModuleItem(String moduleId, int baseEnergyCost, int baseProcessingTime) {
        super(new Item.Properties().stacksTo(1)); // モジュールはスタック不可とする
        this.moduleId = moduleId;
        this.baseEnergyCost = baseEnergyCost;
        this.baseProcessingTime = baseProcessingTime;
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Override
    public int getBaseEnergyCost() {
        return baseEnergyCost;
    }

    @Override
    public int getBaseProcessingTime() {
        return baseProcessingTime;
    }
    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            tooltip.add(net.minecraft.network.chat.Component.translatable(this.getDescriptionId() + ".description").withStyle(net.minecraft.ChatFormatting.GRAY));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.virtualexplorer.energy_cost", baseEnergyCost).withStyle(net.minecraft.ChatFormatting.BLUE));
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.virtualexplorer.process_time", baseProcessingTime).withStyle(net.minecraft.ChatFormatting.GOLD));
        } else {
            tooltip.add(net.minecraft.network.chat.Component.translatable("tooltip.virtualexplorer.hold_shift").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
