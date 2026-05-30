package com.example.storyteller.ui.dialog;

import android.content.Context;
import android.content.Intent;

import com.example.storyteller.ui.activity.MainActivity;

/**
 * MiniMax 配置相关提示弹窗。
 */
public final class MiniMaxSettingsDialogHelper {

    private MiniMaxSettingsDialogHelper() {
        // Utility class
    }

    public static void showApiKeyRequiredDialog(Context context, String usageLabel) {
        new android.app.AlertDialog.Builder(context)
            .setTitle("未配置 MiniMax API Key")
            .setMessage("请先前往设置配置 MiniMax API Key，再" + usageLabel + "。")
            .setNegativeButton("取消", null)
            .setPositiveButton("去设置", (dialog, which) -> openSettings(context))
            .show();
    }

    public static void showProviderDisabledDialog(Context context, String usageLabel) {
        new android.app.AlertDialog.Builder(context)
            .setTitle("MiniMax 未启用")
            .setMessage("请先前往设置启用 MiniMax 提供商，再" + usageLabel + "。")
            .setNegativeButton("取消", null)
            .setPositiveButton("去设置", (dialog, which) -> openSettings(context))
            .show();
    }

    private static void openSettings(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}

