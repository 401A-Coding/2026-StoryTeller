package com.example.storyteller.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 卷章节列表适配器
 * 支持显示多卷结构，每卷可展开/收起
 */
public class VolumeChapterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_VOLUME_HEADER = 0;
    private static final int TYPE_CHAPTER = 1;

    private List<VolumeItem> items;
    private List<VolumeData> volumes; // 保存原始数据引用
    private OnChapterClickListener listener;

    public interface OnChapterClickListener {
        void onChapterClick(int volumeIndex, int chapterIndex, String chapterTitle);
    }

    /**
     * 卷数据项
     */
    public static class VolumeData {
        public String volumeTitle;
        public List<String> chapters;
        public boolean expanded;

        public VolumeData(String title, List<String> chapters) {
            this.volumeTitle = title;
            this.chapters = chapters != null ? chapters : new ArrayList<>();
            this.expanded = true; // 默认展开
        }
    }

    /**
     * 列表项（统一类型）
     */
    public static class VolumeItem {
        public int type; // TYPE_VOLUME_HEADER or TYPE_CHAPTER
        public int volumeIndex;
        public int chapterIndex;
        public String volumeTitle;
        public String chapterTitle;
        public int chapterCount;

        public static VolumeItem createVolumeHeader(int volumeIndex, String title, int chapterCount) {
            VolumeItem item = new VolumeItem();
            item.type = TYPE_VOLUME_HEADER;
            item.volumeIndex = volumeIndex;
            item.volumeTitle = title;
            item.chapterCount = chapterCount;
            return item;
        }

        public static VolumeItem createChapter(int volumeIndex, int chapterIndex, String title) {
            VolumeItem item = new VolumeItem();
            item.type = TYPE_CHAPTER;
            item.volumeIndex = volumeIndex;
            item.chapterIndex = chapterIndex;
            item.chapterTitle = title;
            return item;
        }
    }

    public VolumeChapterAdapter(List<VolumeData> volumes, OnChapterClickListener listener) {
        this.volumes = volumes;
        this.listener = listener;
        this.items = flattenVolumes(volumes);
    }

    /**
     * 将卷结构扁平化为列表项
     */
    private List<VolumeItem> flattenVolumes(List<VolumeData> volumes) {
        List<VolumeItem> flatList = new ArrayList<>();
        
        if (volumes == null || volumes.isEmpty()) {
            return flatList;
        }

        for (int i = 0; i < volumes.size(); i++) {
            VolumeData volume = volumes.get(i);
            
            // 添加卷标题
            flatList.add(VolumeItem.createVolumeHeader(i, volume.volumeTitle, volume.chapters.size()));
            
            // 如果卷是展开的，添加所有章节
            if (volume.expanded) {
                for (int j = 0; j < volume.chapters.size(); j++) {
                    flatList.add(VolumeItem.createChapter(i, j, volume.chapters.get(j)));
                }
            }
        }

        return flatList;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_VOLUME_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_volume_header, parent, false);
            return new VolumeHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chapter_simple, parent, false);
            return new ChapterViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        VolumeItem item = items.get(position);

        if (holder instanceof VolumeHeaderViewHolder) {
            ((VolumeHeaderViewHolder) holder).bind(item, this);
        } else if (holder instanceof ChapterViewHolder) {
            ((ChapterViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 切换卷的展开/收起状态
     */
    public void toggleVolume(int volumeIndex) {
        if (volumeIndex < 0 || volumes == null || volumeIndex >= volumes.size()) {
            return;
        }

        // 切换展开状态
        volumes.get(volumeIndex).expanded = !volumes.get(volumeIndex).expanded;
        
        // 重新构建列表
        this.items = flattenVolumes(volumes);
        notifyDataSetChanged();
    }

    static class VolumeHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvExpandIcon;
        TextView tvVolumeTitle;
        TextView tvChapterCount;
        VolumeChapterAdapter adapter; // 持有适配器引用

        VolumeHeaderViewHolder(View itemView) {
            super(itemView);
            tvExpandIcon = itemView.findViewById(R.id.tv_expand_icon);
            tvVolumeTitle = itemView.findViewById(R.id.tv_volume_title);
            tvChapterCount = itemView.findViewById(R.id.tv_chapter_count);
        }

        void bind(VolumeItem item, VolumeChapterAdapter adapter) {
            this.adapter = adapter;
            tvVolumeTitle.setText(item.volumeTitle);
            tvChapterCount.setText("共" + item.chapterCount + "章");
            
            // 根据展开状态设置图标
            if (adapter.volumes != null && item.volumeIndex < adapter.volumes.size()) {
                boolean expanded = adapter.volumes.get(item.volumeIndex).expanded;
                tvExpandIcon.setText(expanded ? "▼" : "▶");
            } else {
                tvExpandIcon.setText("▼"); // 默认展开
            }
            
            // 点击切换展开/收起
            itemView.setOnClickListener(v -> {
                if (getAdapterPosition() != RecyclerView.NO_POSITION && adapter != null) {
                    adapter.toggleVolume(item.volumeIndex);
                }
            });
        }
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView tvChapterTitle;

        ChapterViewHolder(View itemView) {
            super(itemView);
            tvChapterTitle = itemView.findViewById(R.id.tv_chapter_title);
        }

        void bind(VolumeItem item) {
            tvChapterTitle.setText(item.chapterTitle);
            
            itemView.setOnClickListener(v -> {
                // 点击章节的处理逻辑（如果需要）
            });
        }
    }
}
