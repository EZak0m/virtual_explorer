package com.example.virtualexplorer.item;

import net.minecraft.world.item.Item;

public class CompressedUpgradeItem extends Item {
    private final int multiplier;
    private final java.util.function.Supplier<Item> baseUpgrade;

    public CompressedUpgradeItem(java.util.function.Supplier<Item> baseUpgrade, int multiplier, Properties properties) {
        super(properties);
        this.baseUpgrade = baseUpgrade;
        this.multiplier = multiplier;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public Item getBaseUpgrade() {
        return baseUpgrade.get();
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
