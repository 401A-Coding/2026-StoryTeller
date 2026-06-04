package com.example.storyteller.ui.fragment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.util.AttributeSet;

import com.example.storyteller.model.PlotTreeEvent;
import com.example.storyteller.ui.adapter.PlotTreeTimelineAdapter.Cell;
import com.example.storyteller.ui.adapter.PlotTreeTimelineAdapter.ColumnHeader;
import com.example.storyteller.ui.adapter.PlotTreeTimelineAdapter.ForkTarget;
import com.example.storyteller.ui.adapter.PlotTreeTimelineAdapter.TimelineRow;
import com.example.storyteller.utils.ThemeColorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlotTreeCanvasView extends View {

    // ── Listener ────────────────────────────────────────────
    public interface Listener {
        void onCardClick(PlotTreeEvent event, int branchColor);
        void onForkNodeClick(PlotTreeEvent sourceEvent);
        /** 方向事件卡片被点击 */
        void onDirectionClick(PlotTreeEvent event, int branchId, int directionIndex);
        /** 分支列底部的"+"按钮被点击，用于新建发展方向 */
        void onDirectionAddClick(int branchId, int branchColor);
    }

    // ── Hit testing ────────────────────────────────────────
    private static class HitInfo {
        static final int TYPE_CARD = 0;
        static final int TYPE_FORK_NODE = 1;
        static final int TYPE_DIRECTION_CARD = 2;
        static final int TYPE_DIRECTION_ADD = 3;
        final RectF rect;
        final int type;
        final PlotTreeEvent event;
        final int rowIndex;
        final int branchColor;
        final int branchId;
        final int colIndex;

        HitInfo(RectF rect, int type, PlotTreeEvent event, int rowIndex, int branchColor) {
            this(rect, type, event, rowIndex, branchColor, 0, -1);
        }

        HitInfo(RectF rect, int type, PlotTreeEvent event, int rowIndex, int branchColor, int branchId, int colIndex) {
            this.rect = rect;
            this.type = type;
            this.event = event;
            this.rowIndex = rowIndex;
            this.branchColor = branchColor;
            this.branchId = branchId;
            this.colIndex = colIndex;
        }
    }

    private static final float CARD_WIDTH_DP = 200f;
    private static final float CARD_HEIGHT_DP = 72f;
    private static final float CARD_RADIUS_DP = 8f;
    private static final float CARD_MARGIN_DP = 8f;
    private static final float CARD_PADDING_DP = 10f;
    private static final float HEADER_HEIGHT_DP = 40f;
    private static final float LINE_WIDTH_DP = 2f;
    private static final float ARROW_SIZE_DP = 8f;
    private static final float COLOR_BAR_WIDTH_DP = 4f;
    private static final float FORK_LABEL_SIZE_DP = 10f;
    private static final float DIRECTION_ADD_RADIUS_DP = 14f;

    private float density;
    private float cardWidth;
    private float cardHeight;
    private float cardRadius;
    private float cardMargin;
    private float cardPadding;
    private float headerHeight;
    private float lineWidth;
    private float arrowSize;
    private float colorBarWidth;
    private float forkLabelSize;
    private float directionAddRadius;

    private List<ColumnHeader> columnHeaders = new ArrayList<>();
    private List<TimelineRow> rows = new ArrayList<>();

    private Paint cardBgPaint;
    private Paint cardStrokePaint;
    private Paint linePaint;
    private Paint dashLinePaint;
    private Paint arrowPaint;
    private TextPaint titlePaint;
    private TextPaint summaryPaint;
    private TextPaint headerPaint;
    private TextPaint forkLabelPaint;
    private Paint headerBgPaint;

    private int totalWidth;
    private int totalHeight;

    private final List<HitInfo> hitInfos = new ArrayList<>();
    private Listener listener;

    // Touch tracking for click-vs-scroll disambiguation
    private float downX;
    private float downY;
    private int touchSlop;
    private boolean isClickCandidate;

    private Paint forkNodeBgPaint;
    private TextPaint forkNodeTextPaint;
    private Paint borderPaint;   // header border / fork node stroke

    public PlotTreeCanvasView(Context context) {
        this(context, null);
    }

    public PlotTreeCanvasView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlotTreeCanvasView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setClickable(true);

        cardWidth = CARD_WIDTH_DP * density;
        cardHeight = CARD_HEIGHT_DP * density;
        cardRadius = CARD_RADIUS_DP * density;
        cardMargin = CARD_MARGIN_DP * density;
        cardPadding = CARD_PADDING_DP * density;
        headerHeight = HEADER_HEIGHT_DP * density;
        lineWidth = LINE_WIDTH_DP * density;
        arrowSize = ARROW_SIZE_DP * density;
        colorBarWidth = COLOR_BAR_WIDTH_DP * density;
        forkLabelSize = FORK_LABEL_SIZE_DP * density;
        directionAddRadius = DIRECTION_ADD_RADIUS_DP * density;

        // Create paints (colors set by applyColors)
        cardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBgPaint.setStyle(Paint.Style.FILL);

        cardStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardStrokePaint.setStyle(Paint.Style.STROKE);
        cardStrokePaint.setStrokeWidth(1f * density);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(lineWidth);

        dashLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dashLinePaint.setStyle(Paint.Style.STROKE);
        dashLinePaint.setStrokeWidth(lineWidth);
        dashLinePaint.setPathEffect(new DashPathEffect(new float[]{4f * density, 4f * density}, 0));

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setStyle(Paint.Style.FILL);

        titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(14f * density);
        titlePaint.setFakeBoldText(true);

        summaryPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        summaryPaint.setTextSize(12f * density);

        headerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setTextSize(12f * density);
        headerPaint.setFakeBoldText(true);

        forkLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        forkLabelPaint.setTextSize(forkLabelSize);

        headerBgPaint = new Paint();
        headerBgPaint.setStyle(Paint.Style.FILL);

        forkNodeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        forkNodeBgPaint.setStyle(Paint.Style.FILL);

        forkNodeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        forkNodeTextPaint.setTextSize(14f * density);
        forkNodeTextPaint.setFakeBoldText(true);
        forkNodeTextPaint.setTextAlign(Paint.Align.CENTER);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1f * density);

        applyColors(context);
    }

    /** Apply theme-aware colors from ThemeColorUtils. Called on init and on theme change. */
    private void applyColors(Context context) {
        cardBgPaint.setColor(ThemeColorUtils.getBackgroundCard(context));
        cardStrokePaint.setColor(ThemeColorUtils.getDivider(context));
        linePaint.setColor(ThemeColorUtils.getDivider(context));
        dashLinePaint.setColor(ThemeColorUtils.getDivider(context));
        titlePaint.setColor(ThemeColorUtils.getTextPrimary(context));
        summaryPaint.setColor(ThemeColorUtils.getTextSecondary(context));
        headerPaint.setColor(ThemeColorUtils.getTextSecondary(context));
        headerBgPaint.setColor(ThemeColorUtils.getBackgroundSecondary(context));
        forkNodeBgPaint.setColor(ThemeColorUtils.getBackgroundSecondary(context));
        forkNodeTextPaint.setColor(ThemeColorUtils.getTextHint(context));
        borderPaint.setColor(ThemeColorUtils.getDivider(context));
        forkLabelPaint.setColor(ThemeColorUtils.getTextSecondary(context));
    }

    /** Call when theme changes to update all Canvas-drawn colors. */
    public void onThemeChanged() {
        applyColors(getContext());
        invalidate();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<ColumnHeader> headers, List<TimelineRow> rows) {
        this.columnHeaders = headers != null ? headers : new ArrayList<>();
        this.rows = rows != null ? rows : new ArrayList<>();
        calculateLayout();
        invalidate();
    }

    private void calculateLayout() {
        int colCount = Math.max(1, columnHeaders.size());
        float colW = cardWidth + cardMargin * 2;
        float rowH = cardHeight + cardMargin * 2;

        // 计算各列最大高度：每列的大加号紧跟最后一个卡片下方
        float maxBottom = headerHeight + rows.size() * rowH;
        for (int c = 0; c < colCount; c++) {
            int lastRow = findLastContentRow(c);
            if (lastRow >= 0) {
                // 最后一个卡片下方留 rowH 空间放➕按钮
                float colBottom = headerHeight + (lastRow + 1.5f) * rowH;
                if (colBottom > maxBottom) maxBottom = colBottom;
            }
        }
        totalWidth = (int)(colCount * colW);
        totalHeight = (int)(maxBottom + cardMargin);
        setMinimumWidth(totalWidth);
        setMinimumHeight(totalHeight);
        rebuildHitRects(colCount, colW, rowH);
    }

    /** 查找指定列最后一个有内容的行索引，无内容返回-1 */
    private int findLastContentRow(int col) {
        for (int r = rows.size() - 1; r >= 0; r--) {
            if (hasColContent(rows.get(r), col)) return r;
        }
        return -1;
    }

    private void rebuildHitRects(int colCount, float colW, float rowH) {
        hitInfos.clear();

        // Fork node hit rects FIRST (before card rects) so they take priority
        // when a touch overlaps both (fork node overlaps card edges by ~8dp)
        float forkNodeR = 10f * density;
        for (int r = 0; r < rows.size() - 1; r++) {
            TimelineRow cur = rows.get(r);
            TimelineRow nxt = rows.get(r + 1);
            if (cur.rowType == TimelineRow.TYPE_FORK || nxt.rowType == TimelineRow.TYPE_FORK) continue;
            if (!hasColContent(cur, 0) || !hasColContent(nxt, 0)) continue;
            // Extract the mainline event from the current row
            PlotTreeEvent curEvent = getEventAt(cur, 0);
            if (curEvent == null) continue;
            float cx = colW / 2f;
            float cy = headerHeight + r * rowH + rowH;
            RectF nodeRect = new RectF(cx - forkNodeR, cy - forkNodeR, cx + forkNodeR, cy + forkNodeR);
            hitInfos.add(new HitInfo(nodeRect, HitInfo.TYPE_FORK_NODE, curEvent, r, 0));
        }

        // Card hit rects
        for (int r = 0; r < rows.size(); r++) {
            TimelineRow row = rows.get(r);
            float rowY = headerHeight + r * rowH + cardMargin;
            if (row.rowType == TimelineRow.TYPE_SHARED) {
                if (!row.cells.isEmpty() && row.cells.get(0) != null && row.cells.get(0).event != null) {
                    Cell cell = row.cells.get(0);
                    RectF rect = new RectF(cardMargin, rowY, colCount * colW - cardMargin, rowY + cardHeight);
                    int type = cell.isDirection() ? HitInfo.TYPE_DIRECTION_CARD : HitInfo.TYPE_CARD;
                    hitInfos.add(new HitInfo(rect, type, cell.event, r, cell.branchColor, cell.branchId, 0));
                }
            } else if (row.rowType == TimelineRow.TYPE_FORK) {
                if (!row.cells.isEmpty() && row.cells.get(0) != null && row.cells.get(0).event != null) {
                    Cell cell = row.cells.get(0);
                    RectF rect = new RectF(cardMargin, rowY, colW - cardMargin, rowY + cardHeight);
                    int type = cell.isDirection() ? HitInfo.TYPE_DIRECTION_CARD : HitInfo.TYPE_CARD;
                    hitInfos.add(new HitInfo(rect, type, cell.event, r, cell.branchColor, cell.branchId, 0));
                }
            } else if (row.rowType == TimelineRow.TYPE_SPLIT) {
                for (int c = 0; c < colCount; c++) {
                    if (c < row.cells.size() && row.cells.get(c) != null && row.cells.get(c).event != null) {
                        Cell cell = row.cells.get(c);
                        float left = c * colW + cardMargin;
                        float right = (c + 1) * colW - cardMargin;
                        RectF rect = new RectF(left, rowY, right, rowY + cardHeight);
                        int type = cell.isDirection() ? HitInfo.TYPE_DIRECTION_CARD : HitInfo.TYPE_CARD;
                        hitInfos.add(new HitInfo(rect, type, cell.event, r, cell.branchColor, cell.branchId, c));
                    }
                }
            }
        }

        // Direction add button hit rects — 每列紧跟最后一个卡片下方
        for (int c = 0; c < colCount; c++) {
            int lastRow = findLastContentRow(c);
            if (lastRow < 0) continue;
            int branchId = columnHeaders.get(c).branchId;
            int branchColor = columnHeaders.get(c).branchColor;
            float cx = c * colW + colW / 2f;
            float addBtnY = headerHeight + (lastRow + 1) * rowH + rowH / 2f;
            RectF nodeRect = new RectF(cx - directionAddRadius, addBtnY - directionAddRadius,
                    cx + directionAddRadius, addBtnY + directionAddRadius);
            hitInfos.add(new HitInfo(nodeRect, HitInfo.TYPE_DIRECTION_ADD, null, lastRow + 1, branchColor, branchId, c));
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = Math.max(totalWidth, getSuggestedMinimumWidth());
        int h = Math.max(totalHeight, getSuggestedMinimumHeight());
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), resolveSize(h, heightMeasureSpec));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (listener == null) return super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                isClickCandidate = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isClickCandidate) {
                    float dx = Math.abs(event.getX() - downX);
                    float dy = Math.abs(event.getY() - downY);
                    if (dx > touchSlop || dy > touchSlop) {
                        // Significant movement → this is a scroll, not a click
                        isClickCandidate = false;
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                }
                return isClickCandidate || super.onTouchEvent(event);

            case MotionEvent.ACTION_UP:
                if (isClickCandidate) {
                    float x = event.getX();
                    float y = event.getY();
                    for (HitInfo hit : hitInfos) {
                        if (hit.rect.contains(x, y)) {
                            if (hit.type == HitInfo.TYPE_CARD) {
                                listener.onCardClick(hit.event, hit.branchColor);
                            } else if (hit.type == HitInfo.TYPE_DIRECTION_CARD) {
                                listener.onDirectionClick(hit.event, hit.branchId, hit.colIndex >= 0 ? hit.colIndex : 0);
                            } else if (hit.type == HitInfo.TYPE_FORK_NODE) {
                                listener.onForkNodeClick(hit.event);
                            } else if (hit.type == HitInfo.TYPE_DIRECTION_ADD) {
                                listener.onDirectionAddClick(hit.branchId, hit.branchColor);
                            }
                            performClick();
                            return true;
                        }
                    }
                }
                isClickCandidate = false;
                return super.onTouchEvent(event);

            case MotionEvent.ACTION_CANCEL:
                isClickCandidate = false;
                return super.onTouchEvent(event);
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (columnHeaders.isEmpty()) return;

        int colCount = columnHeaders.size();
        float colW = cardWidth + cardMargin * 2;

        canvas.drawRect(0, 0, getWidth(), headerHeight, headerBgPaint);
        canvas.drawLine(0, headerHeight, getWidth(), headerHeight, borderPaint);

        for (int c = 0; c < colCount; c++) {
            ColumnHeader h = columnHeaders.get(c);
            float cx = c * colW + cardMargin;
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(h.branchColor);
            dotPaint.setStyle(Paint.Style.FILL);
            float dotR = 5f * density;
            canvas.drawCircle(cx + dotR, headerHeight / 2f, dotR, dotPaint);

            float textX = cx + dotR * 2 + 4f * density;
            Paint.FontMetrics fm = headerPaint.getFontMetrics();
            canvas.drawText(h.branchName, textX, headerHeight / 2f - (fm.ascent + fm.descent) / 2f, headerPaint);
        }

        float rowH = cardHeight + cardMargin * 2;
        for (int r = 0; r < rows.size(); r++) {
            TimelineRow row = rows.get(r);
            float rowY = headerHeight + r * rowH + cardMargin;
            switch (row.rowType) {
                case TimelineRow.TYPE_SHARED:
                    drawSharedRow(canvas, row, rowY, colCount, colW);
                    break;
                case TimelineRow.TYPE_FORK:
                    drawForkRow(canvas, row, rowY, colCount, colW);
                    break;
                case TimelineRow.TYPE_SPLIT:
                    drawSplitRow(canvas, row, rowY, colCount, colW);
                    break;
            }
        }

        drawConnectingLines(canvas, colW, rowH);
        drawDirectionAddButtons(canvas, colCount, colW, rowH);
    }

    private void drawSharedRow(Canvas canvas, TimelineRow row, float rowY, int colCount, float colW) {
        if (row.cells.isEmpty()) return;
        Cell cell = row.cells.get(0);
        if (cell == null || cell.event == null) return;
        float left = cardMargin;
        float right = colCount * colW - cardMargin;
        drawCard(canvas, new RectF(left, rowY, right, rowY + cardHeight),
                cell.event.getTitle(), cell.event.getSummary(), cell.branchColor, cell.isDirection());
    }

    private void drawForkRow(Canvas canvas, TimelineRow row, float rowY, int colCount, float colW) {
        if (row.cells.isEmpty()) return;
        Cell cell = row.cells.get(0);
        if (cell == null || cell.event == null) return;

        float left = cardMargin;
        float right = colW - cardMargin;
        drawCard(canvas, new RectF(left, rowY, right, rowY + cardHeight),
                cell.event.getTitle(), cell.event.getSummary(), cell.branchColor, cell.isDirection());

        Map<String, ForkTarget> targetMap = new HashMap<>();
        if (row.forkTargets != null) {
            for (ForkTarget ft : row.forkTargets) targetMap.put(ft.branchName, ft);
        }

        for (int c = 1; c < colCount; c++) {
            ColumnHeader h = columnHeaders.get(c);
            ForkTarget ft = targetMap.get(h.branchName);
            float colCenterX = c * colW + colW / 2f;
            float cy = rowY + cardHeight / 2f;

            if (ft != null) {
                Paint arrowLine = new Paint(Paint.ANTI_ALIAS_FLAG);
                arrowLine.setStyle(Paint.Style.STROKE);
                arrowLine.setStrokeWidth(lineWidth * 2);
                arrowLine.setColor(ft.branchColor);
                float axStart = left + cardWidth + cardMargin;
                canvas.drawLine(axStart, cy, colCenterX, cy, arrowLine);

                arrowPaint.setColor(ft.branchColor);
                Path head = new Path();
                head.moveTo(colCenterX, cy);
                head.lineTo(colCenterX - arrowSize, cy - arrowSize);
                head.lineTo(colCenterX - arrowSize, cy + arrowSize);
                head.close();
                canvas.drawPath(head, arrowPaint);

                forkLabelPaint.setColor(ft.branchColor);
                Paint.FontMetrics fm = forkLabelPaint.getFontMetrics();
                canvas.drawText(ft.branchName, colCenterX, cy - fm.descent - arrowSize - 2f * density, forkLabelPaint);
            } else {
                canvas.drawLine(colCenterX, rowY, colCenterX, rowY + cardHeight, dashLinePaint);
            }
        }
    }

    private void drawSplitRow(Canvas canvas, TimelineRow row, float rowY, int colCount, float colW) {
        int placeholderCount = 0;
        for (int c = 0; c < colCount; c++) {
            float left = c * colW + cardMargin;
            float right = (c + 1) * colW - cardMargin;
            if (c < row.cells.size() && row.cells.get(c) != null) {
                Cell cell = row.cells.get(c);
                if (cell.isPlaceholder) {
                    drawPlaceholderCard(canvas, new RectF(left, rowY, right, rowY + cardHeight),
                            cell.description, cell.branchColor);
                    placeholderCount++;
                } else if (cell.event != null) {
                    drawCard(canvas, new RectF(left, rowY, right, rowY + cardHeight),
                            cell.event.getTitle(), cell.event.getSummary(), cell.branchColor, cell.isDirection());
                } else {
                    float cx = left + (right - left) / 2f;
                    canvas.drawLine(cx, rowY, cx, rowY + cardHeight, dashLinePaint);
                }
            } else {
                float cx = left + (right - left) / 2f;
                canvas.drawLine(cx, rowY, cx, rowY + cardHeight, dashLinePaint);
            }
        }
        if (placeholderCount > 0) {
            android.util.Log.d("PlotTree", "CANVAS drawSplitRow placeholderCount=" + placeholderCount + " rowY=" + rowY);
        }
        // 分叉引导线：若此行有forkTargets（占位冲突替换FORK行场景），在主线卡片右侧绘制
        if (row.forkTargets != null && !row.forkTargets.isEmpty()) {
            Map<String, ForkTarget> targetMap = new HashMap<>();
            for (ForkTarget ft : row.forkTargets) targetMap.put(ft.branchName, ft);
            for (int c = 1; c < colCount; c++) {
                ColumnHeader h = columnHeaders.get(c);
                ForkTarget ft = targetMap.get(h.branchName);
                float colCenterX = c * colW + colW / 2f;
                float cy = rowY + cardHeight / 2f;
                if (ft != null) {
                    Paint arrowLine = new Paint(Paint.ANTI_ALIAS_FLAG);
                    arrowLine.setStyle(Paint.Style.STROKE);
                    arrowLine.setStrokeWidth(lineWidth * 2);
                    arrowLine.setColor(ft.branchColor);
                    float axStart = cardMargin + cardWidth + cardMargin;
                    canvas.drawLine(axStart, cy, colCenterX, cy, arrowLine);
                    arrowPaint.setColor(ft.branchColor);
                    Path head = new Path();
                    head.moveTo(colCenterX, cy);
                    head.lineTo(colCenterX - arrowSize, cy - arrowSize);
                    head.lineTo(colCenterX - arrowSize, cy + arrowSize);
                    head.close();
                    canvas.drawPath(head, arrowPaint);
                    forkLabelPaint.setColor(ft.branchColor);
                    Paint.FontMetrics fm = forkLabelPaint.getFontMetrics();
                    canvas.drawText(ft.branchName, colCenterX, cy - fm.descent - arrowSize - 2f * density, forkLabelPaint);
                }
            }
        }
    }

    private void drawCard(Canvas canvas, RectF rect, String title, String summary, int accentColor, boolean isDirection) {
        if (isDirection) {
            drawDirectionCard(canvas, rect, title, summary, accentColor);
            return;
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, cardBgPaint);

        RectF bar = new RectF(rect.left, rect.top + cardRadius,
                rect.left + colorBarWidth, rect.bottom - cardRadius);
        Paint barPaint = new Paint();
        barPaint.setColor(accentColor);
        barPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bar, colorBarWidth / 2f, colorBarWidth / 2f, barPaint);

        canvas.drawRoundRect(rect, cardRadius, cardRadius, cardStrokePaint);

        float textX = rect.left + colorBarWidth + cardPadding;
        float maxWidth = rect.width() - colorBarWidth - cardPadding * 2;

        String t = TextUtils.isEmpty(title) ? "未命名" : title;
        String et = ellipsize(titlePaint, t, maxWidth);
        Paint.FontMetrics tfm = titlePaint.getFontMetrics();
        float ty = rect.top + cardPadding - tfm.ascent;
        canvas.drawText(et, textX, ty, titlePaint);

        if (!TextUtils.isEmpty(summary)) {
            String es = ellipsize(summaryPaint, summary, maxWidth);
            canvas.drawText(es, textX, ty + tfm.descent - tfm.ascent + 4f * density, summaryPaint);
        }
    }

    /** 绘制方向事件卡片：虚线边框 + 半透明背景，区别于普通事件 */
    private void drawDirectionCard(Canvas canvas, RectF rect, String title, String summary, int accentColor) {
        // 半透明背景
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor((accentColor & 0x00FFFFFF) | 0x18000000);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint);

        // 左侧彩色条
        RectF bar = new RectF(rect.left, rect.top + cardRadius,
                rect.left + colorBarWidth, rect.bottom - cardRadius);
        Paint barPaint = new Paint();
        barPaint.setColor(accentColor);
        barPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bar, colorBarWidth / 2f, colorBarWidth / 2f, barPaint);

        // 虚线边框
        Paint dashStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        dashStroke.setStyle(Paint.Style.STROKE);
        dashStroke.setStrokeWidth(1f * density);
        dashStroke.setColor((accentColor & 0x00FFFFFF) | 0x80000000);
        dashStroke.setPathEffect(new DashPathEffect(new float[]{6f * density, 4f * density}, 0));
        canvas.drawRoundRect(rect, cardRadius, cardRadius, dashStroke);

        // "发展方向" 标签
        float textX = rect.left + colorBarWidth + cardPadding;
        float maxWidth = rect.width() - colorBarWidth - cardPadding * 2;
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(9f * density);
        labelPaint.setColor((accentColor & 0x00FFFFFF) | 0xAA000000);
        String label = "方向";
        canvas.drawText(label, textX, rect.top + cardPadding + 9f * density, labelPaint);

        // 标题
        float titleX = textX + labelPaint.measureText(label) + 4f * density;
        float titleMax = maxWidth - labelPaint.measureText(label) - 4f * density;
        String t = TextUtils.isEmpty(title) ? "未命名方向" : title;
        String et = ellipsize(titlePaint, t, titleMax);
        Paint.FontMetrics tfm = titlePaint.getFontMetrics();
        canvas.drawText(et, titleX, rect.top + cardPadding - tfm.ascent, titlePaint);

        // 摘要（第二行）
        if (!TextUtils.isEmpty(summary)) {
            String es = ellipsize(summaryPaint, summary, maxWidth);
            canvas.drawText(es, textX, rect.top + cardPadding - tfm.ascent + tfm.descent - tfm.ascent + 4f * density, summaryPaint);
        }
    }

    /** 在每个分支列紧跟最后一个卡片下方绘制大号"+"按钮 */
    private void drawDirectionAddButtons(Canvas canvas, int colCount, float colW, float rowH) {
        for (int c = 0; c < colCount; c++) {
            int lastRow = findLastContentRow(c);
            if (lastRow < 0) continue;
            int branchColor = columnHeaders.get(c).branchColor;
            float cx = c * colW + colW / 2f;
            float addBtnY = headerHeight + (lastRow + 1) * rowH + rowH / 2f;

            // 连接线：最后一行底部到大+按钮
            float lineTop = headerHeight + lastRow * rowH + rowH;
            canvas.drawLine(cx, lineTop, cx, addBtnY - directionAddRadius, linePaint);

            // 大+按钮圆形背景
            Paint addBg = new Paint(Paint.ANTI_ALIAS_FLAG);
            addBg.setColor((branchColor & 0x00FFFFFF) | 0x20000000);
            addBg.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, addBtnY, directionAddRadius, addBg);

            // 边框
            Paint addStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            addStroke.setStyle(Paint.Style.STROKE);
            addStroke.setStrokeWidth(1.5f * density);
            addStroke.setColor(branchColor);
            canvas.drawCircle(cx, addBtnY, directionAddRadius, addStroke);

            // "+" 文字
            TextPaint addTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            addTextPaint.setTextSize(18f * density);
            addTextPaint.setColor(branchColor);
            addTextPaint.setFakeBoldText(true);
            addTextPaint.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics afm = addTextPaint.getFontMetrics();
            canvas.drawText("+", cx, addBtnY - (afm.ascent + afm.descent) / 2f, addTextPaint);
        }
    }

    private void drawPlaceholderCard(Canvas canvas, RectF rect, String description, int branchColor) {
        // 半透明背景
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor((branchColor & 0x00FFFFFF) | 0x20000000);
        bgPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(rect, cardRadius, cardRadius, bgPaint);

        // 左侧彩色条
        RectF bar = new RectF(rect.left, rect.top + cardRadius,
                rect.left + colorBarWidth, rect.bottom - cardRadius);
        Paint barPaint = new Paint();
        barPaint.setColor(branchColor);
        barPaint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(bar, colorBarWidth / 2f, colorBarWidth / 2f, barPaint);

        // 虚线边框
        Paint dashStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        dashStroke.setStyle(Paint.Style.STROKE);
        dashStroke.setStrokeWidth(1f * density);
        dashStroke.setColor((branchColor & 0x00FFFFFF) | 0x80000000);
        dashStroke.setPathEffect(new DashPathEffect(new float[]{6f * density, 4f * density}, 0));
        canvas.drawRoundRect(rect, cardRadius, cardRadius, dashStroke);

        // "走向：" 标签
        float textX = rect.left + colorBarWidth + cardPadding;
        float maxWidth = rect.width() - colorBarWidth - cardPadding * 2;
        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTextSize(10f * density);
        labelPaint.setColor(branchColor);
        labelPaint.setFakeBoldText(true);
        canvas.drawText("走向", textX, rect.top + cardPadding + 10f * density, labelPaint);

        // 说明文字
        float descMax = maxWidth - labelPaint.measureText("走向") - 4f * density;
        float descX = textX + labelPaint.measureText("走向") + 4f * density;
        String descText = ellipsize(summaryPaint,
                TextUtils.isEmpty(description) ? "暂无" : description, descMax);
        Paint.FontMetrics dfm = summaryPaint.getFontMetrics();
        canvas.drawText(descText, descX, rect.top + cardPadding - dfm.ascent, summaryPaint);
    }

    private void drawConnectingLines(Canvas canvas, float colW, float rowH) {
        float forkNodeR = 10f * density;
        for (int r = 0; r < rows.size() - 1; r++) {
            TimelineRow cur = rows.get(r);
            TimelineRow nxt = rows.get(r + 1);
            float curBottom = headerHeight + r * rowH + rowH;
            float nxtTop = headerHeight + (r + 1) * rowH;
            for (int c = 0; c < columnHeaders.size(); c++) {
                float cx = c * colW + colW / 2f;
                if (hasColContent(cur, c) && hasColContent(nxt, c)) {
                    canvas.drawLine(cx, curBottom, cx, nxtTop, linePaint);
                }
            }
            // Draw fork node on mainline (col 0) between non-fork rows
            if (cur.rowType != TimelineRow.TYPE_FORK && nxt.rowType != TimelineRow.TYPE_FORK
                    && hasColContent(cur, 0) && hasColContent(nxt, 0)) {
                float nodeCx = colW / 2f;
                float nodeCy = curBottom;
                canvas.drawCircle(nodeCx, nodeCy, forkNodeR, forkNodeBgPaint);
                canvas.drawCircle(nodeCx, nodeCy, forkNodeR, borderPaint);
                Paint.FontMetrics fm = forkNodeTextPaint.getFontMetrics();
                canvas.drawText("+", nodeCx, nodeCy - (fm.ascent + fm.descent) / 2f, forkNodeTextPaint);
            }
        }
    }

    private boolean hasColContent(TimelineRow row, int col) {
        if (row == null) return false;
        switch (row.rowType) {
            case TimelineRow.TYPE_SHARED:
                return col == 0 && !row.cells.isEmpty()
                        && row.cells.get(0) != null && row.cells.get(0).event != null;
            case TimelineRow.TYPE_FORK:
                if (col == 0) return !row.cells.isEmpty()
                        && row.cells.get(0) != null && row.cells.get(0).event != null;
                if (col < columnHeaders.size() && row.forkTargets != null) {
                    ColumnHeader h = columnHeaders.get(col);
                    for (ForkTarget ft : row.forkTargets) {
                        if (ft.branchName.equals(h.branchName)) return true;
                    }
                }
                return false;
            case TimelineRow.TYPE_SPLIT:
                if (col < row.cells.size() && row.cells.get(col) != null) {
                    Cell c = row.cells.get(col);
                    if (c.event != null || c.isPlaceholder) return true;
                }
                // 也检查forkTargets（SPLIT行替换FORK行时携带的引导线目标）
                if (col < columnHeaders.size() && row.forkTargets != null) {
                    ColumnHeader h = columnHeaders.get(col);
                    for (ForkTarget ft : row.forkTargets) {
                        if (ft.branchName.equals(h.branchName)) return true;
                    }
                }
                return false;
        }
        return false;
    }

    /** 检查指定行列是否有方向事件卡片 */
    private boolean isDirectionRow(TimelineRow row, int col) {
        if (row == null) return false;
        switch (row.rowType) {
            case TimelineRow.TYPE_SHARED:
                if (col == 0 && !row.cells.isEmpty() && row.cells.get(0) != null) {
                    return row.cells.get(0).isDirection();
                }
                return false;
            case TimelineRow.TYPE_FORK:
                if (col == 0 && !row.cells.isEmpty() && row.cells.get(0) != null) {
                    return row.cells.get(0).isDirection();
                }
                return false;
            case TimelineRow.TYPE_SPLIT:
                if (col < row.cells.size() && row.cells.get(col) != null) {
                    Cell c = row.cells.get(col);
                    return c.event != null && c.isDirection();
                }
                return false;
        }
        return false;
    }

    private PlotTreeEvent getEventAt(TimelineRow row, int col) {
        if (row == null || row.cells == null || col >= row.cells.size()) return null;
        Cell cell = row.cells.get(col);
        return cell != null ? cell.event : null;
    }

    private String ellipsize(TextPaint paint, String text, float maxWidth) {
        if (TextUtils.isEmpty(text)) return "";
        if (paint.measureText(text) <= maxWidth) return text;
        String e = "...";
        float ew = paint.measureText(e);
        float avail = maxWidth - ew;
        if (avail <= 0) return e;
        for (int i = text.length(); i > 0; i--) {
            String sub = text.substring(0, i);
            if (paint.measureText(sub) <= avail) return sub + e;
        }
        return e;
    }
}
