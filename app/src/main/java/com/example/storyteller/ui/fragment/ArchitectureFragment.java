package com.example.storyteller.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.StoryDao;
import com.example.storyteller.data.remote.ApiClient;
import com.example.storyteller.data.remote.ApiKeyManager;
import com.example.storyteller.data.remote.ModelConfig;
import com.example.storyteller.model.Story;
import com.example.storyteller.ui.adapter.CoverOptionAdapter;
import com.example.storyteller.ui.dialog.ModelProviderSettingsDialogHelper;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * 架构Fragment - 管理作品的基本信息
 */
public class ArchitectureFragment extends BaseFragment {

    private static final String ARG_STORY_ID = "arg_story_id";

    // UI组件
    private MaterialCardView cardCover;
    private ImageView ivCoverImage;
    private View vCoverBackground;
    private View layoutStatusContainer;
    private Spinner spinnerStatus;
    private ImageButton btnFavorite;
    private EditText etTitle;
    private ChipGroup chipGroupSelectedGenres;
    private ChipGroup chipGroupPresetGenres;
    private EditText etCustomGenre;
    private View btnAddGenre;
    private EditText etDescription;
    private TextView tvDescriptionCount;
    private android.widget.Button btnGenerateCover;
    private android.widget.Button btnConfirmCover;

    // 数据
    private Story currentStory;
    private int storyId;
    private StoryDao storyDao;
    private final ApiClient apiClient = ApiClient.getInstance();
    private final ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);
    
    // 标记是否正在加载数据，避免加载时触发自动保存
    private boolean isLoadingData = false;
    
    // 架构变化监听器
    private OnArchitectureChangedListener listener;
    
    // 已选标签列表
    private List<String> selectedGenres = new ArrayList<>();
    
    // 预设标签选项
    private static final String[] PRESET_GENRES = {
        "玄幻", "奇幻", "武侠", "仙侠", "都市", 
        "历史", "科幻", "悬疑", "恐怖", "言情",
        "轻小说", "同人", "游戏", "体育", "其他"
    };
    
    // 状态选项
    private static final String[] STATUS_OPTIONS = {
        "创作中", "已完成", "暂停更新"
    };
    
    // 封面图片请求码
    private static final int REQUEST_CODE_SELECT_COVER = 1001;
    
    // 装饰图形（无封面时显示）
    private View decorationView;

    public static ArchitectureFragment newInstance(int storyId) {
        ArchitectureFragment fragment = new ArchitectureFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STORY_ID, storyId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_architecture;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void initView(View view) {
        // 初始化封面相关组件
        cardCover = view.findViewById(R.id.card_cover);
        ivCoverImage = view.findViewById(R.id.iv_cover_image);
        vCoverBackground = view.findViewById(R.id.v_cover_background);
        layoutStatusContainer = view.findViewById(R.id.layout_status_container);
        spinnerStatus = view.findViewById(R.id.spinner_status);
        btnFavorite = view.findViewById(R.id.btn_favorite);
        
        // 初始化标题和描述
        etTitle = view.findViewById(R.id.et_architecture_title);
        etDescription = view.findViewById(R.id.et_architecture_description);
        tvDescriptionCount = view.findViewById(R.id.tv_description_count);
        
        // 让简介输入框支持垂直滚动
        etDescription.setVerticalScrollBarEnabled(true);
        
        // 当 EditText 获得焦点时，阻止父容器拦截触摸事件
        etDescription.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });
        
        // 初始化标签相关组件
        chipGroupSelectedGenres = view.findViewById(R.id.chip_group_selected_genres);
        chipGroupPresetGenres = view.findViewById(R.id.chip_group_preset_genres);
        etCustomGenre = view.findViewById(R.id.et_custom_genre);
        btnAddGenre = view.findViewById(R.id.btn_add_genre);
        btnGenerateCover = view.findViewById(R.id.btn_generate_cover);
        decorationView = view.findViewById(R.id.view_decoration);

        // AI生成封面按钮
        btnGenerateCover.setOnClickListener(v -> {
            if (!hasMiniMaxApiKey()) {
                ModelProviderSettingsDialogHelper.showApiKeyRequiredDialog(requireContext(), "MiniMax", "生成封面");
                return;
            }
            if (!isMiniMaxEnabled()) {
                ModelProviderSettingsDialogHelper.showProviderDisabledDialog(requireContext(), "MiniMax", "生成封面");
                return;
            }
            showCoverGenerationDialog();
        });

        // 设置状态下拉框
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
            requireContext(),
            R.layout.item_spinner_white,
            STATUS_OPTIONS
        );
        statusAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown_white);
        spinnerStatus.setAdapter(statusAdapter);
        
        // 设置预设标签
        setupPresetGenres();
        
        // 封面点击事件 - 更换封面
        cardCover.setOnClickListener(v -> selectCoverImage());
        
        // 状态标签容器点击事件 - 触发下拉
        layoutStatusContainer.setOnClickListener(v -> {
            spinnerStatus.performClick();
        });
        
        // 收藏星标点击事件
        btnFavorite.setOnClickListener(v -> toggleFavorite());
        
        // 状态变化时自动保存
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isLoadingData) {
                    autoSaveChanges();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // 添加自定义标签按钮
        btnAddGenre.setOnClickListener(v -> addCustomGenre());

        // 字数统计（不触发自动保存）
        etDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvDescriptionCount.setText(s.length() + "/500");
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // 简介变化时自动保存（加载数据时不触发）
                if (!isLoadingData) {
                    autoSaveChanges();
                }
            }
        });

        // 标题变化时自动保存（加载数据时不触发）
        etTitle.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!isLoadingData) {
                    autoSaveChanges();
                }
            }
        });
    }

    @Override
    protected void initData() {
        storyDao = new StoryDao(requireContext());

        if (getArguments() != null) {
            storyId = getArguments().getInt(ARG_STORY_ID, -1);
        }

        if (storyId > 0) {
            loadStoryData();
            
            // 加载完成后，自动设置监听器
            if (getActivity() instanceof OnArchitectureChangedListener) {
                listener = (OnArchitectureChangedListener) getActivity();
            }
        } else {
            Toast.makeText(requireContext(), "未找到作品", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 加载作品数据
     */
    private void loadStoryData() {
        // 标记正在加载，避免触发自动保存
        isLoadingData = true;
        
        currentStory = storyDao.getStoryById(storyId);
        if (currentStory == null) {
            Toast.makeText(requireContext(), "加载作品失败", Toast.LENGTH_SHORT).show();
            isLoadingData = false;
            return;
        }
        // 填充数据
        etTitle.setText(currentStory.getTitle());
        
        // 设置状态
        String category = currentStory.getCategory();
        if (!TextUtils.isEmpty(category)) {
            for (int i = 0; i < STATUS_OPTIONS.length; i++) {
                if (STATUS_OPTIONS[i].equals(category)) {
                    spinnerStatus.setSelection(i);
                    break;
                }
            }
        } else {
            spinnerStatus.setSelection(0); // 默认“创作中”
        }
        
        // 设置标签（从 JSON 解析）
        selectedGenres.clear();
        String genreJson = currentStory.getGenre();
        if (!TextUtils.isEmpty(genreJson)) {
            try {
                Gson gson = new Gson();
                List<String> genres = gson.fromJson(genreJson, new TypeToken<List<String>>(){}.getType());
                if (genres != null) {
                    selectedGenres.addAll(genres);
                }
            } catch (Exception e) {
                // 如果是旧数据（单个字符串），尝试直接添加
                selectedGenres.add(genreJson);
            }
        }
        updateSelectedGenresUI();

        // 设置简介
        String description = currentStory.getDescription();
        if (description != null) {
            etDescription.setText(description);
            tvDescriptionCount.setText(description.length() + "/500");
        } else {
            etDescription.setText("");
            tvDescriptionCount.setText("0/500");
        }
        
        // 更新封面背景
        updateCoverBackground(currentStory.getTitle());
        
        // 加载封面图片（如果有）
        loadCoverImage();
        
        // 更新收藏图标
        updateFavoriteIcon();

        // 标记加载完成
        isLoadingData = false;

        // 注意：大纲和总结目前存储在NovelSummary中，这里暂时留空
        // 后续可以扩展Story模型或创建关联表来存储这些信息
    }
    
    /**
     * 公开方法：刷新视图（用于切换小说后强制刷新）
     */
    public void refreshView() {
        if (storyId > 0) {
            loadStoryData();
        }
    }

    /**
     * 设置架构变化监听器
     */
    public void setOnArchitectureChangedListener(OnArchitectureChangedListener listener) {
        this.listener = listener;
    }

    /**
     * 自动保存修改（静默保存，不显示Toast）
     */
    private void autoSaveChanges() {
        if (currentStory == null || isLoadingData) {
            return;
        }

        // 获取当前输入的值
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString();
        String newStatus = (String) spinnerStatus.getSelectedItem();

        // 验证标题（标题不能为空）
        if (TextUtils.isEmpty(title)) {
            return;
        }

        // 验证简介长度
        if (description.length() > 500) {
            return;
        }

        // 检查是否有变化
        String currentTitle = currentStory.getTitle();
        String currentCategory = currentStory.getCategory();
        String currentDescription = currentStory.getDescription();
        
        // 将标签列表转换为JSON字符串
        Gson gson = new Gson();
        String newGenresJson = gson.toJson(selectedGenres);
        String currentGenresJson = currentStory.getGenre();

        // 如果没有任何变化，不需要保存
        boolean titleChanged = !title.equals(currentTitle);
        boolean statusChanged = !(newStatus != null && newStatus.equals(currentCategory));
        boolean genresChanged = !(newGenresJson != null && newGenresJson.equals(currentGenresJson));
        boolean descriptionChanged = !(description != null ? description.equals(currentDescription) : currentDescription == null);
        
        if (!titleChanged && !statusChanged && !genresChanged && !descriptionChanged) {
            return;
        }

        // 更新内存中的数据
        currentStory.setTitle(title);
        currentStory.setCategory(newStatus);
        currentStory.setGenre(newGenresJson); // 存储为JSON数组
        currentStory.setDescription(description);

        // 只更新架构相关字段，不覆盖字数和结构信息
        int result = storyDao.updateStoryArchitecture(
            currentStory.getId(), 
            title, 
            newStatus, 
            newGenresJson, 
            description
        );
        
        // 通知监听器数据已更新
        if (listener != null) {
            listener.onArchitectureChanged(currentStory);
        }
    }

    /**
     * 手动保存修改（显示Toast提示）
     */
    public void saveChanges() {
        saveChangesInternal(true);
    }

    /**
     * 静默保存修改（不显示Toast）
     */
    public void saveChangesSilently() {
        saveChangesInternal(false);
    }

    /**
     * 内部保存方法
     * @param showToast 是否显示Toast提示
     */
    private void saveChangesInternal(boolean showToast) {
        if (currentStory == null) {
            if (showToast) {
                Toast.makeText(requireContext(), "作品数据不存在", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 验证标题
        String title = etTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            if (showToast) {
                Toast.makeText(requireContext(), "请输入作品标题", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 获取简介（不要trim，保留原始内容）
        String description = etDescription.getText().toString();
        
        // 验证简介长度
        if (description.length() > 500) {
            if (showToast) {
                Toast.makeText(requireContext(), "简介不能超过500字", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // 更新内存中的数据
        currentStory.setTitle(title);
        String newStatus = (String) spinnerStatus.getSelectedItem();
        currentStory.setCategory(newStatus);
        
        // 将标签列表转换为JSON字符串
        Gson gson = new Gson();
        String genresJson = gson.toJson(selectedGenres);
        currentStory.setGenre(genresJson);
        
        currentStory.setDescription(description);

        // 只更新架构相关字段，不覆盖字数和结构信息
        int result = storyDao.updateStoryArchitecture(
            currentStory.getId(), 
            title, 
            newStatus, 
            genresJson, 
            description
        );
        
        if (showToast) {
            if (result > 0) {
                Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show();
                
                // 通知监听器数据已更新
                if (listener != null) {
                    listener.onArchitectureChanged(currentStory);
                }
            } else {
                Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 架构变化监听器接口
     */
    public interface OnArchitectureChangedListener {
        void onArchitectureChanged(Story story);
    }
    
    /**
     * 设置预设标签
     */
    private void setupPresetGenres() {
        chipGroupPresetGenres.removeAllViews();
        for (String genre : PRESET_GENRES) {
            Chip chip = new Chip(requireContext());
            chip.setText(genre);
            chip.setCheckable(false);
            // 使用双击事件添加标签
            final long[] lastClickTime = {0};
            chip.setOnClickListener(v -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime[0] < 300) {
                    // 双击
                    addGenre(genre);
                }
                lastClickTime[0] = currentTime;
            });
            chipGroupPresetGenres.addView(chip);
        }
    }
    
    /**
     * 添加标签
     */
    private void addGenre(String genre) {
        if (TextUtils.isEmpty(genre)) {
            return;
        }
        
        if (selectedGenres.contains(genre)) {
            Toast.makeText(requireContext(), "该标签已存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedGenres.size() >= 8) {
            Toast.makeText(requireContext(), "最多只能添加8个标签", Toast.LENGTH_SHORT).show();
            return;
        }
        
        selectedGenres.add(genre);
        updateSelectedGenresUI();
        
        if (!isLoadingData) {
            autoSaveChanges();
        }
    }
    
    /**
     * 移除标签
     */
    private void removeGenre(String genre) {
        selectedGenres.remove(genre);
        updateSelectedGenresUI();
        
        if (!isLoadingData) {
            autoSaveChanges();
        }
    }
    
    /**
     * 更新已选标签UI
     */
    private void updateSelectedGenresUI() {
        chipGroupSelectedGenres.removeAllViews();
        for (String genre : selectedGenres) {
            Chip chip = new Chip(requireContext());
            chip.setText(genre);
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> removeGenre(genre));
            chipGroupSelectedGenres.addView(chip);
        }
    }
    
    /**
     * 添加自定义标签
     */
    private void addCustomGenre() {
        String customGenre = etCustomGenre.getText().toString().trim();
        if (TextUtils.isEmpty(customGenre)) {
            Toast.makeText(requireContext(), "请输入标签名称", Toast.LENGTH_SHORT).show();
            return;
        }
        
        addGenre(customGenre);
        etCustomGenre.setText(""); // 清空输入框
    }
    
    /**
     * 加载封面图片
     */
    private void loadCoverImage() {
        if (currentStory == null) {
            return;
        }
        
        String coverPath = currentStory.getCoverPath();
        if (!TextUtils.isEmpty(coverPath)) {
            // 有封面图片，显示图片，隐藏渐变背景
            try {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(coverPath);
                if (bitmap != null) {
                    ivCoverImage.setImageBitmap(bitmap);
                    ivCoverImage.setVisibility(View.VISIBLE);
                    vCoverBackground.setVisibility(View.GONE);
                    decorationView.setVisibility(View.GONE);
                } else {
                    // 图片加载失败，显示渐变背景
                    ivCoverImage.setVisibility(View.GONE);
                    vCoverBackground.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ivCoverImage.setVisibility(View.GONE);
                vCoverBackground.setVisibility(View.VISIBLE);
                decorationView.setVisibility(View.VISIBLE);
            }
        } else {
            // 没有封面图片，显示渐变背景和装饰
            ivCoverImage.setVisibility(View.GONE);
            vCoverBackground.setVisibility(View.VISIBLE);
            decorationView.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 选择封面图片
     */
    private void selectCoverImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_SELECT_COVER);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_SELECT_COVER && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                saveCoverImage(imageUri);
            }
        }
    }
    
    /**
     * 保存封面图片
     */
    private void saveCoverImage(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                Toast.makeText(requireContext(), "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 创建文件目录
            File coverDir = new File(requireContext().getFilesDir(), "covers");
            if (!coverDir.exists()) {
                coverDir.mkdirs();
            }
            
            // 创建文件
            String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
            File coverFile = new File(coverDir, fileName);
            
            // 复制文件
            FileOutputStream outputStream = new FileOutputStream(coverFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            // 更新数据库
            String coverPath = coverFile.getAbsolutePath();
            currentStory.setCoverPath(coverPath);
            storyDao.updateStoryCoverPath(currentStory.getId(), coverPath);
            
            // 刷新UI显示新封面
            loadCoverImage();
            
            Toast.makeText(requireContext(), "封面已更新", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "保存封面失败", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 切换收藏状态
     */
    private void toggleFavorite() {
        if (currentStory == null) {
            return;
        }
        
        boolean newStatus = !currentStory.isCollected();
        currentStory.setCollected(newStatus);
        
        // 更新星标图标
        updateFavoriteIcon();
        
        // 保存到数据库
        storyDao.setCollected(currentStory.getId(), newStatus);
        
        Toast.makeText(requireContext(), 
            newStatus ? "已收藏" : "已取消收藏", 
            Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 更新收藏图标
     */
    private void updateFavoriteIcon() {
        if (currentStory != null && currentStory.isCollected()) {
            btnFavorite.setImageResource(R.drawable.ic_star_filled);
        } else {
            btnFavorite.setImageResource(R.drawable.ic_star_border);
        }
    }
    
    /**
     * 根据标题生成渐变色
     */
    private int[] getGradientColors(String title) {
        if (title == null || title.isEmpty()) {
            return new int[]{0xFF1976D2, 0xFF42A5F5};
        }
        
        // 预定义的渐变色组合
        int[][] gradients = {
            {0xFF667eea, 0xFF764ba2}, // 紫蓝渐变
            {0xFFf093fb, 0xFFf5576c}, // 粉红渐变
            {0xFF4facfe, 0xFF00f2fe}, // 蓝青渐变
            {0xFF43e97b, 0xFF38f9d7}, // 绿青渐变
            {0xFFfa709a, 0xFFfee140}, // 粉黄渐变
            {0xFF30cfd0, 0xFF330867}, // 青紫渐变
            {0xFFa8edea, 0xFFfed6e3}, // 浅蓝粉渐变
            {0xFFff9a9e, 0xFFfecfef}, // 粉色渐变
        };
        
        int index = Math.abs(title.hashCode()) % gradients.length;
        return gradients[index];
    }
    
    /**
     * 更新封面背景
     */
    private void updateCoverBackground(String title) {
        int[] colors = getGradientColors(title);
        GradientDrawable gradient = new GradientDrawable();
        gradient.setOrientation(GradientDrawable.Orientation.TL_BR);
        gradient.setColors(colors);
        gradient.setCornerRadius(12f);
        vCoverBackground.setBackground(gradient);
    }
    
    // ==================== AI生成封面功能 ====================

    private boolean hasMiniMaxApiKey() {
        String apiKey = ApiKeyManager.getApiKey(requireContext(), ModelConfig.Provider.MINIMAX);
        return !TextUtils.isEmpty(apiKey);
    }

    private boolean isMiniMaxEnabled() {
        return ModelConfig.isProviderEnabled(requireContext(), ModelConfig.Provider.MINIMAX);
    }

    /**
     * 显示封面生成对话框
     */
    private void showCoverGenerationDialog() {
        if (currentStory == null) {
            Toast.makeText(requireContext(), "请先保存作品信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 构建基础提示词
        String basePrompt = ApiClient.buildCoverPrompt(
            currentStory.getTitle(),
            currentStory.getDescription(),
            selectedGenres
        );
        
        // 使用自定义布局
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_cover_prompt, null);
        EditText input = dialogView.findViewById(R.id.et_cover_prompt);
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("AI生成封面");
        builder.setView(dialogView);
        
        builder.setPositiveButton("生成", (dialog, which) -> {
            String userPrompt = input.getText().toString().trim();
            String fullPrompt = basePrompt;
            if (!userPrompt.isEmpty()) {
                fullPrompt = basePrompt + "\n\nAdditional requirement: " + userPrompt;
            }
            generateCoverImages(fullPrompt, 4);
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    /**
     * 生成封面图片
     */
    private void generateCoverImages(String prompt, int count) {
        // 显示加载对话框（不可取消，点击屏幕无反应）
        android.app.AlertDialog.Builder loadingBuilder = new android.app.AlertDialog.Builder(requireContext());
        loadingBuilder.setTitle("正在生成封面");
        loadingBuilder.setMessage("请稍候...\n\n生成可能需要10-20秒");
        loadingBuilder.setCancelable(false);
        android.app.AlertDialog loadingDialog = loadingBuilder.create();
        loadingDialog.show();
        
        apiClient.generateCover(prompt, count, requireContext(), new ApiClient.CoverCallback() {
            @Override
            public void onSuccess(List<String> imageUrls) {
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    showCoverSelectionDialog(imageUrls);
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * 显示封面选择对话框
     */
    private void showCoverSelectionDialog(List<String> imageUrls) {
        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_cover_selection, null);
        
        // 创建适配器
        CoverOptionAdapter adapter = new CoverOptionAdapter(imageUrls, apiClient, executor, requireContext());
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("选择封面");
        builder.setView(dialogView);
        builder.setPositiveButton("确认", (dialog, which) -> {
            String selectedUrl = adapter.getSelectedUrl();
            if (selectedUrl != null) {
                saveAiGeneratedCover(selectedUrl);
            }
        });
        builder.setNegativeButton("取消", null);
        
        android.app.AlertDialog dialog = builder.create();
        
        // 加载图片
        RecyclerView rvCovers = dialogView.findViewById(R.id.rv_cover_options);
        rvCovers.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2));
        rvCovers.setAdapter(adapter);
        
        // 选中监听器
        adapter.setOnCoverSelectedListener(url -> {
            // 选中后启用确认按钮
            if (btnConfirmCover != null) {
                btnConfirmCover.setEnabled(true);
            }
        });
        
        // 对话框显示后获取按钮并初始禁用
        dialog.setOnShowListener(dialogInterface -> {
            if (btnConfirmCover == null) {
                btnConfirmCover = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            }
            btnConfirmCover.setEnabled(false);
        });
        
        dialog.show();
    }
    
    /**
     * 保存AI生成的封面
     */
    private void saveAiGeneratedCover(String imageUrl) {
        // 显示加载状态
        Toast.makeText(requireContext(), "正在保存封面...", Toast.LENGTH_SHORT).show();
        
        apiClient.downloadImageAsBitmap(imageUrl, requireContext(), executor,
            bitmap -> {
                requireActivity().runOnUiThread(() -> {
                    // 保存到本地
                    String coverPath = saveCoverBitmap(bitmap);
                    if (coverPath != null) {
                        currentStory.setCoverPath(coverPath);
                        storyDao.updateStoryCoverPath(currentStory.getId(), coverPath);
                        loadCoverImage();
                        Toast.makeText(requireContext(), "封面已保存", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "保存封面失败", Toast.LENGTH_SHORT).show();
                    }
                });
            },
            e -> {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "下载封面失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        );
    }
    
    /**
     * 保存Bitmap到本地
     */
    private String saveCoverBitmap(android.graphics.Bitmap bitmap) {
        try {
            File coverDir = new File(requireContext().getFilesDir(), "covers");
            if (!coverDir.exists()) {
                coverDir.mkdirs();
            }
            
            String fileName = "cover_" + System.currentTimeMillis() + ".jpg";
            File coverFile = new File(coverDir, fileName);
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(coverFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            
            return coverFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
