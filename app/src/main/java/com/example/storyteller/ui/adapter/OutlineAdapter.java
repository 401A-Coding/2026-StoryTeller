package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.Chapter;
import com.example.storyteller.model.Volume;

import java.util.List;

/**
 * 大纲适配器
 * 展示卷纲和章纲列表
 */
public class OutlineAdapter extends RecyclerView.Adapter<OutlineAdapter.VolumeViewHolder> {

    private Context context;
    private List<Volume> volumes;
    private OnVolumeClickListener volumeClickListener;
    private OnChapterClickListener chapterClickListener;

    public interface OnVolumeClickListener {
        void onVolumeClick(int volumeIndex);
    }

    public interface OnChapterClickListener {
        void onChapterClick(int volumeIndex, int chapterIndex);
    }

    public OutlineAdapter(Context context, List<Volume> volumes, 
                         OnVolumeClickListener volumeListener,
                         OnChapterClickListener chapterListener) {
        this.context = context;
        this.volumes = volumes;
        this.volumeClickListener = volumeListener;
        this.chapterClickListener = chapterListener;
    }

    @NonNull
    @Override
    public VolumeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_volume_outline, parent, false);
        return new VolumeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VolumeViewHolder holder, int position) {
        Volume volume = volumes.get(position);
        holder.bind(volume, position);
    }

    @Override
    public int getItemCount() {
        return volumes != null ? volumes.size() : 0;
    }

    class VolumeViewHolder extends RecyclerView.ViewHolder {
        private TextView tvVolumeTitle;
        private ProgressBar progressBar;
        private LinearLayout layoutVolumeDetail;
        private TextView tvSummaryPreview;
        private TextView tvTargetInfo;
        private RecyclerView rvChapters;
        private ImageButton btnEditVolume;
        private ImageButton btnExpandCollapse;  // 展开/收起按钮
        private ChapterAdapter chapterAdapter;
        private boolean isExpanded = false;  // 默认收起

        public VolumeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVolumeTitle = itemView.findViewById(R.id.tv_volume_title);
            progressBar = itemView.findViewById(R.id.progress_bar);
            layoutVolumeDetail = itemView.findViewById(R.id.layout_volume_detail);
            tvSummaryPreview = itemView.findViewById(R.id.tv_summary_preview);
            tvTargetInfo = itemView.findViewById(R.id.tv_target_info);
            rvChapters = itemView.findViewById(R.id.rv_chapters);
            btnEditVolume = itemView.findViewById(R.id.btn_edit_volume);
            btnExpandCollapse = itemView.findViewById(R.id.btn_expand_collapse);
        }

        public void bind(Volume volume, int volumeIndex) {
            // 设置卷标题
            tvVolumeTitle.setText("第" + (volumeIndex + 1) + "卷：" + volume.getTitle());

            // 设置章节进度
            List<Chapter> chapters = volume.getChapters();
            int currentCount = chapters != null ? chapters.size() : 0;
            int targetCount = volume.getTargetChapterCount();
            
            if (targetCount > 0) {
                progressBar.setMax(targetCount);
                progressBar.setProgress(currentCount);
            } else {
                progressBar.setMax(1);
                progressBar.setProgress(0);
            }

            // 设置卷摘要预览
            String summary = volume.getSummary();
            if (summary != null && !summary.isEmpty()) {
                tvSummaryPreview.setText(summary);
                tvSummaryPreview.setVisibility(View.VISIBLE);
            } else {
                tvSummaryPreview.setVisibility(View.GONE);
            }

            // 设置目标信息
            StringBuilder targetInfo = new StringBuilder();
            if (volume.getTargetWordCount() > 0) {
                targetInfo.append("目标字数: ").append(volume.getTargetWordCount());
            }
            if (volume.getTargetChapterCount() > 0) {
                if (targetInfo.length() > 0) {
                    targetInfo.append(" | ");
                }
                targetInfo.append("目标章节: ").append(volume.getTargetChapterCount());
            }
            
            if (targetInfo.length() > 0) {
                tvTargetInfo.setText(targetInfo.toString());
                tvTargetInfo.setVisibility(View.VISIBLE);
            } else {
                tvTargetInfo.setVisibility(View.GONE);
            }

            // 设置章节列表（默认收起）
            if (chapters != null && !chapters.isEmpty()) {
                chapterAdapter = new ChapterAdapter(chapters, volumeIndex);
                rvChapters.setLayoutManager(new LinearLayoutManager(context));
                rvChapters.setAdapter(chapterAdapter);
                rvChapters.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            } else {
                rvChapters.setVisibility(View.GONE);
            }

            // 更新展开/收起按钮图标
            updateExpandCollapseIcon();

            // 点击展开/收起按钮
            btnExpandCollapse.setOnClickListener(v -> {
                isExpanded = !isExpanded;
                rvChapters.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                updateExpandCollapseIcon();
            });

            // 编辑按钮点击事件
            btnEditVolume.setOnClickListener(v -> {
                if (volumeClickListener != null) {
                    volumeClickListener.onVolumeClick(volumeIndex);
                }
            });
        }
        
        private void updateExpandCollapseIcon() {
            if (isExpanded) {
                btnExpandCollapse.setImageResource(R.drawable.ic_arrow_drop_down);
                btnExpandCollapse.setRotation(0);
            } else {
                btnExpandCollapse.setImageResource(R.drawable.ic_arrow_drop_down);
                btnExpandCollapse.setRotation(-90);  // 收起时箭头向右
            }
        }
    }

    /**
     * 章节内部适配器
     */
    class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
        private List<Chapter> chapters;
        private int volumeIndex;

        public ChapterAdapter(List<Chapter> chapters, int volumeIndex) {
            this.chapters = chapters;
            this.volumeIndex = volumeIndex;
        }

        @NonNull
        @Override
        public ChapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_chapter_outline, parent, false);
            return new ChapterViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChapterViewHolder holder, int position) {
            Chapter chapter = chapters.get(position);
            holder.bind(chapter, position);
        }

        @Override
        public int getItemCount() {
            return chapters != null ? chapters.size() : 0;
        }

        class ChapterViewHolder extends RecyclerView.ViewHolder {
            private TextView tvChapterIndex;
            private TextView tvChapterTitle;
            private TextView tvChapterRole;
            private TextView tvChapterSummary;
            private TextView tvSuspenseLevel;
            private TextView tvTwistLevel;

            public ChapterViewHolder(@NonNull View itemView) {
                super(itemView);
                tvChapterIndex = itemView.findViewById(R.id.tv_chapter_index);
                tvChapterTitle = itemView.findViewById(R.id.tv_chapter_title);
                tvChapterRole = itemView.findViewById(R.id.tv_chapter_role);
                tvChapterSummary = itemView.findViewById(R.id.tv_chapter_summary);
                tvSuspenseLevel = itemView.findViewById(R.id.tv_suspense_level);
                tvTwistLevel = itemView.findViewById(R.id.tv_twist_level);
            }

            public void bind(Chapter chapter, int chapterIndex) {
                // 章节序号
                tvChapterIndex.setText(String.valueOf(chapterIndex + 1));

                // 章节标题
                String title = chapter.getTitle();
                tvChapterTitle.setText(title != null && !title.isEmpty() ? title : "未命名章");

                // 章节作用（加粗蓝色）
                String role = chapter.getChapterRole();
                if (role != null && !role.isEmpty()) {
                    tvChapterRole.setText(role);
                    tvChapterRole.setVisibility(View.VISIBLE);
                } else {
                    tvChapterRole.setVisibility(View.GONE);
                }

                // 章节摘要
                String summary = chapter.getChapterSummary();
                if (summary != null && !summary.isEmpty()) {
                    tvChapterSummary.setText(summary);
                    tvChapterSummary.setVisibility(View.VISIBLE);
                } else {
                    tvChapterSummary.setVisibility(View.GONE);
                }

                // 悬念级别
                float suspense = chapter.getSuspenseLevel();
                if (suspense > 0) {
                    tvSuspenseLevel.setText("悬念: " + formatLevel(suspense));
                    tvSuspenseLevel.setVisibility(View.VISIBLE);
                } else {
                    tvSuspenseLevel.setVisibility(View.GONE);
                }

                // 转折级别
                float twist = chapter.getTwistLevel();
                if (twist > 0) {
                    tvTwistLevel.setText("转折: " + formatTwistLevel(twist));
                    tvTwistLevel.setVisibility(View.VISIBLE);
                } else {
                    tvTwistLevel.setVisibility(View.GONE);
                }

                // 点击事件
                itemView.setOnClickListener(v -> {
                    if (chapterClickListener != null) {
                        chapterClickListener.onChapterClick(volumeIndex, chapterIndex);
                    }
                });
            }
            
            private String formatLevel(float level) {
                if (level <= 3) return "低";
                if (level <= 6) return "中";
                return "高";
            }

            private String formatTwistLevel(float level) {
                if (level == 0) return "无";
                if (level <= 2) return "轻微";
                if (level <= 3) return "中等";
                return "重大";
            }
        }
    }
}
