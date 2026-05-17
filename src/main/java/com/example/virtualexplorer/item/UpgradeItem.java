package com.example.virtualexplorer.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UpgradeItem extends Item {
    public UpgradeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable(this.getDescriptionId() + ".description").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.virtualexplorer.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        net.minecraft.world.level.Level level = context.getLevel();
        net.minecraft.core.BlockPos pos = context.getClickedPos();
        net.minecraft.world.entity.player.Player player = context.getPlayer();
        net.minecraft.world.item.ItemStack stack = context.getItemInHand();
        
        if (player != null && player.isSecondaryUseActive() && !stack.isEmpty()) {
            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity tableEntity) {
                if (!level.isClientSide) {
                    if (tableEntity.addUpgrade(stack.getItem(), stack.getCount())) {
                        stack.setCount(0);
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§aUpgrade installed!"), true);
                    } else {
                        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.virtualexplorer.status.9"), true);
                    }
                }
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useOn(context);
    }
}
