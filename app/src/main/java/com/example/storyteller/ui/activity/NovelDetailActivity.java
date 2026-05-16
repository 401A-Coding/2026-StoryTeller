package com.example.storyteller.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.storyteller.R;
import com.example.storyteller.model.ImportedNovel;
import com.example.storyteller.ui.adapter.ChapterListAdapter;
import com.example.storyteller.ui.adapter.VolumeChapterAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 小说详情页面
 * 展示导入小说的完整信息和章节列表
 */
public class NovelDetailActivity extends AppCompatActivity {

    public static final String EXTRA_NOVEL_ID = "extra_novel_id";
    public static final String EXTRA_NOVEL_TITLE = "extra_novel_title";
    public static final String EXTRA_NOVEL_AUTHOR = "extra_novel_author";
    public static final String EXTRA_NOVEL_COVER_URL = "extra_novel_cover_url";
    public static final String EXTRA_NOVEL_DESCRIPTION = "extra_novel_description";
    public static final String EXTRA_NOVEL_TAGS = "extra_novel_tags";
    public static final String EXTRA_NOVEL_CHAPTERS = "extra_novel_chapters";
    public static final String EXTRA_NOVEL_STRUCTURE = "extra_novel_structure"; // 新增：卷结构JSON
    public static final String EXTRA_NOVEL_WORD_COUNT = "extra_novel_word_count";
    public static final String EXTRA_NOVEL_IMPORT_TIME = "extra_novel_import_time";

    private ImportedNovel novel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_detail);

        // 获取传入的数据
        loadNovelData();

        // 初始化视图
        initViews();

        // 显示数据
        displayNovelInfo();
    }

    /**
     * 从 Intent 中加载小说数据
     */
    private void loadNovelData() {
        novel = new ImportedNovel();
        novel.setId(getIntent().getIntExtra(EXTRA_NOVEL_ID, 0));
        novel.setTitle(getIntent().getStringExtra(EXTRA_NOVEL_TITLE));
        novel.setAuthor(getIntent().getStringExtra(EXTRA_NOVEL_AUTHOR));
        novel.setCoverUrl(getIntent().getStringExtra(EXTRA_NOVEL_COVER_URL));
        novel.setDescription(getIntent().getStringExtra(EXTRA_NOVEL_DESCRIPTION));
        novel.setTags(getIntent().getStringExtra(EXTRA_NOVEL_TAGS));
        novel.setTotalWords(getIntent().getIntExtra(EXTRA_NOVEL_WORD_COUNT, 0));
        novel.setImportTime(getIntent().getLongExtra(EXTRA_NOVEL_IMPORT_TIME, 0));
    
        // 优先从 structureJson 中解析卷结构和章节数
        String structureJson = getIntent().getStringExtra(EXTRA_NOVEL_STRUCTURE);
        if (structureJson != null && !structureJson.isEmpty()) {
            try {
                JSONArray volumesArray = new JSONArray(structureJson);
                int totalChapters = 0;
                    
                for (int i = 0; i < volumesArray.length(); i++) {
                    JSONObject volumeObj = volumesArray.getJSONObject(i);
                    JSONArray chaptersArray = volumeObj.optJSONArray("chapters");
                    if (chaptersArray != null) {
                        totalChapters += chaptersArray.length();
                    }
                }
                    
                novel.setTotalChapters(totalChapters);
                // 存储 structureJson 用于后续显示
                novel.setContentDir(structureJson);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 回退到旧方式：从 EXTRA_NOVEL_CHAPTERS 解析
            String chaptersJson = getIntent().getStringExtra(EXTRA_NOVEL_CHAPTERS);
            if (chaptersJson != null) {
                try {
                    JSONArray chaptersArray = new JSONArray(chaptersJson);
                    List<String> chapterTitles = new ArrayList<>();
                    for (int i = 0; i < chaptersArray.length(); i++) {
                        chapterTitles.add(chaptersArray.getString(i));
                    }
                    novel.setTotalChapters(chapterTitles.size());
                    // 存储章节标题（用于后续显示）
                    novel.setContentDir(chaptersJson); // 临时存储在 contentDir 字段
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        // 设置返回按钮
        findViewById(R.id.iv_cover).setOnClickListener(v -> finish());
    }

    /**
     * 显示小说信息
     */
    private void displayNovelInfo() {
        // 封面
        if (novel.getCoverUrl() != null && !novel.getCoverUrl().isEmpty()) {
            Glide.with(this)
                .load(novel.getCoverUrl())
                .placeholder(R.drawable.ic_menu_book)
                .error(R.drawable.ic_menu_book)
                .into((android.widget.ImageView) findViewById(R.id.iv_cover));
        } else {
            ((android.widget.ImageView) findViewById(R.id.iv_cover)).setImageResource(R.drawable.ic_menu_book);
        }

        // 标题
        TextView tvTitle = findViewById(R.id.tv_title);
        tvTitle.setText(novel.getTitle());

        // 作者
        TextView tvAuthor = findViewById(R.id.tv_author);
        tvAuthor.setText(novel.getAuthor() != null ? "作者：" + novel.getAuthor() : "作者未知");

        // 标签
        LinearLayout llTags = findViewById(R.id.ll_tags);
        if (novel.getTags() != null && !novel.getTags().equals("[]")) {
            try {
                JSONArray tagsArray = new JSONArray(novel.getTags());
                for (int i = 0; i < tagsArray.length(); i++) {
                    String tag = tagsArray.getString(i);
                    TextView tagView = createTagView(tag);
                    llTags.addView(tagView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 统计信息
        TextView tvChapterCount = findViewById(R.id.tv_chapter_count);
        TextView tvWordCount = findViewById(R.id.tv_word_count);
        TextView tvImportTime = findViewById(R.id.tv_import_time);

        int chapterCount = novel.getTotalChapters();
        int wordCount = novel.getTotalWords();

        if (chapterCount == 0) {
            tvChapterCount.setText("暂无章节");
            tvWordCount.setText("0 字");
        } else {
            tvChapterCount.setText(chapterCount + " 章");
            tvWordCount.setText(formatWordCount(wordCount));
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        tvImportTime.setText("导入于 " + dateFormat.format(new Date(novel.getImportTime())));

        // 简介
        TextView tvDescription = findViewById(R.id.tv_description);
        if (novel.getDescription() != null && !novel.getDescription().isEmpty()) {
            tvDescription.setText(novel.getDescription());
        } else {
            tvDescription.setText("暂无简介");
        }

        // 章节列表
        setupChapterList();

        // 开始仿写按钮
        findViewById(R.id.btn_start_imitation).setOnClickListener(v -> {
            Toast.makeText(this, "仿写功能开发中...", Toast.LENGTH_SHORT).show();
            // TODO: 跳转到仿写页面
        });
    }

    /**
     * 设置章节列表（支持卷结构）
     */
    private void setupChapterList() {
        RecyclerView rvChapters = findViewById(R.id.rv_chapters);
        rvChapters.setLayoutManager(new LinearLayoutManager(this));

        // 尝试从structureJson中解析卷结构
        String structureJson = getIntent().getStringExtra(EXTRA_NOVEL_STRUCTURE);
        
        if (structureJson != null && !structureJson.isEmpty()) {
            // 使用卷结构适配器
            try {
                List<VolumeChapterAdapter.VolumeData> volumes = parseVolumesFromJson(structureJson);
                VolumeChapterAdapter adapter = new VolumeChapterAdapter(volumes, null);
                rvChapters.setAdapter(adapter);
            } catch (Exception e) {
                e.printStackTrace();
                // 如果解析失败，回退到简单列表
                setupSimpleChapterList(rvChapters);
            }
        } else {
            // 没有卷结构，使用简单列表
            setupSimpleChapterList(rvChapters);
        }
    }

    /**
     * 从JSON解析卷结构
     */
    private List<VolumeChapterAdapter.VolumeData> parseVolumesFromJson(String structureJson) throws Exception {
        List<VolumeChapterAdapter.VolumeData> volumes = new ArrayList<>();
        JSONArray volumesArray = new JSONArray(structureJson);
        
        for (int i = 0; i < volumesArray.length(); i++) {
            JSONObject volumeObj = volumesArray.getJSONObject(i);
            String volumeTitle = volumeObj.optString("volumeTitle", "第" + (i + 1) + "卷");
            
            List<String> chapters = new ArrayList<>();
            JSONArray chaptersArray = volumeObj.optJSONArray("chapters");
            if (chaptersArray != null) {
                for (int j = 0; j < chaptersArray.length(); j++) {
                    JSONObject chapterObj = chaptersArray.getJSONObject(j);
                    String chapterTitle = chapterObj.optString("chapterTitle", "");
                    if (!chapterTitle.isEmpty()) {
                        chapters.add(chapterTitle);
                    }
                }
            }
            
            volumes.add(new VolumeChapterAdapter.VolumeData(volumeTitle, chapters));
        }
        
        return volumes;
    }

    /**
     * 设置简单章节列表（回退方案）
     */
    private void setupSimpleChapterList(RecyclerView rvChapters) {
        // 解析章节标题列表
        List<String> chapterTitles = new ArrayList<>();
        try {
            String chaptersJson = novel.getContentDir(); // 临时存储在这里
            if (chaptersJson != null) {
                JSONArray chaptersArray = new JSONArray(chaptersJson);
                for (int i = 0; i < chaptersArray.length(); i++) {
                    chapterTitles.add(chaptersArray.getString(i));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        ChapterListAdapter adapter = new ChapterListAdapter(chapterTitles);
        rvChapters.setAdapter(adapter);
    }

    /**
     * 创建标签视图
     */
    private TextView createTagView(String tag) {
        TextView tagView = new TextView(this);
        tagView.setText(tag);
        tagView.setTextSize(12);
        tagView.setTextColor(getResources().getColor(android.R.color.holo_blue_dark, null));
        tagView.setPadding(16, 6, 16, 6);
        tagView.setBackgroundResource(R.drawable.bg_tag_chip);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.rightMargin = 8;
        tagView.setLayoutParams(params);

        return tagView;
    }

    /**
     * 格式化字数
     */
    private String formatWordCount(int wordCount) {
        if (wordCount <= 0) {
            return "0 字";
        } else if (wordCount < 10000) {
            return wordCount + " 字";
        } else if (wordCount < 100000) {
            return String.format(Locale.getDefault(), "%.1f 万字", wordCount / 10000.0);
        } else {
            return String.format(Locale.getDefault(), "%d 万字", wordCount / 10000);
        }
    }
}
