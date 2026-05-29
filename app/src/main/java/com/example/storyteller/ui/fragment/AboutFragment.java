package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.storyteller.BuildConfig;
import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 关于页面
 * 显示应用信息和相关链接
 */
public class AboutFragment extends BaseFragment {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/401A-Coding/2026-StoryTeller/releases/latest";
    private static final String GITHUB_RELEASES_URL = "https://github.com/401A-Coding/2026-StoryTeller/releases";

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_about;
    }

    @Override
    protected void initView(View view) {
        // 版本信息
        TextView tvVersion = view.findViewById(R.id.tv_version);
        tvVersion.setText("版本 " + BuildConfig.VERSION_NAME);

        // GitHub链接
        view.findViewById(R.id.btn_github).setOnClickListener(v -> openLink("https://github.com/401A-Coding/2026-StoryTeller"));

        // 项目主页
        view.findViewById(R.id.btn_project_page).setOnClickListener(v -> openLink("https://401a-coding.github.io/2026-StoryTeller"));

        // 问题反馈
        view.findViewById(R.id.btn_feedback).setOnClickListener(v -> openLink("https://github.com/401A-Coding/2026-StoryTeller/issues"));

        // 许可证
        view.findViewById(R.id.btn_license).setOnClickListener(v -> showLicense());

        // 检查更新
        view.findViewById(R.id.btn_check_update).setOnClickListener(v -> checkForUpdate());
    }

    @Override
    protected void initData() {
        // 不需要额外初始化
    }

    private void openLink(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLicense() {
        String licenseContent = readLicenseFile();
        if (licenseContent == null) {
            licenseContent = "StoryTeller 是一个开源项目，基于 MIT 许可证开源。\n\n" +
                    "您可以自由使用、修改和分发本项目的代码，但需要保留原作者的版权声明。\n\n" +
                    "详细许可证信息请访问项目主页。";
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("开源许可证")
                .setMessage(licenseContent)
                .setPositiveButton("确定", null)
                .show();
    }

    private String readLicenseFile() {
        try {
            InputStream inputStream = requireContext().getAssets().open("LICENSE");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString().trim();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void checkForUpdate() {
        Toast.makeText(requireContext(), "正在检查更新...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Gson gson = new Gson();
                    GithubRelease release = gson.fromJson(response.toString(), GithubRelease.class);

                    requireActivity().runOnUiThread(() -> showUpdateDialog(release));
                } else {
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "检查更新失败", Toast.LENGTH_SHORT).show());
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showUpdateDialog(GithubRelease release) {
        String latestVersion = release.tag_name;
        String currentVersion = BuildConfig.VERSION_NAME;

        if (compareVersions(latestVersion, currentVersion) > 0) {
            // 有新版本
            new AlertDialog.Builder(requireContext())
                    .setTitle("发现新版本")
                    .setMessage("最新版本：" + latestVersion + "\n\n" + release.body)
                    .setPositiveButton("前往下载", (dialog, which) -> openLink(release.html_url))
                    .setNegativeButton("暂不更新", null)
                    .show();
        } else {
            // 已是最新版本
            new AlertDialog.Builder(requireContext())
                    .setTitle("检查更新")
                    .setMessage("当前版本已是最新版本（" + currentVersion + "）")
                    .setPositiveButton("确定", null)
                    .show();
        }
    }

    private int compareVersions(String version1, String version2) {
        // 移除 v 前缀
        version1 = version1.startsWith("v") ? version1.substring(1) : version1;
        version2 = version2.startsWith("v") ? version2.substring(1) : version2;

        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (v1 != v2) {
                return v1 - v2;
            }
        }
        return 0;
    }

    // GitHub Release JSON 对应类
    private static class GithubRelease {
        String tag_name;
        String name;
        String body;
        String html_url;
    }
}