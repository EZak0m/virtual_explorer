package com.example.virtualexplorer.command;

import com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

public class VirtualExplorerCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ve_setpos")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                    .executes(context -> {
                        int cx = IntegerArgumentType.getInteger(context, "chunkX");
                        int cz = IntegerArgumentType.getInteger(context, "chunkZ");
                        CommandSourceStack source = context.getSource();
                        BlockPos pos = BlockPos.containing(source.getPosition());
                        
                        VirtualMappingTableBlockEntity nearest = findNearest(source, pos);
                        if (nearest != null) {
                            nearest.setCurrentChunkPos(cx, cz);
                            source.sendSuccess(() -> Component.literal("仮想探索機の位置を Chunk (" + cx + ", " + cz + ") に設定しました。"), true);
                            return 1;
                        } else {
                            source.sendFailure(Component.literal("周囲16ブロック以内に仮想探索機が見つかりません。"));
                            return 0;
                        }
                    })
                )
            )
        );

        dispatcher.register(Commands.literal("ve_setpos_block")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("blockX", IntegerArgumentType.integer())
                .then(Commands.argument("blockZ", IntegerArgumentType.integer())
                    .executes(context -> {
                        int bx = IntegerArgumentType.getInteger(context, "blockX");
                        int bz = IntegerArgumentType.getInteger(context, "blockZ");
                        int cx = bx >> 4;
                        int cz = bz >> 4;
                        CommandSourceStack source = context.getSource();
                        BlockPos pos = BlockPos.containing(source.getPosition());
                        
                        VirtualMappingTableBlockEntity nearest = findNearest(source, pos);
                        if (nearest != null) {
                            nearest.setCurrentChunkPos(cx, cz);
                            source.sendSuccess(() -> Component.literal("仮想探索機の位置を Block (" + bx + ", " + bz + ") [Chunk (" + cx + ", " + cz + ")] に設定しました。"), true);
                            return 1;
                        } else {
                            source.sendFailure(Component.literal("周囲16ブロック以内に仮想探索機が見つかりません。"));
                            return 0;
                        }
                    })
                )
            )
        );
    }

    private static VirtualMappingTableBlockEntity findNearest(CommandSourceStack source, BlockPos pos) {
        VirtualMappingTableBlockEntity nearest = null;
        double minDist = Double.MAX_VALUE;
        
        int radius = 16;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = pos.offset(x, y, z);
                    BlockEntity be = source.getLevel().getBlockEntity(p);
                    if (be instanceof VirtualMappingTableBlockEntity table) {
                        double d = p.distSqr(pos);
                        if (d < minDist) {
                            minDist = d;
                            nearest = table;
                        }
                    }
                }
            }
        }
        return nearest;
    }
}
