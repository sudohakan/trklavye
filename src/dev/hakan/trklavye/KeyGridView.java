package dev.hakan.trklavye;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** On-screen Turkish Q keyboard driven by the TV remote's D-pad (focus is tracked here, not by Android). */
public class KeyGridView extends View {

    public interface Listener {
        void onKey(String label);
    }

    static final String SHIFT = "⇧", BACKSPACE = "⌫", SPACE = "boşluk", ENTER = "⏎",
            SYMBOLS = "?123", LETTERS = "abc", SWITCH = "🌐";

    private static final String[][] LETTER_ROWS = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"q", "w", "e", "r", "t", "y", "u", "ı", "o", "p", "ğ", "ü"},
            {"a", "s", "d", "f", "g", "h", "j", "k", "l", "ş", "i"},
            {SHIFT, "z", "x", "c", "v", "b", "n", "m", "ö", "ç", BACKSPACE},
            {SYMBOLS, SWITCH, ",", SPACE, ".", "@", ENTER},
    };
    private static final String[][] SYMBOL_ROWS = {
            {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
            {"!", "'", "^", "+", "%", "&", "/", "(", ")", "=", "?", "_"},
            {"-", "*", ":", ";", "\"", "<", ">", "#", "$", "€", "|"},
            {"~", "[", "]", "{", "}", "\\", "`", "´", "é", "£", BACKSPACE},
            {LETTERS, SWITCH, ",", SPACE, ".", "@", ENTER},
    };
    private static final float SPACE_WEIGHT = 4f;
    private static final int ROW_HEIGHT_DP = 52;

    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private Listener listener;
    private boolean symbols;
    private boolean shifted;
    private int row = 1;
    private int col = 0;

    public KeyGridView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(22 * density);
        setBackgroundColor(0xFF202124);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    boolean isShifted() {
        return shifted;
    }

    void setShifted(boolean value) {
        shifted = value;
        invalidate();
    }

    void toggleSymbols() {
        symbols = !symbols;
        shifted = false;
        clampFocus();
        invalidate();
    }

    private String[][] rows() {
        return symbols ? SYMBOL_ROWS : LETTER_ROWS;
    }

    String display(String label) {
        if (!shifted || label.length() != 1) {
            return label;
        }
        if (label.equals("i")) {
            return "İ";
        }
        if (label.equals("ı")) {
            return "I";
        }
        return label.toUpperCase(java.util.Locale.ROOT);
    }

    void move(int dRow, int dCol) {
        String[][] rows = rows();
        if (dRow != 0) {
            float ratio = (col + 0.5f) / rows[row].length;
            row = Math.max(0, Math.min(rows.length - 1, row + dRow));
            col = (int) (ratio * rows[row].length);
        }
        col = Math.max(0, Math.min(rows[row].length - 1, col + dCol));
        invalidate();
    }

    boolean isBackspaceFocused() {
        return BACKSPACE.equals(rows()[row][col]);
    }

    void press() {
        if (listener != null) {
            listener.onKey(rows()[row][col]);
        }
    }

    private void clampFocus() {
        String[][] rows = rows();
        row = Math.min(row, rows.length - 1);
        col = Math.min(col, rows[row].length - 1);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int height = (int) (rows().length * ROW_HEIGHT_DP * density + 16 * density);
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        String[][] rows = rows();
        float pad = 4 * density;
        float rowH = ROW_HEIGHT_DP * density;
        float top = 8 * density;
        for (int r = 0; r < rows.length; r++) {
            float total = 0;
            for (String k : rows[r]) {
                total += weight(k);
            }
            float unit = (getWidth() - 16 * density) / total;
            float x = 8 * density;
            for (int c = 0; c < rows[r].length; c++) {
                String k = rows[r][c];
                float w = weight(k) * unit;
                boolean focused = r == row && c == col;
                boolean active = k.equals(SHIFT) && shifted;
                keyPaint.setColor(focused ? 0xFF8AB4F8 : active ? 0xFF5F6368 : 0xFF3C4043);
                rect.set(x + pad, top + pad, x + w - pad, top + rowH - pad);
                canvas.drawRoundRect(rect, 8 * density, 8 * density, keyPaint);
                textPaint.setColor(focused ? 0xFF202124 : 0xFFFFFFFF);
                float baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2;
                canvas.drawText(display(k), rect.centerX(), baseline, textPaint);
                x += w;
            }
            top += rowH;
        }
    }

    private static float weight(String key) {
        return key.equals(SPACE) ? SPACE_WEIGHT : 1f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }
        String[][] rows = rows();
        float rowH = ROW_HEIGHT_DP * density;
        int r = (int) ((event.getY() - 8 * density) / rowH);
        if (r < 0 || r >= rows.length) {
            return true;
        }
        float total = 0;
        for (String k : rows[r]) {
            total += weight(k);
        }
        float unit = (getWidth() - 16 * density) / total;
        float x = 8 * density;
        for (int c = 0; c < rows[r].length; c++) {
            float w = weight(rows[r][c]) * unit;
            if (event.getX() < x + w) {
                row = r;
                col = c;
                press();
                break;
            }
            x += w;
        }
        invalidate();
        return true;
    }
}
