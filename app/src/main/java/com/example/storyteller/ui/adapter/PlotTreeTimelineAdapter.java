package com.example.storyteller.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.storyteller.R;
import com.example.storyteller.model.PlotTreeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class PlotTreeTimelineAdapter extends RecyclerView.Adapter<PlotTreeTimelineAdapter.TimelineViewHolder> {

    public interface Listener {
        void onEventClick(PlotTreeEvent event, int branchId);
    }

    // Row types
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_SHARED = 1;
    public static final int TYPE_FORK = 2;
    public static final int TYPE_SPLIT = 3;

    // Data models
    public static class ColumnHeader {
        public String branchName;
        public int branchColor;
        public String forkOrigin;
        public String description;  // 走向说明
        public int branchId;        // 分支ID，用于方向操作回调

        public ColumnHeader(String branchName, int branchColor, String forkOrigin) {
            this(branchName, branchColor, forkOrigin, "", 0);
        }

        public ColumnHeader(String branchName, int branchColor, String forkOrigin, String description) {
            this(branchName, branchColor, forkOrigin, description, 0);
        }

        public ColumnHeader(String branchName, int branchColor, String forkOrigin, String description, int branchId) {
            this.branchName = branchName;
            this.branchColor = branchColor;
            this.forkOrigin = forkOrigin;
            this.description = description != null ? description : "";
            this.branchId = branchId;
        }
    }

    public static class ForkTarget {
        public String branchName;
        public int branchColor;

        public ForkTarget(String branchName, int branchColor) {
            this.branchName = branchName;
            this.branchColor = branchColor;
        }
    }

    public static class Cell {
        public PlotTreeEvent event;  // null for empty cells or placeholder cells
        public int branchColor;
        public String description;   // 走向说明（占位卡片使用）
        public boolean isPlaceholder; // 是否为走向说明占位卡片
        public int directionIndex = -1; // 方向事件在branch.events中的索引，>=0表示方向占位
        public int branchId;          // 所属分支ID

        public Cell(PlotTreeEvent event, int branchColor) {
            this.event = event;
            this.branchColor = branchColor;
            this.isPlaceholder = false;
            this.directionIndex = -1;
        }

        public Cell(PlotTreeEvent event, int branchColor, int branchId) {
            this(event, branchColor);
            this.branchId = branchId;
        }

        /** Create a placeholder cell that shows branch direction description */
        public static Cell placeholder(String description, int branchColor) {
            Cell c = new Cell(null, branchColor);
            c.description = description;
            c.isPlaceholder = true;
            return c;
        }

        public static Cell empty() {
            return new Cell(null, Color.TRANSPARENT);
        }

        /** 该Cell是否为方向事件卡片 */
        public boolean isDirection() {
            return event != null && event.isDirection();
        }
    }

    public static class TimelineRow {
        public static final int TYPE_SHARED = 1;
        public static final int TYPE_FORK = 2;
        public static final int TYPE_SPLIT = 3;

        public int rowType;
        public List<Cell> cells;
        public List<ForkTarget> forkTargets;
        public int forkEventId;

        private TimelineRow(int rowType) {
            this.rowType = rowType;
            this.cells = new ArrayList<>();
        }

        public static TimelineRow shared(Cell cell) {
            TimelineRow row = new TimelineRow(TYPE_SHARED);
            row.cells.add(cell);
            return row;
        }

        public static TimelineRow fork(Cell cell, List<ForkTarget> forkTargets, int forkEventId) {
            TimelineRow row = new TimelineRow(TYPE_FORK);
            row.cells.add(cell);
            row.forkTargets = forkTargets;
            row.forkEventId = forkEventId;
            return row;
        }

        public static TimelineRow split(List<Cell> cells) {
            TimelineRow row = new TimelineRow(TYPE_SPLIT);
            row.cells = cells;
            return row;
        }
    }

    // State
    private List<ColumnHeader> columnHeaders = new ArrayList<>();
    private List<TimelineRow> rows = new ArrayList<>();
    private Listener listener;
    private int columnWidthPx;
    private int cardHeightPx;

    private static final int[] BRANCH_COLORS = {
        0xFF2196F3, 0xFF4CAF50, 0xFF9C27B0, 0xFFFF9800,
        0xFF00BCD4, 0xFFE91E63, 0xFF3F51B5, 0xFF009688
    };

    public PlotTreeTimelineAdapter(int columnWidthPx, int cardHeightPx) {
        this.columnWidthPx = columnWidthPx;
        this.cardHeightPx = cardHeightPx;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<ColumnHeader> headers, List<TimelineRow> rows) {
        this.columnHeaders = headers != null ? headers : new ArrayList<>();
        this.rows = rows != null ? rows : new ArrayList<>();
        notifyDataSetChanged();
    }

    // Return total items: 1 header row + data rows
    @Override
    public int getItemCount() {
        return 1 + rows.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        return rows.get(position - 1).rowType;
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_plot_tree_header, parent, false);
            return new TimelineViewHolder(view, viewType);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_plot_tree_row, parent, false);
        return new TimelineViewHolder(view, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        Context ctx = holder.itemView.getContext();
        if (position == 0) {
            bindHeader(holder, ctx);
            return;
        }

        TimelineRow row = rows.get(position - 1);
        holder.container.removeAllViews();

        switch (row.rowType) {
            case TYPE_SHARED:
                bindShared(holder, row, ctx);
                break;
            case TYPE_FORK:
                bindFork(holder, row, ctx);
                break;
            case TYPE_SPLIT:
                bindSplit(holder, row, ctx);
                break;
        }
    }

    private void bindHeader(TimelineViewHolder holder, Context ctx) {
        holder.container.removeAllViews();
        if (columnHeaders.isEmpty()) return;

        float density = ctx.getResources().getDisplayMetrics().density;

        for (int i = 0; i < columnHeaders.size(); i++) {
            ColumnHeader h = columnHeaders.get(i);
            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.HORIZONTAL);
            col.setGravity(Gravity.CENTER_VERTICAL);
            col.setPadding((int)(8 * density), 0, (int)(8 * density), 0);

            if (i == 0) {
                // Mainline header - full width (only one column visible before forks)
                col.setLayoutParams(new LinearLayout.LayoutParams(columnWidthPx, ViewGroup.LayoutParams.MATCH_PARENT));
            } else {
                col.setLayoutParams(new LinearLayout.LayoutParams(columnWidthPx, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            // Color dot
            View dot = new View(ctx);
            int dotSize = (int)(10 * density);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dotSize, dotSize);
            dotParams.setMargins(0, 0, (int)(6 * density), 0);
            dot.setLayoutParams(dotParams);
            dot.setBackgroundColor(h.branchColor);
            col.addView(dot);

            // Branch name
            TextView tvName = new TextView(ctx);
            tvName.setText(h.branchName);
            tvName.setTextSize(12);
            tvName.setTextColor(ctx.getResources().getColor(R.color.text_primary));
            tvName.setMaxLines(1);
            tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
            col.addView(tvName);

            holder.container.addView(col);
        }
    }

    private void bindShared(TimelineViewHolder holder, TimelineRow row, Context ctx) {
        Cell cell = row.cells.get(0);
        View cardView = inflateCard(cell, ctx);
        // Full width across all columns
        int totalWidth = Math.max(columnWidthPx, columnWidthPx * columnHeaders.size());
        cardView.setLayoutParams(new LinearLayout.LayoutParams(totalWidth, cardHeightPx));
        holder.container.addView(cardView);
    }

    private void bindFork(TimelineViewHolder holder, TimelineRow row, Context ctx) {
        Cell cell = row.cells.get(0);
        float density = ctx.getResources().getDisplayMetrics().density;

        // Build branchName -> ForkTarget lookup
        Map<String, ForkTarget> targetMap = new HashMap<>();
        if (row.forkTargets != null) {
            for (ForkTarget ft : row.forkTargets) {
                targetMap.put(ft.branchName, ft);
            }
        }

        // Col 0: fork event card
        FrameLayout colFrame = new FrameLayout(ctx);
        colFrame.setLayoutParams(new LinearLayout.LayoutParams(columnWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT));
        View cardView = inflateCard(cell, ctx);
        cardView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, cardHeightPx));
        colFrame.addView(cardView);
        holder.container.addView(colFrame);

        // Col 1..N: arrows for matching branches, empty connectors for others
        int colCount = columnHeaders.size();
        for (int i = 1; i < colCount; i++) {
            ColumnHeader h = columnHeaders.get(i);
            ForkTarget ft = targetMap.get(h.branchName);

            FrameLayout branchCol = new FrameLayout(ctx);
            branchCol.setLayoutParams(new LinearLayout.LayoutParams(columnWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (ft != null) {
                LinearLayout arrowLayout = new LinearLayout(ctx);
                arrowLayout.setOrientation(LinearLayout.VERTICAL);
                arrowLayout.setGravity(Gravity.CENTER);
                arrowLayout.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, cardHeightPx));

                TextView tvArrow = new TextView(ctx);
                tvArrow.setText("\u2192");
                tvArrow.setTextSize(16);
                tvArrow.setTextColor(ft.branchColor);
                tvArrow.setGravity(Gravity.CENTER);
                arrowLayout.addView(tvArrow);

                TextView tvLabel = new TextView(ctx);
                tvLabel.setText(ft.branchName);
                tvLabel.setTextSize(10);
                tvLabel.setTextColor(ctx.getResources().getColor(R.color.text_secondary));
                tvLabel.setMaxLines(1);
                tvLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
                tvLabel.setGravity(Gravity.CENTER);
                arrowLayout.addView(tvLabel);

                branchCol.addView(arrowLayout);
            } else {
                View line = new View(ctx);
                FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(
                        (int)(1 * density), cardHeightPx);
                lineParams.gravity = Gravity.CENTER_HORIZONTAL;
                line.setLayoutParams(lineParams);
                line.setBackgroundColor(ctx.getResources().getColor(R.color.divider));
                branchCol.addView(line);
            }

            holder.container.addView(branchCol);
        }
    }

    private void bindSplit(TimelineViewHolder holder, TimelineRow row, Context ctx) {
        int colCount = columnHeaders.size();
        List<Cell> cells = row.cells;

        for (int i = 0; i < colCount; i++) {
            FrameLayout colFrame = new FrameLayout(ctx);
            colFrame.setLayoutParams(new LinearLayout.LayoutParams(columnWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT));

            if (i < cells.size() && cells.get(i) != null && cells.get(i).event != null) {
                View cardView = inflateCard(cells.get(i), ctx);
                cardView.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, cardHeightPx));
                colFrame.addView(cardView);
            } else {
                // Empty connector: vertical line
                View line = new View(ctx);
                FrameLayout.LayoutParams lineParams = new FrameLayout.LayoutParams(
                        (int)(1 * ctx.getResources().getDisplayMetrics().density),
                        cardHeightPx);
                lineParams.gravity = Gravity.CENTER_HORIZONTAL;
                line.setLayoutParams(lineParams);
                line.setBackgroundColor(ctx.getResources().getColor(R.color.divider));
                colFrame.addView(line);
            }

            holder.container.addView(colFrame);
        }
    }

    private View inflateCard(Cell cell, Context ctx) {
        View cardView = LayoutInflater.from(ctx).inflate(R.layout.item_plot_tree_card, null);

        if (cell != null && cell.event != null) {
            PlotTreeEvent event = cell.event;
            TextView tvTitle = cardView.findViewById(R.id.tv_card_title);
            TextView tvSummary = cardView.findViewById(R.id.tv_card_summary);

            tvTitle.setText(TextUtils.isEmpty(event.getTitle()) ? "\u672a\u547d\u540d" : event.getTitle());
            tvSummary.setText(TextUtils.isEmpty(event.getSummary()) ? "" : event.getSummary());

            // Left accent stripe via background tint
            com.google.android.material.card.MaterialCardView mc =
                    (com.google.android.material.card.MaterialCardView) cardView;
            if (cell.branchColor != 0 && cell.branchColor != Color.TRANSPARENT) {
                mc.setStrokeColor(cell.branchColor);
                mc.setStrokeWidth((int)(2 * ctx.getResources().getDisplayMetrics().density));
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEventClick(event, -1);
                }
            });
        }

        return cardView;
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout container;

        TimelineViewHolder(@NonNull View itemView, int viewType) {
            super(itemView);
            if (viewType == TYPE_HEADER) {
                container = itemView.findViewById(R.id.header_container);
            } else {
                container = itemView.findViewById(R.id.row_container);
            }
        }
    }

    public static int getBranchColor(int index) {
        if (index <= 0) return BRANCH_COLORS[0];
        return BRANCH_COLORS[index % BRANCH_COLORS.length];
    }

    public static int getMainlineColor() {
        return BRANCH_COLORS[0];
    }
}