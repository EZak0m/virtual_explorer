package com.example.virtualexplorer.block;

import com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class VirtualMappingTableBlock extends Block implements EntityBlock {

    public VirtualMappingTableBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).requiresCorrectToolForDrops());
    }

    @Override
    protected ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (player.isSecondaryUseActive() && !stack.isEmpty()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VirtualMappingTableBlockEntity tableEntity) {
                if (!level.isClientSide) {
                    if (tableEntity.addUpgrade(stack.getItem(), stack.getCount())) {
                        stack.setCount(0);
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§aUpgrade installed!"), true);
                    } else {
                        // Check if it's an upgrade at all before showing conflict
                        if (stack.getItem() instanceof com.example.virtualexplorer.item.UpgradeItem || stack.getItem() instanceof com.example.virtualexplorer.item.CompressedUpgradeItem) {
                            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.virtualexplorer.status.9"), true);
                        }
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        } else if (!stack.isEmpty() && stack.is(Items.FILLED_MAP)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VirtualMappingTableBlockEntity tableEntity) {
                if (!level.isClientSide) {
                    if (tableEntity.teleportToMap(stack, player)) {
                        return ItemInteractionResult.sidedSuccess(level.isClientSide);
                    }
                } else {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof VirtualMappingTableBlockEntity tableEntity) {
                player.openMenu(tableEntity, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VirtualMappingTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null; // クライアント側ではTick処理を行わない
        }
        return (lvl, pos, blockState, t) -> {
            if (t instanceof VirtualMappingTableBlockEntity be) {
                be.tick();
            }
        };
    }
}
