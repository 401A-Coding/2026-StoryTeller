package com.example.storyteller.ui.activity;

import android.annotation.SuppressLint;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.storyteller.R;

/**
 * 启动页：显示居中 logo 与应用名称，再跳转到主界面
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 3200L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable launchMainRunnable = this::launchMainActivity;
    private boolean launchScheduled = false;
    private boolean animationStarted = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        startEntranceAnimationIfNeeded();
        if (!launchScheduled) {
            launchScheduled = true;
            handler.postDelayed(launchMainRunnable, SPLASH_DELAY_MS);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(launchMainRunnable);
        super.onDestroy();
    }

    private void launchMainActivity() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void startEntranceAnimationIfNeeded() {
        if (animationStarted) {
            return;
        }
        animationStarted = true;

        View logo = findViewById(R.id.splash_logo);
        View appName = findViewById(R.id.splash_app_name);
        View version = findViewById(R.id.splash_app_version);
        View content = findViewById(android.R.id.content);

        content.setAlpha(0f);
        content.animate().alpha(1f).setDuration(220L).start();

        logo.setAlpha(0f);
        logo.setScaleX(0.82f);
        logo.setScaleY(0.82f);
        appName.setAlpha(0f);
        appName.setTranslationY(24f);
        version.setAlpha(0f);
        version.setTranslationY(28f);

        AnimatorSet entranceSet = new AnimatorSet();
        entranceSet.setInterpolator(new AccelerateDecelerateInterpolator());
        entranceSet.playTogether(
            ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f),
            ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.82f, 1f),
            ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.82f, 1f),
            ObjectAnimator.ofFloat(appName, View.ALPHA, 0f, 1f),
            ObjectAnimator.ofFloat(appName, View.TRANSLATION_Y, 24f, 0f),
            ObjectAnimator.ofFloat(version, View.ALPHA, 0f, 1f),
            ObjectAnimator.ofFloat(version, View.TRANSLATION_Y, 28f, 0f)
        );
        entranceSet.setDuration(1600L);
        entranceSet.start();
    }
}

