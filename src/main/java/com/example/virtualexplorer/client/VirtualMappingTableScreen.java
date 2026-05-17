package com.example.virtualexplorer.client;

import com.example.virtualexplorer.VirtualExplorer;
import com.example.virtualexplorer.inventory.VirtualMappingTableMenu;
import com.example.virtualexplorer.inventory.GUISettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class VirtualMappingTableScreen extends AbstractContainerScreen<VirtualMappingTableMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(VirtualExplorer.MODID, "textures/gui/virtual_mapping_table.png");

    private net.minecraft.client.gui.components.Button toggleBtn;

    public VirtualMappingTableScreen(VirtualMappingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUISettings.WIDTH;
        this.imageHeight = GUISettings.HEIGHT;
        this.titleLabelX = GUISettings.TITLE_X;
        this.titleLabelY = GUISettings.TITLE_Y;
        this.inventoryLabelX = GUISettings.INV_LABEL_X;
        this.inventoryLabelY = GUISettings.INV_LABEL_Y;
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
            .bounds(i + GUISettings.TOGGLE_BTN_X, j + GUISettings.TOGGLE_BTN_Y, 30, 16)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("tooltip.virtualexplorer.toggle_btn")))
            .build();
        this.addRenderableWidget(this.toggleBtn);

        net.minecraft.client.gui.components.Button settingsBtn = net.minecraft.client.gui.components.Button.builder(
            Component.literal("⚙"), 
            button -> {
                if (this.menu.getBlockEntity() != null) {
                    this.minecraft.setScreen(new com.example.virtualexplorer.client.UpgradeSettingsScreen(this, this.menu.getBlockEntity()));
                }
            })
            .bounds(i + GUISettings.WIDTH - 20, j + 5, 15, 15)
            .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("tooltip.virtualexplorer.settings_btn")))
            .build();
        this.addRenderableWidget(settingsBtn);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, i, j, 0f, 0f, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        
        // プログレスバー (矢印)
        int scaledProgress = this.menu.getScaledProgress(GUISettings.PROGRESS_W);
        if (scaledProgress > 0) {
            guiGraphics.fill(i + GUISettings.PROGRESS_X, j + GUISettings.PROGRESS_Y, i + GUISettings.PROGRESS_X + scaledProgress, j + GUISettings.PROGRESS_Y + GUISettings.PROGRESS_H, 0xFF00FF00);
        }
        
        // エネルギーバー (右側)
        int scaledEnergy = this.menu.getScaledEnergy(GUISettings.ENERGY_BAR_H);
        if (scaledEnergy > 0) {
            guiGraphics.fill(i + GUISettings.ENERGY_BAR_X, j + GUISettings.ENERGY_BAR_Y + GUISettings.ENERGY_BAR_H - scaledEnergy, i + GUISettings.ENERGY_BAR_X + GUISettings.BAR_WIDTH, j + GUISettings.ENERGY_BAR_Y + GUISettings.ENERGY_BAR_H, 0xFFFF0000);
        }

        // 流体バー (右側)
        int scaledFluid = this.menu.getScaledFluid(GUISettings.FLUID_BAR_H);
        if (scaledFluid > 0) {
            guiGraphics.fill(i + GUISettings.FLUID_BAR_X, j + GUISettings.FLUID_BAR_Y + GUISettings.FLUID_BAR_H - scaledFluid, i + GUISettings.FLUID_BAR_X + GUISettings.BAR_WIDTH, j + GUISettings.FLUID_BAR_Y + GUISettings.FLUID_BAR_H, 0xFF0000FF);
        }

        // 5x5 探索グリッド (右端)
        int gridX = i + GUISettings.GRID_X;
        int gridY = j + GUISettings.GRID_Y;
        int cellSize = GUISettings.GRID_CELL_SIZE;
        guiGraphics.fill(gridX - 1, gridY - 1, gridX + 5 * cellSize, gridY + 5 * cellSize, 0xFF303030);
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int color = this.menu.getGridColor(row * 5 + col);
                int x = gridX + col * cellSize;
                int y = gridY + row * cellSize;
                guiGraphics.fill(x, y, x + cellSize - 1, y + cellSize - 1, 0xFF000000 | color);
                if (row == 2 && col == 2) {
                    guiGraphics.fill(x + 1, y + 1, x + cellSize - 2, y + cellSize - 2, 0xFFFFFFFF);
                }
            }
        }

        // デバッグ用マウス座標表示 (Configで切り替え)
        if (com.example.virtualexplorer.Config.SHOW_DEBUG_COORDINATES.get()) {
            guiGraphics.drawString(this.font, "X: " + mouseX + ", Y: " + mouseY, 5, 5, 0xFFFFFF);
            int relX = mouseX - i;
            int relY = mouseY - j;
            guiGraphics.drawString(this.font, "Mouse: " + relX + ", " + relY, i + 5, j + this.imageHeight - 15, 0xFFFFFF, true);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        int statusId = this.menu.getStatusId();
        Component statusText;
        if (statusId == 7) {
            String name = this.menu.getBlockEntity() != null ? this.menu.getBlockEntity().getTargetStructureName() : "";
            if (name == null || name.isEmpty()) name = "Structure";
            statusText = Component.translatable("gui.virtualexplorer.status.7", Component.literal(name));
        } else {
            statusText = Component.translatable("gui.virtualexplorer.status." + statusId);
        }

        int textColor = 0x404040;
        if (statusId >= 1 && statusId <= 4) textColor = 0xAA0000;
        else if (statusId >= 5) textColor = 0x007700;

        int x = (this.imageWidth - this.font.width(statusText)) / 2;
        guiGraphics.drawString(this.font, statusText, x, GUISettings.STATUS_TEXT_Y, textColor, false);

        // 消費電力の表示
        int energyCost = this.menu.getEnergyCost();
        Component energyText = Component.literal(energyCost + " FE/tick");
        int ex = (this.imageWidth - this.font.width(energyText)) / 2;
        guiGraphics.drawString(this.font, energyText, ex, GUISettings.STATUS_TEXT_Y + 12, 0x404040, false);
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
        
        // プログレスバー ツールチップ
        if (mouseX >= i + GUISettings.PROGRESS_X && mouseX <= i + GUISettings.PROGRESS_X + GUISettings.PROGRESS_W &&
            mouseY >= j + GUISettings.PROGRESS_Y && mouseY <= j + GUISettings.PROGRESS_Y + GUISettings.PROGRESS_H) {
            int progress = this.menu.getRawProgress();
            int max = this.menu.getMaxProgress();
            if (max > 0) {
                int percent = (progress * 100) / max;
                guiGraphics.renderTooltip(this.font, Component.literal(percent + "% (" + progress + " / " + max + " ticks)"), mouseX, mouseY);
            }
        }

        if (mouseX >= i + GUISettings.ENERGY_BAR_X && mouseX <= i + GUISettings.ENERGY_BAR_X + GUISettings.BAR_WIDTH && 
            mouseY >= j + GUISettings.ENERGY_BAR_Y && mouseY <= j + GUISettings.ENERGY_BAR_Y + GUISettings.ENERGY_BAR_H) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getEnergy() + " / " + this.menu.getMaxEnergy() + " FE"), mouseX, mouseY);
        }
        if (mouseX >= i + GUISettings.FLUID_BAR_X && mouseX <= i + GUISettings.FLUID_BAR_X + GUISettings.BAR_WIDTH && 
            mouseY >= j + GUISettings.FLUID_BAR_Y && mouseY <= j + GUISettings.FLUID_BAR_Y + GUISettings.FLUID_BAR_H) {
            guiGraphics.renderTooltip(this.font, Component.literal(this.menu.getFluidAmount() + " / 10000 mB"), mouseX, mouseY);
        }

        // グリッド ツールチップ
        int gridX = i + GUISettings.GRID_X;
        int gridY = j + GUISettings.GRID_Y;
        int cellSize = GUISettings.GRID_CELL_SIZE;
        if (mouseX >= gridX && mouseX < gridX + 5 * cellSize &&
            mouseY >= gridY && mouseY < gridY + 5 * cellSize) {
            int col = (mouseX - gridX) / cellSize;
            int row = (mouseY - gridY) / cellSize;
            int chunkX = this.menu.getCurrentChunkX() + (col - 2);
            int chunkZ = this.menu.getCurrentChunkZ() + (row - 2);
            int blockX = chunkX * 16;
            int blockZ = chunkZ * 16;

            // クライアント側ワールドからバイオーム情報を取得
            net.minecraft.world.level.Level level = net.minecraft.client.Minecraft.getInstance().level;
            Component biomeComponent = null;
            if (level != null) {
                net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(blockX, 64, blockZ);
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder = level.getBiome(pos);
                net.minecraft.resources.ResourceLocation biomeKey = biomeHolder.unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
                if (biomeKey != null) {
                    String biomeName = biomeKey.toString();
                    String descriptionId = net.minecraft.Util.makeDescriptionId("biome", biomeKey);
                    Component baseBiomeComp = Component.translatable(descriptionId);

                    net.minecraft.network.chat.TextColor color = null;
                    java.util.List<? extends String> colors = com.example.virtualexplorer.Config.BIOME_COLORS.get();
                    for (String entry : colors) {
                        String[] split = entry.split(":", 2);
                        if (split.length == 2) {
                            String key = split[0];
                            String colorVal = split[1];
                            if (!key.contains(":")) {
                                key = "minecraft:" + key;
                            }
                            String targetKey = biomeName;
                            if (!targetKey.contains(":")) {
                                targetKey = "minecraft:" + targetKey;
                            }
                            if (key.equalsIgnoreCase(targetKey)) {
                                try {
                                    String hex = colorVal.replace("#", "").trim();
                                    int rgb = Integer.parseInt(hex, 16);
                                    color = net.minecraft.network.chat.TextColor.fromRgb(rgb);
                                } catch (NumberFormatException e) {
                                    // パースエラーは無視
                                }
                            }
                        }
                    }

                    if (color != null) {
                        final net.minecraft.network.chat.TextColor finalColor = color;
                        biomeComponent = Component.literal("Biome: ").append(baseBiomeComp.copy().withStyle(style -> style.withColor(finalColor)));
                    } else {
                        biomeComponent = Component.literal("Biome: ").append(baseBiomeComp.copy().withStyle(net.minecraft.ChatFormatting.GRAY));
                    }
                }
            }

            java.util.List<Component> tooltipComponents = new java.util.ArrayList<>();
            tooltipComponents.add(Component.literal("Chunk: (" + chunkX + ", " + chunkZ + ")"));
            tooltipComponents.add(Component.literal("Block: (" + blockX + ", " + blockZ + ")"));
            if (biomeComponent != null) {
                tooltipComponents.add(biomeComponent);
            }

            guiGraphics.renderComponentTooltip(this.font, tooltipComponents, mouseX, mouseY);
        }
    }

}
