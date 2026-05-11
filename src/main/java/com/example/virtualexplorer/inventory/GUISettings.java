package com.example.virtualexplorer.inventory;

/**
 * GUIの要素配置座標を一括管理するクラスです。
 * 数値を変更することで、スロットと描画物の位置を同時に調整できます。
 */
public class GUISettings {
    // 全体サイズ
    public static final int WIDTH = 256;
    public static final int HEIGHT = 220;

    // --- スロット座標 (左上 i, j からの相対座標) ---
    public static final int SLOT_UPGRADE_X = 30;
    public static final int SLOT_UPGRADE_Y = 55;
    public static final int SLOT_UPGRADE_SPACING = 18;

    public static final int SLOT_MODULE_X = 40;
    public static final int SLOT_MODULE_Y = 25;

    public static final int SLOT_MAP_X = 55;
    public static final int SLOT_MAP_Y = 55;

    public static final int SLOT_FILTER_X = 55;
    public static final int SLOT_FILTER_Y = 73;

    public static final int SLOT_OUTPUT_X = 90;
    public static final int SLOT_OUTPUT_Y = 28;
    public static final int SLOT_OUTPUT_SPACING = 18;

    public static final int PLAYER_INV_X = 47;
    public static final int PLAYER_INV_Y = 120;

    // --- 描画物座標 ---
    public static final int TITLE_X = 40;
    public static final int TITLE_Y = 15;

    public static final int INV_LABEL_X = 47;
    public static final int INV_LABEL_Y = 110;

    public static final int TOGGLE_BTN_X = 55;
    public static final int TOGGLE_BTN_Y = 92;

    public static final int PROGRESS_X = 85;
    public static final int PROGRESS_Y = 75;
    public static final int PROGRESS_W = 24; // 長さ
    public static final int PROGRESS_H = 4; // 太さ

    public static final int ENERGY_BAR_X = 199;
    public static final int ENERGY_BAR_Y = 13;
    public static final int ENERGY_BAR_H = 40;
    public static final int BAR_WIDTH = 8; // バーの太さ

    public static final int FLUID_BAR_X = 224;
    public static final int FLUID_BAR_Y = 13;
    public static final int FLUID_BAR_H = 40;

    public static final int GRID_X = 200;
    public static final int GRID_Y = 77;
    public static final int GRID_CELL_SIZE = 8;

    public static final int STATUS_TEXT_Y = 95;
}
