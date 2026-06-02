package com.example.storyteller.ui.adapter;
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
    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plot_tree_event, parent, false);
        return new EventViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        PlotTreeEvent event = events.get(position);
        holder.tvIndex.setText(String.valueOf(position + 1));
        holder.tvTitle.setText(TextUtils.isEmpty(event.getTitle()) ? "未命名事件" : event.getTitle());
        holder.tvSummary.setText(TextUtils.isEmpty(event.getSummary()) ? "点击补充该事件的剧情摘要" : event.getSummary());
        if (event.getTags() != null && !event.getTags().isEmpty()) {
            holder.tvTags.setVisibility(View.VISIBLE);
            holder.tvTags.setText(TextUtils.join(" · ", event.getTags()));
        } else {
            holder.tvTags.setVisibility(View.GONE);
        }
        if (branchSourceEventId > 0 && event.getId() == branchSourceEventId) {
            holder.tvBranchOrigin.setVisibility(View.VISIBLE);
            holder.tvBranchOrigin.setText("分支从这里开始分化");
        } else {
            holder.tvBranchOrigin.setVisibility(View.GONE);
        }
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
        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIndex = itemView.findViewById(R.id.tv_event_index);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvSummary = itemView.findViewById(R.id.tv_event_summary);
            tvTags = itemView.findViewById(R.id.tv_event_tags);
            tvBranchOrigin = itemView.findViewById(R.id.tv_event_branch_origin);
        }
    }
}
