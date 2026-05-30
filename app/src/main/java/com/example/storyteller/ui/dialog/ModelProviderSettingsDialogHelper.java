package com.example.storyteller.ui.dialog;

import android.content.Context;
import android.content.Intent;

import com.example.storyteller.ui.activity.MainActivity;

/**
 * 模型供应商配置相关提示弹窗。
 */
public final class ModelProviderSettingsDialogHelper {

    private ModelProviderSettingsDialogHelper() {
        // Utility class
    }

    public static void showApiKeyRequiredDialog(Context context, String usageLabel) {
        showApiKeyRequiredDialog(context, null, usageLabel);
    }

    public static void showApiKeyRequiredDialog(Context context, String providerName, String usageLabel) {
        String action = normalizeAction(usageLabel);
        String provider = normalizeProvider(providerName);
        String title = provider == null ? "未配置 API Key" : "未配置 " + provider + " API Key";
        String message = provider == null
            ? "请先前往设置配置 API Key，再" + action + "。"
            : "请先前往设置配置 " + provider + " API Key，再" + action + "。";

        new android.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("取消", null)
            .setPositiveButton("去设置", (dialog, which) -> openSettings(context))
            .show();
    }

    public static void showProviderDisabledDialog(Context context, String usageLabel) {
        showProviderDisabledDialog(context, null, usageLabel);
    }

    public static void showProviderDisabledDialog(Context context, String providerName, String usageLabel) {
        String action = normalizeAction(usageLabel);
        String provider = normalizeProvider(providerName);
        String title = provider == null ? "模型供应商未启用" : provider + " 未启用";
        String message = provider == null
            ? "请先前往设置启用模型供应商，再" + action + "。"
            : "请先前往设置启用 " + provider + " 提供商，再" + action + "。";

        new android.app.AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("取消", null)
            .setPositiveButton("去设置", (dialog, which) -> openSettings(context))
            .show();
    }

    private static String normalizeProvider(String providerName) {
        if (providerName == null) {
            return null;
        }
        String trimmed = providerName.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeAction(String usageLabel) {
        if (usageLabel == null) {
            return "继续操作";
        }
        String trimmed = usageLabel.trim();
        return trimmed.isEmpty() ? "继续操作" : trimmed;
    }

    private static void openSettings(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}



