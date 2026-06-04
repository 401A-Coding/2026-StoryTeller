package com.example.storyteller.ui.adapter;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.PlotTreeEvent;

import java.util.ArrayList;
import java.util.List;

public class PlotTreeEventAdapter extends RecyclerView.Adapter<PlotTreeEventAdapter.EventViewHolder> {

    public interface Listener {
        void onEventClick(PlotTreeEvent event, int position);
    }

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_SEPARATOR = 1;
    private static final int TYPE_EXTERNAL_HEADER = 2;
    private static final int TYPE_FORK_MARKER = 3;

    private static final int COLOR_FORK_BG = 0xFFF0FFF0;

    private static final int COLOR_SEPARATOR_BG = Color.parseColor("#F0F4FF");
    private static final int COLOR_EXTERNAL_BG = Color.parseColor("#FFF3F0");

    private final List<PlotTreeEvent> events = new ArrayList<>();
    private Listener listener;
    private int branchSourceEventId = -1;

    public void setData(List<PlotTreeEvent> newEvents, int branchSourceEventId) {
        events.clear();
        if (newEvents != null) {
            events.addAll(newEvents);
        }
        this.branchSourceEventId = branchSourceEventId;
        notifyDataSetChanged();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        PlotTreeEvent event = events.get(position);
        if (event.getId() == -1) return TYPE_SEPARATOR;
        if (event.getId() == -2) return TYPE_EXTERNAL_HEADER;
        if (event.getId() == -3) return TYPE_FORK_MARKER;
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plot_tree_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        PlotTreeEvent event = events.get(position);
        int viewType = getItemViewType(position);
        boolean isExternalEvent = !TextUtils.isEmpty(event.getTitle())
                && event.getTitle().startsWith("[外部]");

        // --- Default visibility ---
        holder.tvIndex.setVisibility(View.VISIBLE);
        holder.tvIndex.setBackgroundResource(R.drawable.bg_outline_edittext);
        holder.tvIndex.setTextColor(holder.itemView.getContext().getResources()
                .getColor(R.color.text_primary));

        // --- Card styling ---
        com.google.android.material.card.MaterialCardView card =
                (com.google.android.material.card.MaterialCardView) holder.itemView;
        card.setStrokeWidth(1);

        switch (viewType) {
            case TYPE_SEPARATOR:
                // Branch section header
                holder.tvIndex.setVisibility(View.INVISIBLE);
                holder.tvIndex.setText("");
                holder.tvTypeBadge.setVisibility(View.VISIBLE);
                holder.tvTypeBadge.setText("分支段落");
                holder.tvTypeBadge.setTextColor(holder.itemView.getContext().getResources()
                        .getColor(R.color.link_color));
                holder.tvTitle.setText(TextUtils.isEmpty(event.getTitle())
                        ? "未命名分支" : event.getTitle());
                holder.tvTitle.setTextSize(15);
                holder.tvTitle.setTextColor(holder.itemView.getContext().getResources()
                        .getColor(R.color.text_primary));
                holder.tvSummary.setText(TextUtils.isEmpty(event.getSummary())
                        ? "" : event.getSummary());
                holder.tvSummary.setVisibility(
                        TextUtils.isEmpty(event.getSummary()) ? View.GONE : View.VISIBLE);
                holder.tvTags.setVisibility(View.GONE);
                holder.tvBranchOrigin.setVisibility(View.GONE);
                card.setCardBackgroundColor(COLOR_SEPARATOR_BG);
                card.setStrokeColor(Color.parseColor("#D0D8F0"));
                card.setCardElevation(0);
                break;

            case TYPE_EXTERNAL_HEADER:
                // External child branch header
                holder.tvIndex.setVisibility(View.INVISIBLE);
                holder.tvIndex.setText("");
                holder.tvTypeBadge.setVisibility(View.VISIBLE);
                holder.tvTypeBadge.setText("外部作品");
                holder.tvTypeBadge.setTextColor(Color.parseColor("#E65100"));
                holder.tvTitle.setText(TextUtils.isEmpty(event.getTitle())
                        ? "" : event.getTitle());
                holder.tvTitle.setTextSize(15);
                holder.tvTitle.setTextColor(holder.itemView.getContext().getResources()
                        .getColor(R.color.text_primary));
                holder.tvSummary.setText(TextUtils.isEmpty(event.getSummary())
                        ? "来自导出作品的分支剧情" : event.getSummary());
                holder.tvSummary.setVisibility(View.VISIBLE);
                holder.tvTags.setVisibility(View.GONE);
                holder.tvBranchOrigin.setVisibility(View.GONE);
                card.setCardBackgroundColor(COLOR_EXTERNAL_BG);
                card.setStrokeColor(Color.parseColor("#F0D0C0"));
                card.setCardElevation(0);
                break;

            case TYPE_FORK_MARKER:
                holder.tvIndex.setVisibility(View.INVISIBLE);
                holder.tvTypeBadge.setVisibility(View.VISIBLE);
                holder.tvTypeBadge.setText("分叉");
                holder.tvTypeBadge.setTextColor(Color.parseColor("#2E7D32"));
                holder.tvTitle.setText(TextUtils.isEmpty(event.getTitle()) ? "" : event.getTitle());
                holder.tvTitle.setTextSize(14);
                holder.tvSummary.setText(TextUtils.isEmpty(event.getSummary()) ? "" : event.getSummary());
                holder.tvSummary.setVisibility(TextUtils.isEmpty(event.getSummary()) ? View.GONE : View.VISIBLE);
                holder.tvTags.setVisibility(View.GONE);
                holder.tvBranchOrigin.setVisibility(View.GONE);
                card.setCardBackgroundColor(COLOR_FORK_BG);
                card.setStrokeColor(Color.parseColor("#C8E6C9"));
                card.setCardElevation(0);
                holder.itemView.setOnClickListener(v -> {});
                return;

            default:
                // Normal event
                holder.tvIndex.setText(String.valueOf(position + 1));
                holder.tvIndex.setBackgroundResource(R.drawable.bg_outline_edittext);
                holder.tvIndex.setTextColor(holder.itemView.getContext().getResources()
                        .getColor(isExternalEvent ? R.color.text_secondary
                                : R.color.text_primary));

                if (isExternalEvent) {
                    holder.tvTypeBadge.setVisibility(View.VISIBLE);
                    holder.tvTypeBadge.setText("外部");
                    holder.tvTypeBadge.setTextColor(Color.parseColor("#E65100"));
                } else {
                    holder.tvTypeBadge.setVisibility(View.GONE);
                }

                holder.tvTitle.setText(TextUtils.isEmpty(event.getTitle())
                        ? "未命名事件" : event.getTitle());
                holder.tvTitle.setTextSize(16);
                holder.tvTitle.setTextColor(holder.itemView.getContext().getResources()
                        .getColor(isExternalEvent ? R.color.text_secondary
                                : R.color.text_primary));

                holder.tvSummary.setText(TextUtils.isEmpty(event.getSummary())
                        ? "点击补充该事件的剧情摘要" : event.getSummary());
                holder.tvSummary.setVisibility(View.VISIBLE);

                if (event.getTags() != null && !event.getTags().isEmpty()) {
                    holder.tvTags.setVisibility(View.VISIBLE);
                    holder.tvTags.setText(TextUtils.join(" · ", event.getTags()));
                } else {
                    holder.tvTags.setVisibility(View.GONE);
                }

                if (branchSourceEventId > 0 && event.getId() == branchSourceEventId
                        && !isExternalEvent) {
                    holder.tvBranchOrigin.setVisibility(View.VISIBLE);
                    holder.tvBranchOrigin.setText("分支从这里开始分叉");
                } else {
                    holder.tvBranchOrigin.setVisibility(View.GONE);
                }

                card.setCardBackgroundColor(
                        isExternalEvent ? Color.parseColor("#FFFBF9")
                                : holder.itemView.getContext().getResources()
                                .getColor(R.color.background_card));
                card.setStrokeColor(holder.itemView.getContext().getResources()
                        .getColor(isExternalEvent ? R.color.divider : R.color.divider));
                card.setCardElevation(1);
                break;
        }

        // Click: separators and external headers in all-branches mode are non-interactive;
        // the fragment handles this logic, but we also apply a visual cue.
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        final TextView tvIndex;
        final TextView tvTitle;
        final TextView tvSummary;
        final TextView tvTags;
        final TextView tvBranchOrigin;
        final TextView tvTypeBadge;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_event_index);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvSummary = itemView.findViewById(R.id.tv_event_summary);
            tvTags = itemView.findViewById(R.id.tv_event_tags);
            tvBranchOrigin = itemView.findViewById(R.id.tv_event_branch_origin);
            tvTypeBadge = itemView.findViewById(R.id.tv_event_type_badge);
        }
    }
}