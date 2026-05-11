package com.example.virtualexplorer.api;

/**
 * 仮想探索の各モジュール（地下探索、ネザー探索など）の振る舞いを定義するインターフェース。
 * MinecraftのAPIに依存させず、純粋なパラメータの提供に徹することでテストを容易にします。
 */
public interface IExplorerModule {
    /**
     * 1回の探索オペレーションにかかる基本エネルギー（FE）コスト。
     * @return FEコスト
     */
    int getBaseEnergyCost();

    /**
     * 1回の探索オペレーションにかかる基本処理時間（Tick数）。
     * @return 処理時間（Tick）
     */
    int getBaseProcessingTime();

    /**
     * モジュールの識別子（例："underground", "nether"）。
     * @return 識別子文字列
     */
    String getModuleId();
}
