package com.example.storyteller.ui.fragment;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlotTreeCanvasView extends View {

    // ── Listener ────────────────────────────────────────────
    public interface Listener {
        void onCardClick(PlotTreeEvent event, int branchColor);
        void onForkNodeClick(PlotTreeEvent sourceEvent);
    }

    // ── Hit testing ────────────────────────────────────────
    private static class HitInfo {
        static final int TYPE_CARD = 0;
        static final int TYPE_FORK_NODE = 1;
        final RectF rect;
        final int type;
        final PlotTreeEvent event;
        final int rowIndex;
        final int branchColor;

        HitInfo(RectF rect, int type, PlotTreeEvent event, int rowIndex, int branchColor) {
            this.rect = rect;
            this.type = type;
            this.event = event;
            this.rowIndex = rowIndex;
            this.branchColor = branchColor;
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

        cardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBgPaint.setStyle(Paint.Style.FILL);
        cardBgPaint.setColor(Color.WHITE);

        cardStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardStrokePaint.setStyle(Paint.Style.STROKE);
        cardStrokePaint.setStrokeWidth(1f * density);
        cardStrokePaint.setColor(0xFFE0E0E0);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(lineWidth);
        linePaint.setColor(0xFFBDBDBD);

        dashLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dashLinePaint.setStyle(Paint.Style.STROKE);
        dashLinePaint.setStrokeWidth(lineWidth);
        dashLinePaint.setColor(0xFFE0E0E0);
        dashLinePaint.setPathEffect(new DashPathEffect(new float[]{4f * density, 4f * density}, 0));

        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setStyle(Paint.Style.FILL);

        titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setTextSize(14f * density);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(0xFF212121);

        summaryPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        summaryPaint.setTextSize(12f * density);
        summaryPaint.setColor(0xFF757575);

        headerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setTextSize(12f * density);
        headerPaint.setFakeBoldText(true);
        headerPaint.setColor(0xFF616161);

        forkLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        forkLabelPaint.setTextSize(forkLabelSize);
        forkLabelPaint.setColor(0xFF757575);

        headerBgPaint = new Paint();
        headerBgPaint.setStyle(Paint.Style.FILL);
        headerBgPaint.setColor(0xFFF5F5F5);

        forkNodeBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        forkNodeBgPaint.setStyle(Paint.Style.FILL);
        forkNodeBgPaint.setColor(0xFFF0F0F0);

        forkNodeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        forkNodeTextPaint.setTextSize(14f * density);
        forkNodeTextPaint.setFakeBoldText(true);
        forkNodeTextPaint.setColor(0xFF9E9E9E);
        forkNodeTextPaint.setTextAlign(Paint.Align.CENTER);
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
        int rowCount = rows.size();
        float colW = cardWidth + cardMargin * 2;
        float rowH = cardHeight + cardMargin * 2;
        totalWidth = (int)(colCount * colW);
        totalHeight = (int)(headerHeight + rowCount * rowH);
        setMinimumWidth(totalWidth);
        setMinimumHeight(totalHeight);
        rebuildHitRects(colCount, colW, rowH);
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
                    hitInfos.add(new HitInfo(rect, HitInfo.TYPE_CARD, cell.event, r, cell.branchColor));
                }
            } else if (row.rowType == TimelineRow.TYPE_FORK) {
                if (!row.cells.isEmpty() && row.cells.get(0) != null && row.cells.get(0).event != null) {
                    Cell cell = row.cells.get(0);
                    RectF rect = new RectF(cardMargin, rowY, colW - cardMargin, rowY + cardHeight);
                    hitInfos.add(new HitInfo(rect, HitInfo.TYPE_CARD, cell.event, r, cell.branchColor));
                }
            } else if (row.rowType == TimelineRow.TYPE_SPLIT) {
                for (int c = 0; c < colCount; c++) {
                    if (c < row.cells.size() && row.cells.get(c) != null && row.cells.get(c).event != null) {
                        Cell cell = row.cells.get(c);
                        float left = c * colW + cardMargin;
                        float right = (c + 1) * colW - cardMargin;
                        RectF rect = new RectF(left, rowY, right, rowY + cardHeight);
                        hitInfos.add(new HitInfo(rect, HitInfo.TYPE_CARD, cell.event, r, cell.branchColor));
                    }
                }
            }
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
                            } else if (hit.type == HitInfo.TYPE_FORK_NODE) {
                                listener.onForkNodeClick(hit.event);
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
        Paint hBorder = new Paint();
        hBorder.setColor(0xFFE0E0E0);
        hBorder.setStrokeWidth(1f * density);
        canvas.drawLine(0, headerHeight, getWidth(), headerHeight, hBorder);

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
    }

    private void drawSharedRow(Canvas canvas, TimelineRow row, float rowY, int colCount, float colW) {
        if (row.cells.isEmpty()) return;
        Cell cell = row.cells.get(0);
        if (cell == null || cell.event == null) return;
        float left = cardMargin;
        float right = colCount * colW - cardMargin;
        drawCard(canvas, new RectF(left, rowY, right, rowY + cardHeight),
                cell.event.getTitle(), cell.event.getSummary(), cell.branchColor);
    }

    private void drawForkRow(Canvas canvas, TimelineRow row, float rowY, int colCount, float colW) {
        if (row.cells.isEmpty()) return;
        Cell cell = row.cells.get(0);
        if (cell == null || cell.event == null) return;

        float left = cardMargin;
        float right = colW - cardMargin;
        drawCard(canvas, new RectF(left, rowY, right, rowY + cardHeight),
                cell.event.getTitle(), cell.event.getSummary(), cell.branchColor);

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
        for (int c = 0; c < colCount; c++) {
            float left = c * colW + cardMargin;
            float right = (c + 1) * colW - cardMargin;
            if (c < row.cells.size() && row.cells.get(c) != null && row.cells.get(c).event != null) {
                Cell cell = row.cells.get(c);
                drawCard(canvas, new RectF(left, rowY, right, rowY + cardHeight),
                        cell.event.getTitle(), cell.event.getSummary(), cell.branchColor);
            } else {
                float cx = left + (right - left) / 2f;
                canvas.drawLine(cx, rowY, cx, rowY + cardHeight, dashLinePaint);
            }
        }
    }

    private void drawCard(Canvas canvas, RectF rect, String title, String summary, int accentColor) {
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
                Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                strokePaint.setStyle(Paint.Style.STROKE);
                strokePaint.setColor(0xFFBDBDBD);
                strokePaint.setStrokeWidth(1f * density);
                canvas.drawCircle(nodeCx, nodeCy, forkNodeR, strokePaint);
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
                return col < row.cells.size() && row.cells.get(col) != null
                        && row.cells.get(col).event != null;
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
