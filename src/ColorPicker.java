import javax.microedition.lcdui.*;

/**
 * ColorPicker v4 – enhanced with:
 *  - 3 modes: Palette / RGB sliders / Named colours
 *  - Opacity / alpha slider
 *  - Recent colour history (last 6 picked)
 *  - Hex input form
 *  - Smooth gradient preview swatch
 *  - rgba() output option
 */
public class ColorPicker extends Canvas implements CommandListener {

    private HTMLViewerMIDlet midlet;
    private HTMLEditor       parentEditor;
    private int selectedR = 0, selectedG = 122, selectedB = 204, selectedA = 255;
    private int cursorX = 0, cursorY = 0;
    private int mode = 0; // 0=palette 1=sliders 2=named

    private Command backCmd;
    private Command selectCmd;
    private Command modeCmd;
    private Command hexCmd;
    private Command applyCmd;
    private Command rgbaCmd;

    private Form      hexForm;
    private TextField hexField;

    // Recent history
    private int[]    history     = new int[6];
    private int      historyCount = 0;

    // ----------------------------------------------------------------
    // Palette (30 colours, 6 cols)
    // ----------------------------------------------------------------
    private static final int[] PALETTE = {
        0xFF0000, 0xFF4500, 0xFF8C00, 0xFFA500, 0xFFD700, 0xFFFF00,
        0x9ACD32, 0x008000, 0x00FF00, 0x00FA9A, 0x00CED1, 0x00BFFF,
        0x0000FF, 0x4B0082, 0x8B008B, 0xFF00FF, 0xFF1493, 0xDC143C,
        0xFFFFFF, 0xD3D3D3, 0xA9A9A9, 0x808080, 0x404040, 0x000000,
        0xFFF8DC, 0xFFE4B5, 0xF4A460, 0xD2691E, 0x8B4513, 0x800000
    };
    private static final int COLS = 6;

    // Named colours
    private static final String[] NAMED_LABELS = {
        "Tomato","Coral","Orange","Gold","Yellow","Lime",
        "Green","Teal","Cyan","SkyBlue","Blue","Purple",
        "Magenta","Pink","White","Silver","Gray","Black"
    };
    private static final int[] NAMED_VALUES = {
        0xFF6347, 0xFF7F50, 0xFF8C00, 0xFFD700, 0xFFFF00, 0x00FF00,
        0x008000, 0x008080, 0x00FFFF, 0x87CEEB, 0x0000FF, 0x800080,
        0xFF00FF, 0xFFC0CB, 0xFFFFFF, 0xC0C0C0, 0x808080, 0x000000
    };
    private int namedCursor = 0;

    // Theme
    private static final int C_BG     = 0x0D1117;
    private static final int C_HEADER = 0x007ACC;
    private static final int C_HEADER2= 0x00B4D8;
    private static final int C_WHITE  = 0xFFFFFF;
    private static final int C_GRAY   = 0x8B949E;
    private static final int C_STATUS = 0x21262D;

    public ColorPicker(HTMLViewerMIDlet midlet, HTMLEditor parentEditor) {
        this.midlet       = midlet;
        this.parentEditor = parentEditor;

        backCmd   = new Command("Back",        Command.BACK, 1);
        selectCmd = new Command("Use Color",   Command.OK,   1);
        modeCmd   = new Command("Next Mode",   Command.ITEM, 2);
        hexCmd    = new Command("Enter Hex",   Command.ITEM, 3);
        applyCmd  = new Command("Apply",       Command.OK,   1);
        rgbaCmd   = new Command("Use rgba()",  Command.ITEM, 4);

        addCommand(backCmd);
        addCommand(selectCmd);
        addCommand(modeCmd);
        addCommand(hexCmd);
        addCommand(rgbaCmd);
        setCommandListener(this);
    }

    public void show() { midlet.getDisplay().setCurrent(this); }

    // ----------------------------------------------------------------
    // Paint
    // ----------------------------------------------------------------
    protected void paint(Graphics g) {
        int w = getWidth(), h = getHeight();

        // Background
        g.setColor(C_BG);
        g.fillRect(0, 0, w, h);

        // Header
        g.setColor(C_HEADER);
        g.fillRect(0, 0, w, 22);
        g.setColor(C_HEADER2);
        g.fillRect(0, 20, w, 2);
        String[] modeNames = { "Palette", "RGB Sliders", "Named" };
        g.setColor(C_WHITE);
        g.drawString("Color Picker – " + modeNames[mode], 5, 4,
                     Graphics.TOP | Graphics.LEFT);

        switch (mode) {
            case 0: drawPalette(g, w, h);  break;
            case 1: drawSliders(g, w, h);  break;
            case 2: drawNamed(g, w, h);    break;
        }

        // Preview swatch + hex at bottom
        drawPreviewBar(g, w, h);
    }

    private void drawPalette(Graphics g, int w, int h) {
        g.setColor(C_GRAY);
        g.drawString("D-pad navigate, FIRE=select", 5, 25, Graphics.TOP | Graphics.LEFT);

        int cellW = (w - 10) / COLS, cellH = 18, startY = 38;

        for (int i = 0; i < PALETTE.length; i++) {
            int col = i % COLS, row = i / COLS;
            int px = 5 + col * cellW, py = startY + row * (cellH + 2);
            int cr = (PALETTE[i] >> 16) & 0xFF;
            int cg = (PALETTE[i] >> 8) & 0xFF;
            int cb = PALETTE[i] & 0xFF;

            // Shadow
            g.setColor(0x0D1117);
            g.fillRect(px + 1, py + 1, cellW - 3, cellH);

            g.setColor(cr, cg, cb);
            g.fillRoundRect(px, py, cellW - 2, cellH, 3, 3);

            if (col == cursorX && row == cursorY) {
                selectedR = cr; selectedG = cg; selectedB = cb;
                g.setColor(C_WHITE);
                g.drawRoundRect(px - 1, py - 1, cellW, cellH + 1, 3, 3);
                g.setColor(C_BG);
                g.drawRoundRect(px - 2, py - 2, cellW + 2, cellH + 3, 3, 3);
            }
        }

        // History row
        drawHistoryRow(g, w, h);
    }

    private void drawSliders(Graphics g, int w, int h) {
        int barW = w - 68;
        g.setColor(C_GRAY);
        g.drawString("2/8=R  4/6=G  1/3=B  7/9=A", 4, 25,
                     Graphics.TOP | Graphics.LEFT);

        drawSlider(g, "R", selectedR, 0xFF0000, 4, 40,  barW, w);
        drawSlider(g, "G", selectedG, 0x00FF00, 4, 62,  barW, w);
        drawSlider(g, "B", selectedB, 0x0000FF, 4, 84,  barW, w);
        drawAlphaSlider(g, 4, 106, barW, w);

        drawHistoryRow(g, w, h);
    }

    private void drawSlider(Graphics g, String label, int val, int fullColor,
                            int lx, int y, int barW, int w) {
        int r = (fullColor >> 16) & 0xFF;
        int gv= (fullColor >> 8)  & 0xFF;
        int b =  fullColor        & 0xFF;

        g.setColor(r/4, gv/4, b/4);
        g.fillRoundRect(lx + 18, y, barW, 14, 4, 4);
        g.setColor(r, gv, b);
        g.fillRoundRect(lx + 18, y, (val * barW) / 255, 14, 4, 4);

        // Thumb
        int thumbX = lx + 18 + (val * barW) / 255;
        g.setColor(C_WHITE);
        g.fillRoundRect(thumbX - 3, y - 1, 6, 16, 3, 3);

        g.setColor(r, gv, b);
        g.drawString(label + ":", lx, y + 1, Graphics.TOP | Graphics.LEFT);
        g.setColor(C_WHITE);
        g.drawString(String.valueOf(val), w - 30, y + 1, Graphics.TOP | Graphics.LEFT);
    }

    private void drawAlphaSlider(Graphics g, int lx, int y, int barW, int w) {
        // Checkered background hint
        g.setColor(0x333333);
        g.fillRoundRect(lx + 18, y, barW, 14, 4, 4);

        // Gradient from transparent to opaque current colour
        for (int x = 0; x < barW; x++) {
            int alpha = (x * 255) / barW;
            int r = (selectedR * alpha) / 255 + (0x1E * (255 - alpha)) / 255;
            int gv= (selectedG * alpha) / 255 + (0x1E * (255 - alpha)) / 255;
            int b = (selectedB * alpha) / 255 + (0x1E * (255 - alpha)) / 255;
            g.setColor(r, gv, b);
            g.drawLine(lx + 18 + x, y, lx + 18 + x, y + 13);
        }

        int thumbX = lx + 18 + (selectedA * barW) / 255;
        g.setColor(C_WHITE);
        g.fillRoundRect(thumbX - 3, y - 1, 6, 16, 3, 3);

        g.setColor(C_GRAY);
        g.drawString("A:", lx, y + 1, Graphics.TOP | Graphics.LEFT);
        g.setColor(C_WHITE);
        g.drawString(String.valueOf(selectedA), w - 30, y + 1, Graphics.TOP | Graphics.LEFT);
    }

    private void drawNamed(Graphics g, int w, int h) {
        g.setColor(C_GRAY);
        g.drawString("UP/DN select, FIRE=use", 5, 25, Graphics.TOP | Graphics.LEFT);

        int cellW = (w - 10) / 3, cellH = 20, startY = 38;
        for (int i = 0; i < NAMED_VALUES.length; i++) {
            int col = i % 3, row = i / 3;
            int px = 5 + col * cellW, py = startY + row * (cellH + 3);
            int cr = (NAMED_VALUES[i] >> 16) & 0xFF;
            int cg = (NAMED_VALUES[i] >> 8)  & 0xFF;
            int cb = NAMED_VALUES[i] & 0xFF;

            g.setColor(cr, cg, cb);
            g.fillRoundRect(px, py, cellW - 4, cellH, 4, 4);

            // Label with auto contrast
            g.setColor((cr + cg + cb) > 382 ? 0x000000 : 0xFFFFFF);
            g.drawString(NAMED_LABELS[i], px + 2, py + 3, Graphics.TOP | Graphics.LEFT);

            if (i == namedCursor) {
                selectedR = cr; selectedG = cg; selectedB = cb;
                g.setColor(C_WHITE);
                g.drawRoundRect(px - 1, py - 1, cellW - 2, cellH + 2, 4, 4);
            }
        }
        drawHistoryRow(g, w, h);
    }

    private void drawHistoryRow(Graphics g, int w, int h) {
        if (historyCount == 0) return;
        int y = h - 46, cellW = 16;
        g.setColor(C_GRAY);
        g.drawString("Recent:", 3, y, Graphics.TOP | Graphics.LEFT);
        for (int i = 0; i < historyCount; i++) {
            int cr = (history[i] >> 16) & 0xFF;
            int cg = (history[i] >> 8)  & 0xFF;
            int cb = history[i]         & 0xFF;
            g.setColor(cr, cg, cb);
            g.fillRoundRect(44 + i * (cellW + 2), y, cellW, 12, 3, 3);
        }
    }

    private void drawPreviewBar(Graphics g, int w, int h) {
        // Swatch
        g.setColor(selectedR, selectedG, selectedB);
        g.fillRoundRect(5, h - 32, 48, 22, 4, 4);
        g.setColor(C_WHITE);
        g.drawRoundRect(5, h - 32, 48, 22, 4, 4);

        // Hex text
        String hex = "#" + toHex(selectedR) + toHex(selectedG) + toHex(selectedB);
        g.setColor(C_WHITE);
        g.drawString(hex, 58, h - 30, Graphics.TOP | Graphics.LEFT);

        // Alpha %
        int pct = (selectedA * 100) / 255;
        g.setColor(C_GRAY);
        g.drawString("A:" + pct + "%", 58, h - 18, Graphics.TOP | Graphics.LEFT);
    }

    // ----------------------------------------------------------------
    // Key handling
    // ----------------------------------------------------------------
    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        int step = 8;

        if (mode == 0) { // palette
            int rows = PALETTE.length / COLS;
            if (action == LEFT)  cursorX = Math.max(0,      cursorX - 1);
            if (action == RIGHT) cursorX = Math.min(COLS-1, cursorX + 1);
            if (action == UP)    cursorY = Math.max(0,      cursorY - 1);
            if (action == DOWN)  cursorY = Math.min(rows-1, cursorY + 1);
            if (action == FIRE)  pickCurrentColor();
        } else if (mode == 1) { // sliders
            if (keyCode == KEY_NUM2) selectedR = Math.min(255, selectedR + step);
            if (keyCode == KEY_NUM8) selectedR = Math.max(0,   selectedR - step);
            if (keyCode == KEY_NUM4) selectedG = Math.min(255, selectedG + step);
            if (keyCode == KEY_NUM6) selectedG = Math.max(0,   selectedG - step);
            if (keyCode == KEY_NUM1) selectedB = Math.min(255, selectedB + step);
            if (keyCode == KEY_NUM3) selectedB = Math.max(0,   selectedB - step);
            if (keyCode == KEY_NUM7) selectedA = Math.min(255, selectedA + step);
            if (keyCode == KEY_NUM9) selectedA = Math.max(0,   selectedA - step);
            if (action == FIRE)  pickCurrentColor();
        } else { // named
            if (action == UP)    namedCursor = Math.max(0, namedCursor - 1);
            if (action == DOWN)  namedCursor = Math.min(NAMED_VALUES.length - 1, namedCursor + 1);
            if (action == LEFT)  namedCursor = Math.max(0, namedCursor - 3);
            if (action == RIGHT) namedCursor = Math.min(NAMED_VALUES.length - 1, namedCursor + 3);
            if (action == FIRE)  pickCurrentColor();
        }
        repaint();
    }

    private void pickCurrentColor() {
        // Add to history
        int packed = (selectedR << 16) | (selectedG << 8) | selectedB;
        if (historyCount < 6) history[historyCount++] = packed;
        else {
            for (int i = 0; i < 5; i++) history[i] = history[i + 1];
            history[5] = packed;
        }
    }

    // ----------------------------------------------------------------
    // Command handling
    // ----------------------------------------------------------------
    public void commandAction(Command c, Displayable d) {
        if (d == hexForm) {
            if (c == applyCmd) { parseHex(hexField.getString()); midlet.getDisplay().setCurrent(this); }
            else if (c == backCmd) midlet.getDisplay().setCurrent(this);
            return;
        }
        if (c == backCmd) {
            parentEditor.returnToEditor();
        } else if (c == selectCmd) {
            pickCurrentColor();
            String hex = "#" + toHex(selectedR) + toHex(selectedG) + toHex(selectedB);
            parentEditor.onColorSelected(hex);
        } else if (c == rgbaCmd) {
            pickCurrentColor();
            String rgba = "rgba(" + selectedR + "," + selectedG + "," + selectedB
                        + "," + (selectedA / 255.0f) + ")";
            parentEditor.onColorSelected(rgba);
        } else if (c == modeCmd) {
            mode = (mode + 1) % 3;
            repaint();
        } else if (c == hexCmd) {
            showHexInput();
        }
    }

    private void showHexInput() {
        hexForm  = new Form("Enter Hex Color");
        hexField = new TextField("Hex (#RRGGBB):",
            "#" + toHex(selectedR) + toHex(selectedG) + toHex(selectedB), 8, TextField.ANY);
        hexForm.append(hexField);
        hexForm.addCommand(applyCmd);
        hexForm.addCommand(backCmd);
        hexForm.setCommandListener(this);
        midlet.getDisplay().setCurrent(hexForm);
    }

    private void parseHex(String hex) {
        try {
            if (hex.startsWith("#")) hex = hex.substring(1);
            if (hex.length() == 6) {
                selectedR = Integer.parseInt(hex.substring(0, 2), 16);
                selectedG = Integer.parseInt(hex.substring(2, 4), 16);
                selectedB = Integer.parseInt(hex.substring(4, 6), 16);
            }
        } catch (Exception e) {}
        repaint();
    }

    private String toHex(int v) {
        String h = Integer.toHexString(v).toUpperCase();
        return h.length() == 1 ? "0" + h : h;
    }
}
