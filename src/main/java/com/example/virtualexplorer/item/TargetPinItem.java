package com.example.virtualexplorer.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import java.util.List;
import java.util.Map;

public class TargetPinItem extends Item {
    
    public TargetPinItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack pinStack = player.getItemInHand(hand);
        
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            CompoundTag tag = pinStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            
            // 1. 構造物をチェック
            Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> structures = serverLevel.structureManager().getAllStructuresAt(player.blockPosition());
            if (!structures.isEmpty()) {
                Structure structure = structures.keySet().iterator().next();
                ResourceLocation structureId = serverLevel.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
                if (structureId != null) {
                    tag.putString("TargetStructure", structureId.toString());
                    tag.remove("TargetBiome");
                    pinStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                    player.displayClientMessage(Component.translatable("message.virtualexplorer.target_pin.set_structure", structureId.toString()), true);
                    return InteractionResultHolder.success(pinStack);
                }
            }
            
            // 2. 構造物にいなければバイオームをチェック
            ResourceLocation biomeId = serverLevel.getBiome(player.blockPosition()).unwrapKey().map(key -> key.location()).orElse(null);
            if (biomeId != null) {
                tag.putString("TargetBiome", biomeId.toString());
                tag.remove("TargetStructure");
                pinStack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.displayClientMessage(Component.translatable("message.virtualexplorer.target_pin.set_biome", biomeId.toString()), true);
                return InteractionResultHolder.success(pinStack);
            }
        }
        
        return InteractionResultHolder.pass(pinStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("TargetStructure")) {
                tooltipComponents.add(Component.translatable("tooltip.virtualexplorer.target_pin.structure", tag.getString("TargetStructure")).withStyle(net.minecraft.ChatFormatting.AQUA));
            } else if (tag.contains("TargetBiome")) {
                tooltipComponents.add(Component.translatable("tooltip.virtualexplorer.target_pin.biome", tag.getString("TargetBiome")).withStyle(net.minecraft.ChatFormatting.GREEN));
            } else {
                tooltipComponents.add(Component.translatable("tooltip.virtualexplorer.target_pin.unconfigured").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.virtualexplorer.target_pin.unconfigured").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
        tooltipComponents.add(Component.translatable("tooltip.virtualexplorer.target_pin.usage").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
