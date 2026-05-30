package com.example.storyteller.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import com.example.storyteller.R;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * 启动页：显示居中 logo 与应用名称，再跳转到主界面
 */
@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1800L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable launchMainRunnable = this::launchMainActivity;
    private boolean launchScheduled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        View content = findViewById(android.R.id.content);
        content.setAlpha(0f);
        content.animate().alpha(1f).setDuration(220L).start();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
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
}

