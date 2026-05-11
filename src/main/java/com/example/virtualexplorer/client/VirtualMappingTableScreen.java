package com.example.virtualexplorer.client;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.inventory.VirtualMappingTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class VirtualMappingTableScreen extends AbstractContainerScreen<VirtualMappingTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(VirtualExplorer.MODID, "textures/gui/virtual_mapping_table.png");

    private net.minecraft.client.gui.components.Button toggleBtn;

    public VirtualMappingTableScreen(VirtualMappingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 220;
        this.inventoryLabelY = this.imageHeight - 114;
    }

    @Override
    protected void init() {
        super.init();
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        this.toggleBtn = net.minecraft.client.gui.components.Button.builder(
            Component.literal(this.menu.isActive() ? "ON" : "OFF"), 
            button -> {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            })
            .bounds(i + 30, j + 80, 30, 16)
            .build();
        this.addRenderableWidget(this.toggleBtn);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, i, j, 0, 0, this.imageWidth, this.imageHeight);
        
        // プログレスバー (矢印を模した横棒)
        int scaledProgress = this.menu.getScaledProgress();
        if (scaledProgress > 0) {
            guiGraphics.fill(i + 52, j + 40, i + 52 + scaledProgress, j + 44, 0xFF00FF00);
        }
        
        // エネルギーバー
        int scaledEnergy = this.menu.getScaledEnergy(60); // 60px height
        if (scaledEnergy > 0) {
            guiGraphics.fill(i + 180, j + 20 + 60 - scaledEnergy, i + 188, j + 80, 0xFFFF0000);
        }

        // 流体バー
        int scaledFluid = this.menu.getScaledFluid(60);
        if (scaledFluid > 0) {
            guiGraphics.fill(i + 195, j + 20 + 60 - scaledFluid, i + 203, j + 80, 0xFF0000FF);
        }

        // 5x5 探索グリッド
        int gridX = i + 215;
        int gridY = j + 20;
        guiGraphics.fill(gridX - 2, gridY - 2, gridX + 5 * 8, gridY + 5 * 8, 0xFF404040);
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int color = this.menu.getGridColor(row * 5 + col);
                int x = gridX + col * 8;
                int y = gridY + row * 8;
                guiGraphics.fill(x, y, x + 7, y + 7, 0xFF000000 | color);
                if (row == 2 && col == 2) {
                    guiGraphics.fill(x + 2, y + 2, x + 5, y + 5, 0xFFFFFFFF);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 48, this.inventoryLabelY, 4210752, false);

        int statusId = this.menu.getStatusId();
        Component statusText;
        if (statusId == 7) {
            int structureId = this.menu.getTargetStructureId();
            Component structureName = Component.translatable("structure.virtualexplorer." + structureId);
            statusText = Component.translatable("gui.virtualexplorer.status.7", structureName);
        } else {
            statusText = Component.translatable("gui.virtualexplorer.status." + statusId);
        }

        int textColor = 0x404040;
        if (statusId >= 1 && statusId <= 4) textColor = 0xAA0000;
        else if (statusId >= 5) textColor = 0x007700;

        int x = (this.imageWidth - this.font.width(statusText)) / 2;
        guiGraphics.drawString(this.font, statusText, x, 92, textColor, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        if (this.toggleBtn != null) {
            this.toggleBtn.setMessage(Component.literal(this.menu.isActive() ? "ON" : "OFF"));
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        
        if (mouseX >= i + 180 && mouseX <= i + 188 && mouseY >= j + 20 && mouseY <= j + 80) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " FE"), mouseX, mouseY);
        }
        if (mouseX >= i + 195 && mouseX <= i + 203 && mouseY >= j + 20 && mouseY <= j + 80) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getFluidAmount() + " / 10000 mB"), mouseX, mouseY);
        }
    }
}
