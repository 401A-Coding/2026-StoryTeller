package com.example.storyteller.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;

import com.example.storyteller.R;

/**
 * 主题感知颜色工具类
 * 用于在深色/浅色主题下获取正确的颜色
 */
public class ThemeColorUtils {

    // 文本颜色
    @ColorInt
    public static int getTextPrimary(Context context) {
        return isNightMode(context) ? 0xFFFFFFFF : 0xFF212121;
    }

    @ColorInt
    public static int getTextSecondary(Context context) {
        return isNightMode(context) ? 0xFFB0B0B0 : 0xFF757575;
    }

    @ColorInt
    public static int getTextHint(Context context) {
        return isNightMode(context) ? 0xFF808080 : 0xFF9E9E9E;
    }

    // 背景颜色
    @ColorInt
    public static int getBackgroundPrimary(Context context) {
        return isNightMode(context) ? 0xFF121212 : 0xFFFAFAFA;
    }

    @ColorInt
    public static int getBackgroundSecondary(Context context) {
        return isNightMode(context) ? 0xFF1E1E1E : 0xFFF5F5F5;
    }

    @ColorInt
    public static int getBackgroundCard(Context context) {
        return isNightMode(context) ? 0xFF2D2D2D : 0xFFFFFFFF;
    }

    // 分隔线颜色
    @ColorInt
    public static int getDivider(Context context) {
        return isNightMode(context) ? 0xFF3D3D3D : 0xFFE0E0E0;
    }

    // 链接/可点击文本颜色
    @ColorInt
    public static int getLinkColor(Context context) {
        // 使用 colors.xml 中定义的 link_color
        return ContextCompat.getColor(context, R.color.link_color);
    }

    // 次要文本（用于标签、描述等）
    @ColorInt
    public static int getSecondaryTextColor(Context context) {
        return isNightMode(context) ? 0xFFB0B0B0 : 0xFF757575;
    }

    // 错误/警告颜色
    @ColorInt
    public static int getErrorColor(Context context) {
        return isNightMode(context) ? 0xFFCF6679 : 0xFFB00020;
    }

    // 检测是否为深色模式
    private static boolean isNightMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    // 便捷方法：设置 TextView 的文字颜色（主题感知）
    public static void setTextColorPrimary(android.widget.TextView textView) {
        if (textView != null) {
            textView.setTextColor(getTextPrimary(textView.getContext()));
        }
    }

    public static void setTextColorSecondary(android.widget.TextView textView) {
        if (textView != null) {
            textView.setTextColor(getTextSecondary(textView.getContext()));
        }
    }

    public static void setTextColorHint(android.widget.TextView textView) {
        if (textView != null) {
            textView.setTextColor(getTextHint(textView.getContext()));
        }
    }

    public static void setLinkColor(android.widget.TextView textView) {
        if (textView != null) {
            textView.setTextColor(getLinkColor(textView.getContext()));
        }
    }

    // 设置背景颜色（主题感知）
    public static void setBackgroundPrimary(android.view.View view) {
        if (view != null) {
            view.setBackgroundColor(getBackgroundPrimary(view.getContext()));
        }
    }

    public static void setBackgroundCard(android.view.View view) {
        if (view != null) {
            view.setBackgroundColor(getBackgroundCard(view.getContext()));
        }
    }
}