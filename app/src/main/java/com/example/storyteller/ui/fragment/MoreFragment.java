package com.example.storyteller.ui.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Story;
import com.example.storyteller.model.Volume;
import com.example.storyteller.utils.JsonUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 更多功能Fragment
 * 收纳作品管理、设置、帮助等次要功能
 */
public class MoreFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    private int storyId;
    private StoryDao storyDao;
    private Story currentStory;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private ApiClient apiClient = ApiClient.getInstance();
    private Bitmap pendingCoverBitmap;
    private int pendingSaveAsCover = 0;

    // 图片选择Launcher
    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    handleSelectedImage(uri, pendingSaveAsCover);
                }
            });

    // 文件选择器Launcher
    private final ActivityResultLauncher<String> createDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("text/markdown"), uri -> {
                if (uri != null) {
                    saveExportedFile(uri);
                }
            });

    public static MoreFragment newInstance(int storyId) {
        MoreFragment fragment = new MoreFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_more;
    }

    @Override
    protected void initView(View view) {
        storyDao = new StoryDao(requireContext());

        // 作品管理
        view.findViewById(R.id.btn_export).setOnClickListener(v -> exportStory());
        view.findViewById(R.id.btn_share).setOnClickListener(v -> shareStory());
        view.findViewById(R.id.btn_share_image).setOnClickListener(v -> shareStoryImage());

        // 帮助与反馈
        view.findViewById(R.id.btn_help).setOnClickListener(v -> showHelp());
        view.findViewById(R.id.btn_feedback).setOnClickListener(v -> showFeedback());
    }

    @Override
    protected void initData() {
        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }
    }

    /**
     * 导出作品
     */
    private void exportStory() {
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "作品不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = sanitizeFileName(currentStory.getTitle()) + ".md";
        createDocumentLauncher.launch(fileName);
    }

    /**
     * 保存导出文件
     */
    private void saveExportedFile(Uri uri) {
        try {
            String content = buildMarkdownContent();
            
            java.io.OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(content.getBytes("UTF-8"));
                outputStream.close();
                Toast.makeText(requireContext(), "导出成功！", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 构建Markdown内容
     */
    private String buildMarkdownContent() {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(currentStory.getTitle()).append("\n\n");

        String description = currentStory.getDescription();
        if (description != null && !description.isEmpty()) {
            sb.append("## 简介\n\n").append(description).append("\n\n");
        }

        // 章节内容
        String structure = currentStory.getStructure();
        if (structure != null && !structure.isEmpty()) {
            try {
                List<Volume> volumes = JsonUtils.fromJson(structure,
                    new com.google.gson.reflect.TypeToken<List<Volume>>(){}.getType());
                if (volumes != null) {
                    for (int i = 0; i < volumes.size(); i++) {
                        Volume volume = volumes.get(i);
                        String volumeTitle = volume.getTitle();
                        if (volumeTitle == null || volumeTitle.isEmpty()) {
                            volumeTitle = "第" + (i + 1) + "卷";
                        }
                        sb.append("## ").append(volumeTitle).append("\n\n");

                        List<Chapter> chapters = volume.getChapters();
                        if (chapters != null) {
                            for (int j = 0; j < chapters.size(); j++) {
                                Chapter chapter = chapters.get(j);
                                String chapterTitle = chapter.getTitle();
                                String chapterContent = chapter.getContent();

                                if (chapterTitle != null && !chapterTitle.isEmpty()) {
                                    sb.append("### ").append(chapterTitle).append("\n\n");
                                }
                                if (chapterContent != null && !chapterContent.isEmpty()) {
                                    sb.append(chapterContent);
                                } else {
                                    sb.append("（暂无正文）");
                                }
                                sb.append("\n\n");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sb.append("\n（卷章节解析失败）\n");
            }
        } else {
            sb.append("\n（暂无卷章节结构）\n");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        sb.append("---\n");
        sb.append("**导出时间**：").append(sdf.format(new Date())).append("\n");
        sb.append("**导出工具**：StoryTeller\n");

        return sb.toString();
    }

    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "未命名作品";
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 分享作品（分享完整Markdown文件）
     */
    private void shareStory() {
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "作品不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String content = buildMarkdownContent();
            String fileName = sanitizeFileName(currentStory.getTitle()) + ".md";

            // 保存到缓存目录
            File cacheDir = requireContext().getCacheDir();
            File shareFile = new File(cacheDir, fileName);
            FileOutputStream fos = new FileOutputStream(shareFile);
            fos.write(content.getBytes("UTF-8"));
            fos.flush();
            fos.close();

            // 通过FileProvider分享
            Uri contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    shareFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/markdown");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享作品：" + currentStory.getTitle());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooserIntent = Intent.createChooser(shareIntent, "分享作品");
            startActivity(chooserIntent);

            shareFile.deleteOnExit();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示帮助
     */
    private void showHelp() {
        String helpContent = "📖 功能概览\n\n" +
                "【写作Tab】\n" +
                "• 大纲：生成/查看小说整体框架\n" +
                "• 章节：编辑卷结构和章节内容\n" +
                "• AI辅助：创作建议、续写、优化\n\n" +
                "【架构Tab】\n" +
                "• 基本信息：书名、简介、标签\n" +
                "• 大纲：查看/编辑故事主线\n" +
                "• 设定：角色、物品、地点等设定\n" +
                "• 关系：管理角色间的关系\n\n" +
                "【AI功能】\n" +
                "• 智能体：执行复杂创作任务\n" +
                "• 生图：AI生成封面/配图\n" +
                "• 审校：全面审核内容质量\n\n" +
                "【快捷操作】\n" +
                "• 点击设定卡片可查看预览\n" +
                "• 分享卡片可自定义封面\n\n" +
                "【数据安全】\n" +
                "• 自动保存，无需手动保存\n" +
                "• 随时可导出为TXT文件\n\n" +
                "如需帮助，请通过GitHub提交Issue";

        new AlertDialog.Builder(requireContext())
            .setTitle("使用帮助")
            .setMessage(helpContent)
            .setPositiveButton("确定", null)
            .setNegativeButton("访问GitHub", (dialog, which) -> {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://github.com/401A-Coding/2026-StoryTeller"));
                startActivity(intent);
            })
            .show();
    }

    /**
     * 显示反馈
     */
    private void showFeedback() {
        String[] options = {"GitHub Issue（功能建议/Bug）", "发送邮件（其他问题）", "访问项目主页"};
        new AlertDialog.Builder(requireContext())
            .setTitle("意见反馈")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // GitHub Issue
                        Intent issueIntent = new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/401A-Coding/2026-StoryTeller/issues/new"));
                        startActivity(issueIntent);
                        break;
                    case 1: // 发送邮件
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                        emailIntent.setData(android.net.Uri.parse("mailto:"));
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"1750096317@qq.com"});
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "StoryTeller意见反馈");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, "\n\n---\n" +
                            "版本：1.0\n" +
                            "设备：Android\n");
                        if (emailIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                            startActivity(Intent.createChooser(emailIntent, "发送邮件"));
                        } else {
                            Toast.makeText(requireContext(), "未找到邮件应用", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2: // 项目主页
                        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/401A-Coding/2026-StoryTeller"));
                        startActivity(webIntent);
                        break;
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // ==================== 图片分享功能 ====================

    /**
     * 分享作品图片（卡片形式）
     */
    private void shareStoryImage() {
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "作品不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        showCoverSourceDialog();
    }

    /**
     * 显示封面来源选择对话框
     */
    private void showCoverSourceDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_share_cover, null);

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        builder.setNegativeButton("取消", null);
        android.app.AlertDialog dialog = builder.create();

        // 检查是否存在已有封面
        String existingCoverPath = currentStory.getCoverPath();
        boolean hasExistingCover = existingCoverPath != null && !existingCoverPath.isEmpty()
                && new File(existingCoverPath).exists();
        // 选项0：使用现有封面
        View cardUseExisting = dialogView.findViewById(R.id.card_use_existing);
        if (hasExistingCover) {
            cardUseExisting.setVisibility(View.VISIBLE);
            cardUseExisting.setOnClickListener(v -> {
                dialog.dismiss();
                generateAndShareWithExistingCover(existingCoverPath);
            });
        } else {
            cardUseExisting.setVisibility(View.GONE);
        }

        dialogView.findViewById(R.id.card_select_image).setOnClickListener(v -> {
            CheckBox cb = dialogView.findViewById(R.id.cb_save_as_cover_1);
            pendingSaveAsCover = cb.isChecked() ? 1 : 0;
            pendingCoverBitmap = null;
            dialog.dismiss();
            selectImageLauncher.launch("image/*");
        });

        dialogView.findViewById(R.id.card_generate_image).setOnClickListener(v -> {
            CheckBox cb = dialogView.findViewById(R.id.cb_save_as_cover_2);
            pendingSaveAsCover = cb.isChecked() ? 1 : 0;
            pendingCoverBitmap = null;
            dialog.dismiss();
            generateAiCover();
        });

        dialogView.findViewById(R.id.card_gradient_bg).setOnClickListener(v -> {
            CheckBox cb = dialogView.findViewById(R.id.cb_save_as_cover_3);
            pendingSaveAsCover = cb.isChecked() ? 1 : 0;
            pendingCoverBitmap = null;
            dialog.dismiss();
            generateAndShareWithGradient();
        });

        dialog.show();
    }

    /**
     * 处理选择的本地图片
     */
    private void handleSelectedImage(Uri uri, int saveAsCover) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            pendingCoverBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            if (saveAsCover == 1) {
                saveCoverImageToFile(pendingCoverBitmap);
            }
            generateAndShareWithBitmap(pendingCoverBitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "图片加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * AI生成封面图片
     */
    private void generateAiCover() {
        // 显示等待对话框
        AlertDialog loadingDialog = new AlertDialog.Builder(requireContext())
                .setMessage("正在生成封面...\n请稍候")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        String prompt = ApiClient.buildCoverPrompt(
                currentStory.getTitle(),
                currentStory.getDescription(),
                null
        );

        apiClient.generateCover(prompt, 1, requireContext(), new ApiClient.CoverCallback() {
            @Override
            public void onSuccess(List<String> imageUrls) {
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    if (imageUrls != null && !imageUrls.isEmpty()) {
                        downloadAndShareCover(imageUrls.get(0));
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(requireContext(), "生成失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 下载AI生成的图片并分享
     */
    private void downloadAndShareCover(String imageUrl) {
        Toast.makeText(requireContext(), "正在下载封面...", Toast.LENGTH_SHORT).show();

        apiClient.downloadImageAsBitmap(imageUrl, requireContext(), executor,
                bitmap -> {
                    requireActivity().runOnUiThread(() -> {
                        if (pendingSaveAsCover == 1) {
                            saveCoverImageToFile(bitmap);
                        }
                        generateAndShareWithBitmap(bitmap);
                    });
                },
                e -> {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "下载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
        );
    }

    /**
     * 保存封面图片到文件
     */
    private void saveCoverImageToFile(Bitmap bitmap) {
        try {
            File coverDir = new File(requireContext().getFilesDir(), "covers");
            if (!coverDir.exists()) {
                coverDir.mkdirs();
            }

            String fileName = "cover_" + currentStory.getId() + "_" + System.currentTimeMillis() + ".jpg";
            File coverFile = new File(coverDir, fileName);

            FileOutputStream fos = new FileOutputStream(coverFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            String coverPath = coverFile.getAbsolutePath();
            currentStory.setCoverPath(coverPath);
            storyDao.updateStoryCoverPath(currentStory.getId(), coverPath);

            Toast.makeText(requireContext(), "封面已保存", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "保存封面失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 使用图片生成并分享卡片
     * 参考番茄小说分享卡片样式
     */
    private void generateAndShareWithBitmap(Bitmap coverBitmap) {
        int cardWidth = 540;
        int cardHeight = 960;
        Bitmap cardBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cardBitmap);

        // 浅蓝色渐变背景
        Paint bgPaint = new Paint();
        LinearGradient bgGradient = new LinearGradient(
                0, 0, 0, cardHeight,
                new int[]{0xFFD0EDF9, 0xFFE6F5FC},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP);
        bgPaint.setShader(bgGradient);
        canvas.drawRect(0, 0, cardWidth, cardHeight, bgPaint);

        int margin = 30;
        int contentTop = 55;

        // ========== 顶部分享信息（居中）==========
        Paint headerPaint = new Paint();
        headerPaint.setColor(0xFF555555);
        headerPaint.setTextSize(22);
        headerPaint.setTextAlign(Paint.Align.CENTER);
        headerPaint.setAntiAlias(true);
        String shareInfo = "来自StoryTeller的分享 · " + getCurrentDate();
        canvas.drawText(shareInfo, cardWidth / 2f, contentTop + 25, headerPaint);

        // ========== 封面阴影 ==========
        int coverW = 160;
        int coverH = 220;
        int coverLeft = (cardWidth - coverW) / 2;
        int coverTop = contentTop + 70; // 增加间距

        Paint shadowPaint = new Paint();
        shadowPaint.setColor(0x33000000);
        shadowPaint.setAntiAlias(true);
        RectF shadowRect = new RectF(coverLeft + 4, coverTop + 4, coverLeft + coverW + 4, coverTop + coverH + 4);
        canvas.drawRoundRect(shadowRect, 12, 12, shadowPaint);

        // ========== 封面图片（带圆角）==========
        Paint coverPaint = new Paint();
        coverPaint.setAntiAlias(true);
        coverPaint.setFilterBitmap(true);

        RectF coverRect = new RectF(coverLeft, coverTop, coverLeft + coverW, coverTop + coverH);

        // 使用Path创建圆角裁剪区域
        float cornerRadius = 12;
        android.graphics.Path coverPath = new android.graphics.Path();
        coverPath.addRoundRect(coverRect, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW);
        canvas.save();
        canvas.clipPath(coverPath);

        // 缩放封面图片使其填充整个区域
        float scaleX = (float) coverW / coverBitmap.getWidth();
        float scaleY = (float) coverH / coverBitmap.getHeight();
        float scale = Math.max(scaleX, scaleY);
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postScale(scale, scale);

        // 计算居中偏移（裁剪多余部分）
        float scaledW = coverBitmap.getWidth() * scale;
        float scaledH = coverBitmap.getHeight() * scale;
        float dx = coverLeft - (scaledW - coverW) / 2f;
        float dy = coverTop - (scaledH - coverH) / 2f;
        matrix.postTranslate(dx, dy);

        canvas.drawBitmap(coverBitmap, matrix, coverPaint);
        canvas.restore();

        // 封面边框（圆角）
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(0x22000000);
        borderPaint.setStrokeWidth(1);
        borderPaint.setAntiAlias(true);
        canvas.drawRoundRect(coverRect, cornerRadius, cornerRadius, borderPaint);

        drawCardText(canvas, cardWidth, cardHeight, coverTop + coverH + 35);
        shareBitmap(cardBitmap);
    }

    /**
     * 获取当前日期
     */
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日", Locale.CHINESE);
        return sdf.format(new Date());
    }

    /**
     * 使用渐变背景生成并分享卡片
     */
    private void generateAndShareWithGradient() {
        int cardWidth = 540;
        int cardHeight = 960;
        Bitmap cardBitmap = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cardBitmap);

        int[] colors = getGradientColors(currentStory.getTitle());

        Paint gradientPaint = new Paint();
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, cardHeight,
                colors[0], colors[1],
                Shader.TileMode.CLAMP);
        gradientPaint.setShader(gradient);
        canvas.drawRect(0, 0, cardWidth, cardHeight, gradientPaint);

        int contentTop = 55;
        int margin = 30;
        drawCardText(canvas, cardWidth, cardHeight, contentTop + 50);
        shareBitmap(cardBitmap);
    }

    /**
     * 绘制卡片文字区域（参考番茄小说样式）
     */
    private void drawCardText(Canvas canvas, int cardWidth, int cardHeight, int contentStartY) {
        int margin = 30;
        int contentEndY = cardHeight - 140;

        // ========== 书名 ==========
        Paint titlePaint = new Paint();
        titlePaint.setColor(0xFF222222);
        titlePaint.setTextSize(36);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setAntiAlias(true);

        String title = currentStory.getTitle();
        int titleY = contentStartY + 30; // 增加间距

        String[] titleLines = breakTitle(title, 14);
        for (int i = 0; i < titleLines.length && i < 2; i++) {
            canvas.drawText(titleLines[i], cardWidth / 2f, titleY + i * 50, titlePaint); // 增加行距
        }
        int titleBottomY = titleY + titleLines.length * 50 + 30; // 增加间距

        // ========== 简介 ==========
        String description = currentStory.getDescription();
        if (description != null && !description.isEmpty()) {
            Paint descPaint = new Paint();
            descPaint.setColor(0xFF555555);
            descPaint.setTextSize(24); // 稍微增大字体
            descPaint.setTextAlign(Paint.Align.LEFT);
            descPaint.setAntiAlias(true);

            int maxWidth = cardWidth - margin * 2;
            int lineHeight = 44; // 增加行高
            wrapText(description, maxWidth, 6, descPaint, canvas, titleBottomY, lineHeight, margin);
        }

        // ========== 分隔线 ==========
        int dividerY = contentEndY - 30; // 增加间距
        Paint dividerPaint = new Paint();
        dividerPaint.setColor(0xFFDDDDDD);
        dividerPaint.setStrokeWidth(1);
        canvas.drawLine(margin, dividerY, cardWidth - margin, dividerY, dividerPaint);

        // ========== 底部应用信息 ==========
        Paint appNamePaint = new Paint();
        appNamePaint.setColor(0xFF222222);
        appNamePaint.setTextSize(28);
        appNamePaint.setAntiAlias(true);
        canvas.drawText("StoryTeller", margin, dividerY + 55, appNamePaint); // 增加间距

        Paint appTipPaint = new Paint();
        appTipPaint.setColor(0xFF777777);
        appTipPaint.setTextSize(20);
        appTipPaint.setAntiAlias(true);
        canvas.drawText("扫码关注获取更多内容", margin, dividerY + 90, appTipPaint); // 增加间距

        // ========== 二维码 ==========
        drawQRCode(canvas, cardWidth, cardHeight, dividerY);
    }

    /**
     * 绘制二维码图片
     */
    private void drawQRCode(Canvas canvas, int cardWidth, int cardHeight, int dividerY) {
        try {
            String url = "https://github.com/401A-Coding/2026-StoryTeller";
            int qrSize = 100;
            int qrLeft = cardWidth - qrSize - 30;
            int qrTop = dividerY + 20;

            // 使用ZXing生成二维码Bitmap
            java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
            hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");

            com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(url, com.google.zxing.BarcodeFormat.QR_CODE, qrSize, qrSize, hints);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF222222 : 0xFFFFFFFF;
                }
            }

            Bitmap qrBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            qrBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            // 绘制二维码
            canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null);

            // 二维码边框
            Paint borderPaint = new Paint();
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setColor(0xFFDDDDDD);
            borderPaint.setStrokeWidth(1);
            borderPaint.setAntiAlias(true);
            canvas.drawRect(qrLeft - 2, qrTop - 2, qrLeft + qrSize + 2, qrTop + qrSize + 2, borderPaint);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将标题分成多行
     */
    private String[] breakTitle(String title, int maxChars) {
        if (title.length() <= maxChars) {
            return new String[]{title};
        }
        int mid = title.length() / 2;
        int spaceIdx = title.indexOf(' ', mid);
        if (spaceIdx > mid && spaceIdx < title.length() - 1) {
            mid = spaceIdx;
        } else {
            spaceIdx = title.lastIndexOf(' ', mid);
            if (spaceIdx > mid - 5) {
                mid = spaceIdx;
            }
        }
        String line1 = title.substring(0, mid).trim();
        String line2 = title.substring(mid).trim();
        return new String[]{line1, line2};
    }

    /**
     * 换行文本绘制（支持中英文混合）
     */
    private void wrapText(String text, int maxWidth, int maxLines, Paint paint, Canvas canvas, int startY, int lineHeight, int leftMargin) {
        if (text == null || text.isEmpty()) return;

        StringBuilder currentLine = new StringBuilder();
        int lines = 0;
        int currentY = startY;
        boolean exceeded = false; // 是否已超出区域
        StringBuilder overflowContent = new StringBuilder(); // 超出部分的内容

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (exceeded) {
                // 已经超出区域，只收集超出部分的内容用于最后截断
                overflowContent.append(c);
                continue;
            }

            String testLine = currentLine.toString() + c;
            float testWidth = paint.measureText(testLine);

            if (testWidth > maxWidth && currentLine.length() > 0) {
                // 当前行已满，需要换行
                if (lines < maxLines - 1) {
                    // 还未达到最后一行，正常绘制
                    canvas.drawText(currentLine.toString(), leftMargin, currentY, paint);
                    currentY += lineHeight;
                    lines++;
                } else {
                    // 已经是最后一行，标记为超出，后续内容收集起来
                    exceeded = true;
                    overflowContent.append(currentLine.toString());
                    overflowContent.append(c);
                }
                currentLine = new StringBuilder();
                // 检查单个字符是否就超过宽度
                if (paint.measureText(String.valueOf(c)) > maxWidth) {
                    continue;
                }
                currentLine.append(c);
            } else {
                currentLine.append(c);
            }
        }

        // 绘制最后一行
        if (exceeded && overflowContent.length() > 0) {
            // 最后一行需要截断并添加省略号
            String truncated = truncateWithEllipsis(overflowContent.toString(), maxWidth, paint);
            canvas.drawText(truncated, leftMargin, currentY, paint);
        } else if (currentLine.length() > 0) {
            // 内容未超出区域，正常绘制
            canvas.drawText(currentLine.toString(), leftMargin, currentY, paint);
        }
    }

    /**
     * 截断文本并添加省略号
     */
    private String truncateWithEllipsis(String text, int maxWidth, Paint paint) {
        if (text == null || text.isEmpty()) return text;

        String ellipsis = "...";
        float ellipsisWidth = paint.measureText(ellipsis);
        float availableWidth = maxWidth - ellipsisWidth;

        // 如果可用宽度太小，直接返回省略号
        if (availableWidth <= 0) {
            return ellipsis;
        }

        // 找到可以放入的字符位置
        StringBuilder truncated = new StringBuilder();
        float currentWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            float charWidth = paint.measureText(String.valueOf(text.charAt(i)));
            if (currentWidth + charWidth > availableWidth) {
                break;
            }
            currentWidth += charWidth;
            truncated.append(text.charAt(i));
        }
        return truncated.toString() + ellipsis;
    }

    /**
     * 根据标题生成渐变色
     */
    private int[] getGradientColors(String title) {
        if (title == null || title.isEmpty()) {
            return new int[]{0xFF1976D2, 0xFF42A5F5};
        }

        int[][] gradients = {
                {0xFF667eea, 0xFF764ba2},
                {0xFFf093fb, 0xFFf5576c},
                {0xFF4facfe, 0xFF00f2fe},
                {0xFF43e97b, 0xFF38f9d7},
                {0xFFfa709a, 0xFFfee140},
                {0xFF30cfd0, 0xFF330867},
                {0xFFa8edea, 0xFFfed6e3},
                {0xFFff9a9e, 0xFFfecfef},
        };

        int index = Math.abs(title.hashCode()) % gradients.length;
        return gradients[index];
    }

    /**
     * 分享Bitmap图片
     */
    private void shareBitmap(Bitmap bitmap) {
        try {
            File cacheDir = requireContext().getCacheDir();
            File shareFile = new File(cacheDir, "share_card_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(shareFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            Uri contentUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    shareFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "分享作品：" + currentStory.getTitle());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 设置ClipData以在分享对话框中显示图片预览
            shareIntent.setClipData(android.content.ClipData.newRawUri("", contentUri));

            Intent chooserIntent = Intent.createChooser(shareIntent, "分享作品卡片");
            startActivity(chooserIntent);

            shareFile.deleteOnExit();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "分享失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 使用现有封面生成并分享卡片
     */
    private void generateAndShareWithExistingCover(String coverPath) {
        try {
            Bitmap coverBitmap = BitmapFactory.decodeFile(coverPath);
            if (coverBitmap != null) {
                generateAndShareWithBitmap(coverBitmap);
            } else {
                Toast.makeText(requireContext(), "封面加载失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "封面加载失败", Toast.LENGTH_SHORT).show();
        }
    }
}