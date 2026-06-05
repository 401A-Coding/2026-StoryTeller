package com.example.storyteller.ui.adapter;

import com.example.storyteller.model.SeriesGroup;
import com.example.storyteller.model.Story;

/**
 * 书架列表项包装类，统一管理独立书籍和系列文件夹两种类型
 */
public class BookshelfDisplayItem {

    public static final int TYPE_STORY = 0;
    public static final int TYPE_SERIES_FOLDER = 1;

    public final int type;
    public final Story story;
    public final SeriesGroup seriesGroup;

    private BookshelfDisplayItem(int type, Story story, SeriesGroup seriesGroup) {
        this.type = type;
        this.story = story;
        this.seriesGroup = seriesGroup;
    }

    public static BookshelfDisplayItem story(Story story) {
        return new BookshelfDisplayItem(TYPE_STORY, story, null);
    }

    public static BookshelfDisplayItem seriesFolder(SeriesGroup group) {
        return new BookshelfDisplayItem(TYPE_SERIES_FOLDER, null, group);
    }
}
