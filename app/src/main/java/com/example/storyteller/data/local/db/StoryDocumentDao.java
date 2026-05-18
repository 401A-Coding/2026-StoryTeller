package com.example.storyteller.data.local.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.storyteller.model.StoryDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * 故事文档数据访问对象
 * 负责story_document表的CRUD操作
 */
public class StoryDocumentDao {
    private final SQLiteDatabase db;

    public StoryDocumentDao(Context context) {
        DBHelper dbHelper = DBHelper.getInstance(context);
        this.db = dbHelper.getWritableDatabase();
    }

    /**
     * 插入新文档
     * @return 新文档的ID，失败返回-1
     */
    public long insertDocument(StoryDocument doc) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_DOCUMENT_STORY_ID, doc.getStoryId());
        values.put(DBHelper.COL_STORY_DOCUMENT_TITLE, doc.getTitle());
        values.put(DBHelper.COL_STORY_DOCUMENT_CONTENT, doc.getContent());
        values.put(DBHelper.COL_STORY_DOCUMENT_CATEGORY, doc.getCategory());
        values.put(DBHelper.COL_STORY_DOCUMENT_CREATE_TIME, System.currentTimeMillis());
        values.put(DBHelper.COL_STORY_DOCUMENT_UPDATE_TIME, System.currentTimeMillis());

        return db.insert(DBHelper.TABLE_STORY_DOCUMENT, null, values);
    }

    /**
     * 根据ID查询文档
     */
    public StoryDocument getDocumentById(int docId) {
        Cursor cursor = db.query(
            DBHelper.TABLE_STORY_DOCUMENT,
            null,
            DBHelper.COL_STORY_DOCUMENT_ID + "=?",
            new String[]{String.valueOf(docId)},
            null, null, null
        );

        StoryDocument doc = null;
        if (cursor != null && cursor.moveToFirst()) {
            doc = cursorToDocument(cursor);
            cursor.close();
        }
        return doc;
    }

    /**
     * 查询指定故事的所有文档
     * @param storyId 故事ID
     * @return 文档列表（按更新时间降序）
     */
    public List<StoryDocument> getDocumentsByStory(int storyId) {
        List<StoryDocument> documents = new ArrayList<>();
        Cursor cursor = db.query(
            DBHelper.TABLE_STORY_DOCUMENT,
            null,
            DBHelper.COL_STORY_DOCUMENT_STORY_ID + "=?",
            new String[]{String.valueOf(storyId)},
            null, null,
            DBHelper.COL_STORY_DOCUMENT_UPDATE_TIME + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                documents.add(cursorToDocument(cursor));
            }
            cursor.close();
        }
        return documents;
    }

    /**
     * 根据分类查询文档
     * @param storyId 故事ID
     * @param category 分类
     * @return 文档列表
     */
    public List<StoryDocument> getDocumentsByCategory(int storyId, String category) {
        List<StoryDocument> documents = new ArrayList<>();
        Cursor cursor = db.query(
            DBHelper.TABLE_STORY_DOCUMENT,
            null,
            DBHelper.COL_STORY_DOCUMENT_STORY_ID + "=? AND " + DBHelper.COL_STORY_DOCUMENT_CATEGORY + "=?",
            new String[]{String.valueOf(storyId), category},
            null, null,
            DBHelper.COL_STORY_DOCUMENT_UPDATE_TIME + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                documents.add(cursorToDocument(cursor));
            }
            cursor.close();
        }
        return documents;
    }

    /**
     * 搜索文档（标题或内容包含关键词）
     * @param storyId 故事ID
     * @param keyword 搜索关键词
     * @return 文档列表
     */
    public List<StoryDocument> searchDocuments(int storyId, String keyword) {
        List<StoryDocument> documents = new ArrayList<>();
        String selection = DBHelper.COL_STORY_DOCUMENT_STORY_ID + "=? AND (" +
                          DBHelper.COL_STORY_DOCUMENT_TITLE + " LIKE ? OR " +
                          DBHelper.COL_STORY_DOCUMENT_CONTENT + " LIKE ?)";
        String[] args = new String[]{
            String.valueOf(storyId),
            "%" + keyword + "%",
            "%" + keyword + "%"
        };

        Cursor cursor = db.query(
            DBHelper.TABLE_STORY_DOCUMENT,
            null,
            selection,
            args,
            null, null,
            DBHelper.COL_STORY_DOCUMENT_UPDATE_TIME + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                documents.add(cursorToDocument(cursor));
            }
            cursor.close();
        }
        return documents;
    }

    /**
     * 更新文档
     * @return 受影响的行数
     */
    public int updateDocument(StoryDocument doc) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COL_STORY_DOCUMENT_TITLE, doc.getTitle());
        values.put(DBHelper.COL_STORY_DOCUMENT_CONTENT, doc.getContent());
        values.put(DBHelper.COL_STORY_DOCUMENT_CATEGORY, doc.getCategory());
        values.put(DBHelper.COL_STORY_DOCUMENT_UPDATE_TIME, System.currentTimeMillis());

        return db.update(
            DBHelper.TABLE_STORY_DOCUMENT,
            values,
            DBHelper.COL_STORY_DOCUMENT_ID + "=?",
            new String[]{String.valueOf(doc.getId())}
        );
    }

    /**
     * 删除文档
     * @return 受影响的行数
     */
    public int deleteDocument(int docId) {
        return db.delete(
            DBHelper.TABLE_STORY_DOCUMENT,
            DBHelper.COL_STORY_DOCUMENT_ID + "=?",
            new String[]{String.valueOf(docId)}
        );
    }

    /**
     * 批量删除文档
     * @param docIds 文档ID列表
     * @return 受影响的行数
     */
    public int deleteDocuments(List<Integer> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return 0;
        }

        StringBuilder whereClause = new StringBuilder(DBHelper.COL_STORY_DOCUMENT_ID + " IN (");
        for (int i = 0; i < docIds.size(); i++) {
            whereClause.append("?");
            if (i < docIds.size() - 1) {
                whereClause.append(",");
            }
        }
        whereClause.append(")");

        String[] args = new String[docIds.size()];
        for (int i = 0; i < docIds.size(); i++) {
            args[i] = String.valueOf(docIds.get(i));
        }

        return db.delete(
            DBHelper.TABLE_STORY_DOCUMENT,
            whereClause.toString(),
            args
        );
    }

    /**
     * 统计故事的文档数量
     */
    public int countDocumentsByStory(int storyId) {
        Cursor cursor = db.query(
            DBHelper.TABLE_STORY_DOCUMENT,
            new String[]{"COUNT(*)"},
            DBHelper.COL_STORY_DOCUMENT_STORY_ID + "=?",
            new String[]{String.valueOf(storyId)},
            null, null, null
        );

        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * 将Cursor转换为StoryDocument对象
     */
    private StoryDocument cursorToDocument(Cursor cursor) {
        StoryDocument doc = new StoryDocument();
        doc.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_DOCUMENT_ID)));
        doc.setStoryId(cursor.getInt(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_DOCUMENT_STORY_ID)));
        doc.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_DOCUMENT_TITLE)));
        doc.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_DOCUMENT_CONTENT)));
        doc.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_DOCUMENT_CATEGORY)));
        doc.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_DOCUMENT_CREATE_TIME)));
        doc.setUpdateTime(cursor.getLong(cursor.getColumnIndexOrThrow(DBHelper.COL_STORY_DOCUMENT_UPDATE_TIME)));
        return doc;
    }
}
