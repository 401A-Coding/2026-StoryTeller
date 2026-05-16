package com.example.storyteller.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * 导入小说文件管理器
 * 负责管理导入小说的章节文件存储
 */
public class ImportedNovelFileManager {
    private static final String TAG = "ImportedNovelFileManager";
    private static final String IMPORTED_NOVELS_DIR = "imported_novels";

    private final Context context;

    public ImportedNovelFileManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * 为小说创建存储目录
     * @param novelId 小说ID
     * @return 目录路径
     */
    public String createNovelDirectory(int novelId) {
        File novelsDir = new File(context.getFilesDir(), IMPORTED_NOVELS_DIR);
        if (!novelsDir.exists()) {
            novelsDir.mkdirs();
        }

        File novelDir = new File(novelsDir, "novel_" + novelId);
        if (!novelDir.exists()) {
            novelDir.mkdirs();
        }

        return novelDir.getAbsolutePath();
    }

    /**
     * 为卷创建子目录
     * @param novelDir 小说根目录
     * @param volumeIndex 卷索引
     * @return 卷目录路径
     */
    public String createVolumeDirectory(String novelDir, int volumeIndex) {
        File volumeDir = new File(novelDir, "volume_" + volumeIndex);
        if (!volumeDir.exists()) {
            volumeDir.mkdirs();
        }
        return volumeDir.getAbsolutePath();
    }

    /**
     * 保存章节内容到文件
     * @param volumeDir 卷目录
     * @param chapterIndex 章节索引
     * @param content 章节内容
     * @return 文件相对路径（用于存入structureJson）
     */
    public String saveChapterContent(String volumeDir, int chapterIndex, String content) {
        String fileName = "chapter_" + chapterIndex + ".txt";
        File chapterFile = new File(volumeDir, fileName);

        try (FileWriter writer = new FileWriter(chapterFile)) {
            writer.write(content);
            writer.flush();
            Log.d(TAG, "章节保存成功: " + chapterFile.getAbsolutePath());
            return "volume_" + new File(volumeDir).getName().replace("volume_", "") 
                   + "/" + fileName;
        } catch (IOException e) {
            Log.e(TAG, "章节保存失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 读取章节内容
     * @param novelDir 小说根目录
     * @param filePath 文件相对路径（如：volume_0/chapter_0.txt）
     * @return 章节内容
     */
    public String readChapterContent(String novelDir, String filePath) {
        File chapterFile = new File(novelDir, filePath);
        if (!chapterFile.exists()) {
            Log.w(TAG, "章节文件不存在: " + filePath);
            return null;
        }

        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(chapterFile)
            );
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            return content.toString();
        } catch (IOException e) {
            Log.e(TAG, "读取章节失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 删除小说及其所有文件
     * @param novelId 小说ID
     */
    public void deleteNovel(int novelId) {
        File novelsDir = new File(context.getFilesDir(), IMPORTED_NOVELS_DIR);
        File novelDir = new File(novelsDir, "novel_" + novelId);
        
        if (novelDir.exists()) {
            deleteDirectory(novelDir);
            Log.d(TAG, "小说目录已删除: " + novelDir.getAbsolutePath());
        }
    }

    /**
     * 删除小说目录（通过路径）
     * @param novelDirPath 小说目录路径
     */
    public void deleteNovelDirectory(String novelDirPath) {
        if (novelDirPath == null || novelDirPath.isEmpty()) {
            return;
        }
        File novelDir = new File(novelDirPath);
        if (novelDir.exists()) {
            deleteDirectory(novelDir);
            Log.d(TAG, "小说目录已删除: " + novelDir.getAbsolutePath());
        }
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    /**
     * 获取小说目录路径
     */
    public String getNovelDirectory(int novelId) {
        File novelsDir = new File(context.getFilesDir(), IMPORTED_NOVELS_DIR);
        File novelDir = new File(novelsDir, "novel_" + novelId);
        return novelDir.exists() ? novelDir.getAbsolutePath() : null;
    }
}

