package com.example.virtualexplorer.network;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

public record UninstallUpgradePayload(BlockPos pos, String itemId, int amount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UninstallUpgradePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(VirtualExplorer.MODID, "uninstall_upgrade"));

    public static final StreamCodec<FriendlyByteBuf, UninstallUpgradePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeUtf(payload.itemId());
                buf.writeInt(payload.amount());
            },
            buf -> new UninstallUpgradePayload(buf.readBlockPos(), buf.readUtf(), buf.readInt())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UninstallUpgradePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            Level level = player.level();
            BlockPos pos = payload.pos();
            if (level.isLoaded(pos)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof VirtualMappingTableBlockEntity vmt) {
                    Item targetItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(payload.itemId()));
                    if (targetItem != net.minecraft.world.item.Items.AIR) {
                        int currentCount = vmt.getInstalledUpgrades().getInt(targetItem);
                        int removeAmount = Math.min(payload.amount(), currentCount);
                        
                        if (removeAmount > 0) {
                            vmt.getInstalledUpgrades().put(targetItem, currentCount - removeAmount);
                            if (vmt.getInstalledUpgrades().getInt(targetItem) <= 0) {
                                vmt.getInstalledUpgrades().removeInt(targetItem);
                            }
                            vmt.setChanged();

                            // アイテムをプレイヤーに返す処理
                            int remaining = removeAmount;
                            
                            // 圧縮アイテムの生成ロジック
                            java.util.List<Item> compItems = new java.util.ArrayList<>();
                            for (var comp : com.example.virtualexplorer.init.ItemInit.COMPRESSED_UPGRADES) {
                                com.example.virtualexplorer.item.CompressedUpgradeItem cui = comp.get();
                                if (cui.getBaseUpgrade() == targetItem) {
                                    compItems.add(cui);
                                }
                            }
                            
                            // 倍率降順にソート (729x, 81x, 9x)
                            compItems.sort((a, b) -> Integer.compare(((com.example.virtualexplorer.item.CompressedUpgradeItem)b).getMultiplier(), ((com.example.virtualexplorer.item.CompressedUpgradeItem)a).getMultiplier()));

                            for (Item comp : compItems) {
                                int mult = ((com.example.virtualexplorer.item.CompressedUpgradeItem)comp).getMultiplier();
                                while (remaining >= mult) {
                                    int countToGive = remaining / mult;
                                    ItemStack stack = new ItemStack(comp, 1);
                                    int maxStack = stack.getMaxStackSize();
                                    int giving = Math.min(countToGive, maxStack);
                                    
                                    player.getInventory().placeItemBackInInventory(new ItemStack(comp, giving));
                                    remaining -= giving * mult;
                                }
                            }
                            
                            while (remaining > 0) {
                                ItemStack stack = new ItemStack(targetItem, 1);
                                int maxStack = stack.getMaxStackSize();
                                int giving = Math.min(remaining, maxStack);
                                
                                player.getInventory().placeItemBackInInventory(new ItemStack(targetItem, giving));
                                remaining -= giving;
                            }
                        }
                    }
                }
            }
        });
    }
}
