package com.example.storyteller.ui.fragment;

import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.base.BaseFragment;
import com.example.storyteller.data.local.db.ImportedNovelDao;
import com.example.storyteller.data.remote.NovelCrawler;
import com.example.storyteller.model.ImportedNovel;
import com.example.storyteller.model.NovelSummary;
import com.example.storyteller.ui.adapter.ImportedNovelAdapter;
import com.example.storyteller.utils.ImportedNovelFileManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 参考书库Fragment
 * 显示导入的小说列表，支持URL爬取导入
 */
public class ReferenceLibraryFragment extends BaseFragment {

    private ImportedNovelDao novelDao;
    private ImportedNovelAdapter adapter;
    private RecyclerView rvNovelList;
    private TextView tvEmptyHint;
    private NovelCrawler novelCrawler;
    private ImportedNovelFileManager fileManager;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_reference_library;
    }

    @Override
    protected void initView(View view) {
        rvNovelList = view.findViewById(R.id.rv_novel_list);
        tvEmptyHint = view.findViewById(R.id.tv_empty_hint);
        Button btnImport = view.findViewById(R.id.btn_import);

        // 设置RecyclerView
        rvNovelList.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ImportedNovelAdapter();
        adapter.setListener(new ImportedNovelAdapter.Listener() {
            @Override
            public void onNovelClick(@NonNull ImportedNovel novel) {
                // 跳转到小说详情页
                openNovelDetail(novel);
            }
            
            @Override
            public void onNovelLongClick(@NonNull ImportedNovel novel) {
                showDeleteDialog(novel);
            }
        });
        rvNovelList.setAdapter(adapter);

        // 导入按钮
        btnImport.setOnClickListener(v -> showImportUrlDialog());
    }

    @Override
    protected void initData() {
        novelDao = new ImportedNovelDao(requireContext());
        novelCrawler = new NovelCrawler();
        fileManager = new ImportedNovelFileManager(requireContext());
        loadNovelList();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次返回页面时刷新列表
        if (novelDao != null) {
            loadNovelList();
        }
    }

    /**
     * 加载小说列表
     */
    private void loadNovelList() {
        List<ImportedNovel> novels = novelDao.getAll();
        
        if (novels.isEmpty()) {
            // 显示空状态
            tvEmptyHint.setVisibility(View.VISIBLE);
            rvNovelList.setVisibility(View.GONE);
        } else {
            // 显示列表
            tvEmptyHint.setVisibility(View.GONE);
            rvNovelList.setVisibility(View.VISIBLE);
            adapter.setData(novels);
        }
    }

    /**
     * 显示导入URL对话框
     */
    private void showImportUrlDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("请输入小说章节页URL（如番茄小说网）");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        input.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("导入小说")
                .setMessage("目前支持爬取番茄小说网\n\n示例URL：https://fanqienovel.com/page/xxxxxx")
                .setView(input)
                .setPositiveButton("开始导入", (dialog, which) -> {
                    String url = input.getText() == null ? "" : input.getText().toString().trim();
                    if (url.isEmpty()) {
                        Toast.makeText(requireContext(), "请输入URL", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 检查是否已导入
                    ImportedNovel existing = novelDao.getByUrl(url);
                    if (existing != null) {
                        Toast.makeText(requireContext(), "该小说已导入", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // 开始爬取
                    startCrawling(url);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 开始爬取小说
     */
    private void startCrawling(String url) {
        Toast.makeText(requireContext(), "正在爬取小说...", Toast.LENGTH_SHORT).show();
        
        novelCrawler.crawlNovelDetail(url, new NovelCrawler.CrawlCallback() {
            @Override
            public void onSuccess(NovelSummary summary, int savedCount) {
                requireActivity().runOnUiThread(() -> {
                    // 爬取成功，保存小说信息
                    saveImportedNovel(summary, url);
                });
            }

            @Override
            public void onFailure(Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "爬取失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 保存导入的小说
     */
    private void saveImportedNovel(NovelSummary summary, String url) {
        // 1. 先插入数据库获取ID
        ImportedNovel novel = new ImportedNovel(
            summary.getTitle(),
            summary.getAuthor(),
            summary.getSourceUrl()
        );
        novel.setDescription(summary.getDescription());
        
        long id = novelDao.insert(novel);
        if (id <= 0) {
            Toast.makeText(requireContext(), "保存失败", Toast.LENGTH_SHORT).show();
            return;
        }
        
        novel.setId((int) id);
        
        // 2. 创建文件目录
        String contentDir = fileManager.createNovelDirectory((int) id);
        novel.setContentDir(contentDir);
        
        // 3. 生成章节结构（使用真实章节标题）
        String structureJson = generateTestStructure(summary);
        novel.setStructureJson(structureJson);
        
        // 4. 使用真实的章节数和字数
        List<String> chapterTitles = summary.getChapterTitles();
        int totalChapters = chapterTitles != null ? chapterTitles.size() : 0;
        int totalWords = summary.getTotalWords();
        
        novel.setTotalChapters(totalChapters);
        novel.setTotalWords(totalWords);
        
        // 5. 保存封面URL
        novel.setCoverUrl(summary.getCoverUrl());
        
        // 6. 保存标签（JSON格式）
        try {
            List<String> tags = summary.getTags();
            if (tags != null && !tags.isEmpty()) {
                org.json.JSONArray tagsArray = new org.json.JSONArray();
                for (String tag : tags) {
                    tagsArray.put(tag);
                }
                novel.setTags(tagsArray.toString());
            } else {
                novel.setTags("[]");
            }
        } catch (Exception e) {
            e.printStackTrace();
            novel.setTags("[]");
        }
        
        // 7. 更新数据库
        novelDao.update(novel);
        
        // 8. 爬取章节内容（新增）
        crawlAndSaveChapterContents(novel, summary);
        
        Toast.makeText(requireContext(), "导入成功！", Toast.LENGTH_SHORT).show();
        loadNovelList();
    }

    /**
     * 生成章节结构（支持多卷，使用真实章节标题）
     */
    private String generateTestStructure(NovelSummary summary) {
        try {
            JSONArray volumes = new JSONArray();
            
            // 获取卷信息列表
            List<String> volumeInfos = summary.getVolumes();
            List<String> chapterTitles = summary.getChapterTitles();
            
            android.util.Log.d("ReferenceLibrary", "卷数量: " + (volumeInfos != null ? volumeInfos.size() : 0));
            android.util.Log.d("ReferenceLibrary", "章节总数: " + (chapterTitles != null ? chapterTitles.size() : 0));
            
            if (volumeInfos != null && !volumeInfos.isEmpty()) {
                // 有多卷结构，按卷分组
                int chapterStartIndex = 0;
                
                for (int v = 0; v < volumeInfos.size(); v++) {
                    String volumeInfo = volumeInfos.get(v);
                    android.util.Log.d("ReferenceLibrary", "卷" + v + "信息: " + volumeInfo);
                    
                    JSONObject volumeObj = new JSONObject();
                    volumeObj.put("volumeIndex", v);
                    String volumeTitle = extractVolumeTitle(volumeInfo);
                    volumeObj.put("volumeTitle", volumeTitle);
                    
                    // 提取该卷的章节数
                    int chapterCount = extractChapterCount(volumeInfo);
                    android.util.Log.d("ReferenceLibrary", "卷" + v + " '" + volumeTitle + "' 应有章节数: " + chapterCount);
                    
                    JSONArray chapters = new JSONArray();
                    
                    // 添加该卷的章节
                    int totalChapters = chapterTitles != null ? chapterTitles.size() : 0;
                    int endIndex = Math.min(chapterStartIndex + chapterCount, totalChapters);
                    
                    android.util.Log.d("ReferenceLibrary", "卷" + v + " 章节范围: " + chapterStartIndex + " - " + endIndex);
                    
                    for (int i = chapterStartIndex; i < endIndex; i++) {
                        JSONObject chapter = new JSONObject();
                        chapter.put("chapterIndex", i - chapterStartIndex);
                        chapter.put("chapterTitle", chapterTitles.get(i));
                        chapter.put("wordCount", 3000);
                        chapter.put("filePath", "volume_" + v + "/chapter_" + (i - chapterStartIndex) + ".txt");
                        chapters.put(chapter);
                    }
                    
                    android.util.Log.d("ReferenceLibrary", "卷" + v + " 实际章节数: " + chapters.length());
                    
                    volumeObj.put("chapters", chapters);
                    volumes.put(volumeObj);
                    
                    chapterStartIndex = endIndex;
                }
            } else {
                // 没有卷信息，创建默认单卷
                JSONObject volume1 = new JSONObject();
                volume1.put("volumeIndex", 0);
                volume1.put("volumeTitle", "第一卷");
                
                JSONArray chapters = new JSONArray();
                
                if (chapterTitles != null && !chapterTitles.isEmpty()) {
                    for (int i = 0; i < chapterTitles.size(); i++) {
                        JSONObject chapter = new JSONObject();
                        chapter.put("chapterIndex", i);
                        chapter.put("chapterTitle", chapterTitles.get(i));
                        chapter.put("wordCount", 3000);
                        chapter.put("filePath", "volume_0/chapter_" + i + ".txt");
                        chapters.put(chapter);
                    }
                } else {
                    // 如果没有章节列表，生成10章默认数据
                    for (int i = 0; i < 10; i++) {
                        JSONObject chapter = new JSONObject();
                        chapter.put("chapterIndex", i);
                        chapter.put("chapterTitle", "第" + (i + 1) + "章");
                        chapter.put("wordCount", 3000);
                        chapter.put("filePath", "volume_0/chapter_" + i + ".txt");
                        chapters.put(chapter);
                    }
                }
                
                volume1.put("chapters", chapters);
                volumes.put(volume1);
            }
            
            String result = volumes.toString();
            android.util.Log.d("ReferenceLibrary", "生成的 structureJson: " + result);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }
    
    /**
     * 从卷信息字符串中提取卷标题
     * 例如："第一卷 共1082章" -> "第一卷"
     *      "第二卷：《我怎么成了白月光？！》 共36章" -> "第二卷：《我怎么成了白月光？！》"
     */
    private String extractVolumeTitle(String volumeInfo) {
        if (volumeInfo == null || volumeInfo.isEmpty()) {
            return "未知卷";
        }
        
        // 尝试去除末尾的章节数信息
        // 匹配模式："共XXX章" 或 "共XXX章"
        int coIndex = volumeInfo.lastIndexOf("共");
        if (coIndex > 0) {
            return volumeInfo.substring(0, coIndex).trim();
        }
        
        return volumeInfo.trim();
    }
    
    /**
     * 从卷信息字符串中提取章节数
     * 例如："第一卷 共1082章" -> 1082
     *      "第三卷：共创《黑月光冠冕-续》共16章" -> 16
     */
    private int extractChapterCount(String volumeInfo) {
        if (volumeInfo == null || volumeInfo.isEmpty()) {
            return 0;
        }
        
        try {
            // 使用正则表达式提取"共XXX章"中的数字
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("共(\\d+)章");
            java.util.regex.Matcher matcher = pattern.matcher(volumeInfo);
            
            if (matcher.find()) {
                String countStr = matcher.group(1);
                int count = Integer.parseInt(countStr);
                android.util.Log.d("ReferenceLibrary", "提取章节数成功: " + count + " from: " + volumeInfo);
                return count;
            } else {
                android.util.Log.w("ReferenceLibrary", "未找到章节数模式 in: " + volumeInfo);
            }
        } catch (Exception e) {
            android.util.Log.e("ReferenceLibrary", "提取章节数失败: " + volumeInfo, e);
            e.printStackTrace();
        }
        
        return 0;
    }

    /**
     * 爬取并保存章节内容（新增方法）
     */
    private void crawlAndSaveChapterContents(ImportedNovel novel, NovelSummary summary) {
        android.util.Log.d("ReferenceLibrary", "========== 开始爬取章节内容 ==========");
        android.util.Log.d("ReferenceLibrary", "小说ID: " + novel.getId());
        android.util.Log.d("ReferenceLibrary", "小说标题: " + novel.getTitle());
        
        // TODO: 实现章节内容爬取功能
        // 当前方案：暂时生成测试内容
        
        android.util.Log.d("ReferenceLibrary", "⚠️ 章节内容爬取功能待实现");
        
        // 暂时生成测试内容
        generateTestChapterFiles(novel.getContentDir(), novel.getStructureJson());
        
        android.util.Log.d("ReferenceLibrary", "✅ 测试章节文件生成完成\n");
    }

    /**
     * 生成测试章节文件
     */
    private void generateTestChapterFiles(String contentDir, String structureJson) {
        try {
            JSONArray volumes = new JSONArray(structureJson);
            
            for (int v = 0; v < volumes.length(); v++) {
                JSONObject volume = volumes.getJSONObject(v);
                int volumeIndex = volume.getInt("volumeIndex");
                
                // 创建卷目录
                String volumeDir = fileManager.createVolumeDirectory(contentDir, volumeIndex);
                
                JSONArray chapters = volume.getJSONArray("chapters");
                for (int c = 0; c < chapters.length(); c++) {
                    JSONObject chapter = chapters.getJSONObject(c);
                    int chapterIndex = chapter.getInt("chapterIndex");
                    String chapterTitle = chapter.getString("chapterTitle");
                    
                    // 生成测试内容
                    String content = generateTestChapterContent(chapterTitle);
                    
                    // 保存文件
                    fileManager.saveChapterContent(volumeDir, chapterIndex, content);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成测试章节内容
     */
    private String generateTestChapterContent(String chapterTitle) {
        StringBuilder sb = new StringBuilder();
        sb.append(chapterTitle).append("\n\n");
        sb.append("这是测试章节内容。\n\n");
        sb.append("在实际应用中，这里会显示从小说网站爬取的真实内容。\n\n");
        sb.append("目前功能正在开发中，后续将实现：\n");
        sb.append("1. 爬取章节正文内容\n");
        sb.append("2. 解析HTML为纯文本\n");
        sb.append("3. 保存到文件系统\n");
        sb.append("4. 支持离线阅读\n\n");
        
        // 生成一些重复内容模拟真实章节
        for (int i = 0; i < 50; i++) {
            sb.append("第").append(i + 1).append("行测试文本，用于模拟真实章节的长度和内容。\n");
        }
        
        return sb.toString();
    }

    /**
     * 打开小说详情页
     */
    private void openNovelDetail(ImportedNovel novel) {
        android.content.Intent intent = new android.content.Intent(requireContext(), com.example.storyteller.ui.activity.NovelDetailActivity.class);
        intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_ID, novel.getId());
        intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_TITLE, novel.getTitle());
        intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_AUTHOR, novel.getAuthor());
        intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_COVER_URL, novel.getCoverUrl());
        intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_DESCRIPTION, novel.getDescription());
        intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_TAGS, novel.getTags());
        intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_WORD_COUNT, novel.getTotalWords());
        intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_IMPORT_TIME, novel.getImportTime());
        intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_SOURCE_URL, novel.getSourceUrl());
        
        // 传递完整的卷结构（structureJson）
        if (novel.getStructureJson() != null) {
            intent.putExtra(com.example.storyteller.ui.activity.NovelDetailActivity.EXTRA_NOVEL_STRUCTURE, novel.getStructureJson());
        }
        
        startActivity(intent);
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteDialog(ImportedNovel novel) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除小说")
                .setMessage("确定要删除《" + novel.getTitle() + "》吗？\n\n注意：这将同时删除所有章节文件，此操作不可恢复。")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteNovel(novel);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * 删除小说
     */
    private void deleteNovel(ImportedNovel novel) {
        // 1. 删除文件系统内容
        if (novel.getContentDir() != null) {
            fileManager.deleteNovelDirectory(novel.getContentDir());
        }
        
        // 2. 删除数据库记录
        int deleted = novelDao.delete(novel.getId());
        if (deleted > 0) {
            Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show();
            loadNovelList();
        } else {
            Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 从URL提取标签（JSON格式）
     */
    private String extractTagsFromUrl(String url) {
        try {
            // 使用Jsoup获取页面
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(url)
                .timeout(10000)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get();
            
            // 提取标签
            org.jsoup.select.Elements labelElements = doc.select("div.info-label span");
            org.json.JSONArray tagsArray = new org.json.JSONArray();
            
            for (org.jsoup.nodes.Element element : labelElements) {
                String tag = element.text().trim();
                if (!tag.isEmpty()) {
                    tagsArray.put(tag);
                }
            }
            
            return tagsArray.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }
}
