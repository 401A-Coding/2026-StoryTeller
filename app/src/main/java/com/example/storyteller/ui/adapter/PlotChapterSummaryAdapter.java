package com.example.storyteller.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.PlotChapterSummary;
import com.example.storyteller.model.PlotOverviewSummary;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlotChapterSummaryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_OVERVIEW = 0;
    private static final int TYPE_CHAPTER = 1;

    private PlotOverviewSummary overview;
    private List<PlotChapterSummary> items;
    private String detailLevel = "standard";
    private Listener listener;
    private final Set<String> expandedChapterKeys = new HashSet<>();

    public interface Listener {
        void onEditChapter(@NonNull PlotChapterSummary summary, int position);
    }

    public PlotChapterSummaryAdapter(List<PlotChapterSummary> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setData(PlotOverviewSummary overview, List<PlotChapterSummary> items) {
        setData(overview, items, this.detailLevel);
    }

    public void setData(PlotOverviewSummary overview, List<PlotChapterSummary> items, String detailLevel) {
        this.overview = overview;
        this.items = items == null ? new ArrayList<>() : items;
        this.detailLevel = TextUtils.isEmpty(detailLevel) ? "standard" : detailLevel;
        retainExpandedState(this.items);
        notifyDataSetChanged();
    }

    public void setData(List<PlotChapterSummary> items) {
        setData(this.overview, items, this.detailLevel);
    }

    public void setData(List<PlotChapterSummary> items, String detailLevel) {
        setData(this.overview, items, detailLevel);
    }

    @Override
    public int getItemViewType(int position) {
        if (overview != null && position == 0) {
            return TYPE_OVERVIEW;
        }
        return TYPE_CHAPTER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_OVERVIEW) {
            return new OverviewViewHolder(inflater.inflate(R.layout.item_plot_overview_summary, parent, false));
        }
        return new ChapterViewHolder(inflater.inflate(R.layout.item_plot_chapter_summary, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof OverviewViewHolder) {
            bindOverview((OverviewViewHolder) holder, overview);
            return;
        }

        ChapterViewHolder chapterHolder = (ChapterViewHolder) holder;
        PlotChapterSummary item = items.get(getChapterIndex(position));
        boolean compactMode = "brief".equals(detailLevel);
        String chapterKey = buildChapterKey(item);
        boolean expanded = expandedChapterKeys.contains(chapterKey);
        chapterHolder.tvChapterLabel.setText(item.getChapterLabel());
        bindSourceChip(chapterHolder.chipChapterSource, item);
        chapterHolder.tvChapterTitle.setText(item.getChapterTitle());
        bindSection(chapterHolder.tvBriefSummary, item.getBriefSummary());
        bindSection(chapterHolder.tvKeyEvents, buildListSection("关键事件：\n", item.getKeyEvents(), true));
        bindSection(chapterHolder.tvCharacters, buildListSection("出场人物：", item.getCharacters(), false));
        if (compactMode) {
            bindSection(chapterHolder.tvDetailSummary, "");
            bindSection(chapterHolder.tvConflict, "");
            bindSection(chapterHolder.tvStoryFunction, "");
        } else {
            bindSection(chapterHolder.tvDetailSummary, buildTextSection("章节详述：\n", item.getDetailSummary()));
            bindSection(chapterHolder.tvConflict, buildTextSection("核心冲突：", item.getConflict()));
            bindSection(chapterHolder.tvStoryFunction, buildTextSection("章节作用：", item.getStoryFunction()));
        }
        chapterHolder.layoutDetailContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        chapterHolder.tvExpandToggle.setText(expanded ? R.string.plot_collapse : R.string.plot_expand);

        chapterHolder.btnEditChapter.setOnClickListener(v -> {
            if (listener == null) {
                return;
            }
            int adapterPosition = chapterHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            int chapterIndex = getChapterIndex(adapterPosition);
            if (chapterIndex < 0 || chapterIndex >= items.size()) {
                return;
            }
            listener.onEditChapter(items.get(chapterIndex), chapterIndex);
        });

        chapterHolder.itemView.setOnClickListener(v -> {
            int adapterPosition = chapterHolder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }
            int chapterIndex = getChapterIndex(adapterPosition);
            if (chapterIndex < 0 || chapterIndex >= items.size()) {
                return;
            }
            toggleExpanded(items.get(chapterIndex));
            notifyItemChanged(adapterPosition);
        });
    }

    private void retainExpandedState(List<PlotChapterSummary> summaries) {
        Set<String> validKeys = new HashSet<>();
        if (summaries != null) {
            for (PlotChapterSummary summary : summaries) {
                validKeys.add(buildChapterKey(summary));
            }
        }
        expandedChapterKeys.retainAll(validKeys);
    }

    private void toggleExpanded(PlotChapterSummary summary) {
        String key = buildChapterKey(summary);
        if (expandedChapterKeys.contains(key)) {
            expandedChapterKeys.remove(key);
        } else {
            expandedChapterKeys.add(key);
        }
    }

    private String buildChapterKey(PlotChapterSummary summary) {
        if (summary == null) {
            return "";
        }
        String label = TextUtils.isEmpty(summary.getChapterLabel()) ? "" : summary.getChapterLabel();
        String title = TextUtils.isEmpty(summary.getChapterTitle()) ? "" : summary.getChapterTitle();
        return label + "#" + title;
    }

    private void bindOverview(OverviewViewHolder holder, PlotOverviewSummary overview) {
        if (overview == null) {
            holder.tvOverviewContent.setText("");
            return;
        }
        boolean compactMode = "brief".equals(detailLevel);
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(overview.getOverallSummary())) {
            builder.append(overview.getOverallSummary());
        }
        appendSection(builder, "主线推进", overview.getMainLine());
        if (!compactMode) {
            appendSection(builder, "关键转折", overview.getTurningPoints());
            appendSection(builder, "人物线索", overview.getCharacterThreads());
        }
        if (!compactMode && !TextUtils.isEmpty(overview.getRhythm())) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("节奏观察：").append(overview.getRhythm());
        }
        holder.tvOverviewContent.setText(builder.toString());
    }

    private void appendSection(StringBuilder builder, String title, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(title).append("：");
        for (String item : items) {
            if (TextUtils.isEmpty(item)) {
                continue;
            }
            builder.append("\n• ").append(item);
        }
    }

    private void bindSection(TextView textView, String content) {
        if (TextUtils.isEmpty(content)) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(content);
        }
    }

    private int getChapterIndex(int adapterPosition) {
        return overview == null ? adapterPosition : adapterPosition - 1;
    }

    private String joinAsBullets(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (TextUtils.isEmpty(item)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("• ").append(item);
        }
        return builder.toString();
    }

    private String joinAsCommaList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String item : items) {
            if (TextUtils.isEmpty(item)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("、");
            }
            builder.append(item);
        }
        return builder.toString();
    }

    private String buildTextSection(String prefix, String value) {
        if (TextUtils.isEmpty(value)) {
            return "";
        }
        return prefix + value;
    }

    private String buildListSection(String prefix, List<String> values, boolean useBullets) {
        String content = useBullets ? joinAsBullets(values) : joinAsCommaList(values);
        if (TextUtils.isEmpty(content)) {
            return "";
        }
        return prefix + content;
    }

    private void bindSourceChip(Chip chip, PlotChapterSummary item) {
        if (chip == null || item == null || TextUtils.isEmpty(item.getSource())) {
            if (chip != null) {
                chip.setVisibility(View.GONE);
            }
            return;
        }
        chip.setVisibility(View.VISIBLE);
        switch (item.getSource()) {
            case "ai":
                chip.setText(R.string.plot_chapter_source_ai);
                break;
            case "tolerant":
                chip.setText(R.string.plot_chapter_source_tolerant);
                break;
            default:
                chip.setText(R.string.plot_chapter_source_fallback);
                break;
        }
    }

    @Override
    public int getItemCount() {
        int chapterCount = items == null ? 0 : items.size();
        return chapterCount + (overview == null ? 0 : 1);
    }

    static class ChapterViewHolder extends RecyclerView.ViewHolder {
        final TextView tvChapterLabel;
        final Chip chipChapterSource;
        final TextView tvChapterTitle;
        final TextView tvBriefSummary;
        final TextView tvDetailSummary;
        final TextView tvKeyEvents;
        final TextView tvCharacters;
        final TextView tvConflict;
        final TextView tvStoryFunction;
        final View layoutDetailContainer;
        final TextView tvExpandToggle;
        final View btnEditChapter;

        ChapterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChapterLabel = itemView.findViewById(R.id.tv_plot_chapter_label);
            chipChapterSource = itemView.findViewById(R.id.chip_plot_chapter_source);
            tvExpandToggle = itemView.findViewById(R.id.tv_plot_expand_toggle);
            tvChapterTitle = itemView.findViewById(R.id.tv_plot_chapter_title);
            tvBriefSummary = itemView.findViewById(R.id.tv_plot_brief_summary);
            layoutDetailContainer = itemView.findViewById(R.id.layout_plot_detail_container);
            tvDetailSummary = itemView.findViewById(R.id.tv_plot_detail_summary);
            tvKeyEvents = itemView.findViewById(R.id.tv_plot_key_events);
            tvCharacters = itemView.findViewById(R.id.tv_plot_characters);
            tvConflict = itemView.findViewById(R.id.tv_plot_conflict);
            tvStoryFunction = itemView.findViewById(R.id.tv_plot_story_function);
            btnEditChapter = itemView.findViewById(R.id.btn_plot_edit_chapter);
        }
    }

    static class OverviewViewHolder extends RecyclerView.ViewHolder {
        final TextView tvOverviewContent;

        OverviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOverviewContent = itemView.findViewById(R.id.tv_plot_overview_content);
        }
    }
}
