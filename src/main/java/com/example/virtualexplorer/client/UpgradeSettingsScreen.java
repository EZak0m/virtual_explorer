package com.example.virtualexplorer.client;

import com.example.virtualexplorer.block.entity.VirtualMappingTableBlockEntity;
import com.example.virtualexplorer.network.UninstallUpgradePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpgradeSettingsScreen extends Screen {
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath("virtualexplorer", "textures/gui/upgrade_settings_bg.png");

    private final Screen parent;
    private final VirtualMappingTableBlockEntity blockEntity;
    private final List<Item> installedUpgradeKeys = new ArrayList<>();
    private final Button[] extractButtons = new Button[5];
    private Button upButton;
    private Button downButton;
    private int scrollOffset = 0;

    public UpgradeSettingsScreen(Screen parent, VirtualMappingTableBlockEntity blockEntity) {
        super(Component.translatable("gui.virtualexplorer.upgrade_settings"));
        this.parent = parent;
        this.blockEntity = blockEntity;
        updateUpgradeList();
    }

    private void updateUpgradeList() {
        installedUpgradeKeys.clear();
        for (Map.Entry<Item, Integer> entry : blockEntity.getInstalledUpgrades().entrySet()) {
            if (entry.getValue() > 0) {
                installedUpgradeKeys.add(entry.getKey());
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        updateUpgradeList();
        
        int startX = (this.width - 200) / 2;
        int startY = (this.height - 200) / 2;

        // 戻るボタン
        this.addRenderableWidget(Button.builder(Component.literal("Back"), b -> {
            this.minecraft.setScreen(parent);
        }).bounds(startX + 140, startY + 172, 50, 20)
          .tooltip(Tooltip.create(Component.translatable("tooltip.virtualexplorer.back_btn")))
          .build());

        // 各スロット用のExtractボタン
        for (int i = 0; i < 5; i++) {
            final int index = i;
            extractButtons[i] = this.addRenderableWidget(Button.builder(Component.literal("Extract"), b -> {
                int actualIndex = scrollOffset + index;
                if (actualIndex < installedUpgradeKeys.size()) {
                    Item item = installedUpgradeKeys.get(actualIndex);
                    int count = blockEntity.getInstalledUpgrades().getInt(item);
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    
                    int amount;
                    if (Screen.hasShiftDown()) {
                        if (count >= 729) amount = 729;
                        else if (count >= 81) amount = 81;
                        else if (count >= 9) amount = 9;
                        else amount = count;
                    } else {
                        amount = 1;
                    }
                    
                    PacketDistributor.sendToServer(new UninstallUpgradePayload(blockEntity.getBlockPos(), id.toString(), amount));
                }
            }).bounds(startX + 140, startY + 30 + i * 28, 50, 18).build());
        }
        
        // スクロールボタン ↑
        upButton = this.addRenderableWidget(Button.builder(Component.literal("↑"), b -> {
            if (scrollOffset > 0) scrollOffset--;
        }).bounds(startX + 124, startY + 172, 14, 20)
          .tooltip(Tooltip.create(Component.translatable("tooltip.virtualexplorer.scroll_up")))
          .build());

        // スクロールボタン ↓
        downButton = this.addRenderableWidget(Button.builder(Component.literal("↓"), b -> {
            if (scrollOffset < Math.max(0, installedUpgradeKeys.size() - 5)) scrollOffset++;
        }).bounds(startX + 108, startY + 172, 14, 20)
          .tooltip(Tooltip.create(Component.translatable("tooltip.virtualexplorer.scroll_down")))
          .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int startX = (this.width - 200) / 2;
        int startY = (this.height - 200) / 2;

        // 背景画像の描画 (256x256 のテクスチャを 200x200 にフィットさせて描画)
        guiGraphics.blit(BACKGROUND_TEXTURE, startX, startY, 0f, 0f, 200, 200, 256, 256);
        
        // タイトル描画
        guiGraphics.drawCenteredString(this.font, this.title, startX + 100, startY + 10, 0xE0C068); // スチームパンク真鍮ゴールド色

        updateUpgradeList();

        // スクロールボタンの有効無効制御
        upButton.active = (scrollOffset > 0);
        downButton.active = (scrollOffset < Math.max(0, installedUpgradeKeys.size() - 5));

        // Shiftキーの押下状態によってツールチップを更新し、表示非表示もリスト長さに応じて制御
        boolean shift = Screen.hasShiftDown();
        for (int i = 0; i < 5; i++) {
            int actualIndex = scrollOffset + i;
            if (actualIndex < installedUpgradeKeys.size()) {
                Item item = installedUpgradeKeys.get(actualIndex);
                int count = blockEntity.getInstalledUpgrades().getInt(item);
                
                int y = startY + 30 + i * 28;
                
                // アイテムと個数をレンダリング
                guiGraphics.renderItem(new ItemStack(item), startX + 10, y + 1);
                
                // アイテム表示名のトリミング（画面からはみ出さないように最大13文字にトリミング）
                String displayName = item.getName(new ItemStack(item)).getString();
                if (displayName.length() > 13) {
                    displayName = displayName.substring(0, 11) + "..";
                }
                guiGraphics.drawString(this.font, displayName, startX + 30, y + 5, 0xE0E0E0, false);
                guiGraphics.drawString(this.font, "x" + count, startX + 105, y + 5, 0xFFFF55, false);

                // ボタンの有効化とツールチップ設定
                extractButtons[i].visible = true;
                extractButtons[i].active = true;
                if (shift) {
                    extractButtons[i].setTooltip(Tooltip.create(Component.translatable("tooltip.virtualexplorer.extract_btn_shift")));
                } else {
                    extractButtons[i].setTooltip(Tooltip.create(Component.translatable("tooltip.virtualexplorer.extract_btn")));
                }
            } else {
                extractButtons[i].visible = false;
                extractButtons[i].active = false;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
