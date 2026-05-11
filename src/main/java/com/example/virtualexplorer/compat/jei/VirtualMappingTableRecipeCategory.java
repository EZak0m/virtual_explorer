package com.example.virtualexplorer.compat.jei;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.init.BlockInit;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class VirtualMappingTableRecipeCategory implements IRecipeCategory<VirtualMappingTableRecipeCategory.VirtualRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(VirtualExplorer.MODID, "virtual_mapping_table");
    public static final RecipeType<VirtualRecipe> TYPE = new RecipeType<>(UID, VirtualRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public VirtualMappingTableRecipeCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(120, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(BlockInit.VIRTUAL_MAPPING_TABLE.get()));
    }

    @Override
    public RecipeType<VirtualRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.virtualexplorer.virtual_mapping_table");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VirtualRecipe recipe, IFocusGroup focuses) {
        // Module Slot
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 10).addItemStack(recipe.module());
        // Map Slot
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 30).addItemStack(new ItemStack(Items.MAP));
        
        // Output Slots (Sample of results)
        for (int i = 0; i < Math.min(recipe.results().size(), 8); i++) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 50 + (i % 4) * 18, 10 + (i / 4) * 18)
                   .addItemStack(recipe.results().get(i));
        }
    }

    public record VirtualRecipe(ItemStack module, List<ItemStack> results) {}
}
