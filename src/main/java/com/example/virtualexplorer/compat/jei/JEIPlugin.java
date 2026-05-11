package com.example.virtualexplorer.compat.jei;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.init.BlockInit;
import com.example.virtualexplorer.init.ItemInit;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(VirtualExplorer.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new VirtualMappingTableRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<VirtualMappingTableRecipeCategory.VirtualRecipe> recipes = new ArrayList<>();

        // 地上
        recipes.add(new VirtualMappingTableRecipeCategory.VirtualRecipe(
            new ItemStack(ItemInit.MODULE_SURFACE.get()),
            List.of(new ItemStack(Items.OAK_LOG), new ItemStack(Items.OAK_SAPLING), new ItemStack(Items.APPLE))
        ));

        // 地下
        recipes.add(new VirtualMappingTableRecipeCategory.VirtualRecipe(
            new ItemStack(ItemInit.MODULE_UNDERGROUND.get()),
            List.of(new ItemStack(Items.RAW_IRON), new ItemStack(Items.RAW_GOLD), new ItemStack(Items.DIAMOND), new ItemStack(Items.COAL))
        ));

        // ネザー
        recipes.add(new VirtualMappingTableRecipeCategory.VirtualRecipe(
            new ItemStack(ItemInit.MODULE_NETHER.get()),
            List.of(new ItemStack(Items.QUARTZ), new ItemStack(Items.GLOWSTONE_DUST), new ItemStack(Items.NETHERRACK))
        ));

        // エンド
        recipes.add(new VirtualMappingTableRecipeCategory.VirtualRecipe(
            new ItemStack(ItemInit.MODULE_END.get()),
            List.of(new ItemStack(Items.CHORUS_FRUIT), new ItemStack(Items.END_STONE))
        ));

        registration.addRecipes(VirtualMappingTableRecipeCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(BlockInit.VIRTUAL_MAPPING_TABLE.get()), VirtualMappingTableRecipeCategory.TYPE);
    }
}
