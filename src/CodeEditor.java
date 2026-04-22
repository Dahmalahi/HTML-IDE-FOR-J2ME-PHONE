import javax.microedition.lcdui.*;
import java.util.Vector;

/**
 * CodeEditor v4 – full-screen code editor with:
 *  - Syntax highlighting (HTML / CSS / JS / combined)
 *  - Undo / Redo via UndoManager
 *  - SnippetLibrary integration (replaces old TemplateManager)
 *  - Richer status bar showing cursor, file type badge, undo indicator
 *  - Line-number gutter with soft alternating row tint
 *  - Horizontal scroll for long lines
 *  - Key shortcut overlay (press * to toggle)
 */
public class CodeEditor extends Canvas implements CommandListener {

    private HTMLViewerMIDlet midlet;
    private HTMLEditor       parentEditor;
    private String           content;
    private String           fileType;
    private int              scrollY      = 0;
    private int              scrollX      = 0;
    private int              selectedLine = 0;
    private Vector           lines;
    private String           statusMessage = "7=Line 5=All 9=New 0=Del *=Keys";
    private boolean          showKeyHelp   = false;

    // Undo/Redo
    private UndoManager undo = new UndoManager();

    // Commands
    private Command saveCmd;
    private Command backCmd;
    private Command snippetsCmd;
    private Command previewCmd;
    private Command formatCmd;
    private Command findCmd;
    private Command undoCmd;
    private Command editSaveCmd;
    private Command editCancelCmd;

    private TextBox editBox;
    private int     editingLineIndex = -1;
    private boolean editAllMode      = false;

    // ----------------------------------------------------------------
    // Theme colours (VS Code dark inspired)
    // ----------------------------------------------------------------
    private static final int C_BG          = 0x1E1E1E;
    private static final int C_BG_ALT      = 0x252526;
    private static final int C_LINE_HL     = 0x2D2D30;
    private static final int C_GUTTER      = 0x252526;
    private static final int C_GUTTER_NUM  = 0x858585;
    private static final int C_TITLE       = 0x007ACC;
    private static final int C_TITLE2      = 0x00B4D8;
    private static final int C_STATUS      = 0x21262D;
    private static final int C_WHITE       = 0xFFFFFF;
    private static final int C_HINT        = 0x8B949E;

    // HTML colours
    private static final int C_TAG         = 0x569CD6;
    private static final int C_ATTR        = 0x9CDCFE;
    private static final int C_STRING      = 0xCE9178;
    private static final int C_COMMENT     = 0x6A9955;
    private static final int C_TEXT        = 0xD4D4D4;

    // CSS colours
    private static final int C_SELECTOR    = 0xD7BA7D;
    private static final int C_PROPERTY    = 0x9CDCFE;
    private static final int C_VALUE       = 0xCE9178;
    private static final int C_BRACE       = 0xFFD700;

    // JS colours
    private static final int C_KW          = 0x569CD6;
    private static final int C_JS_STR      = 0xCE9178;
    private static final int C_JS_CMT      = 0x6A9955;
    private static final int C_JS_FN       = 0xDCDCAA;
    private static final int C_JS_NUM      = 0xB5CEA8;
    private static final int C_JS_TXT      = 0xD4D4D4;

    private static final String[] JS_KEYWORDS = {
        "function","var","let","const","if","else","for","while","return",
        "true","false","null","undefined","new","this","document","window",
        "console","switch","case","break","continue","typeof","instanceof",
        "class","extends","import","export","default","try","catch","finally"
    };

    // File-type badge colours
    private static final int C_BADGE_HTML = 0xFF7B00;
    private static final int C_BADGE_CSS  = 0x00B4D8;
    private static final int C_BADGE_JS   = 0xF0DB4F;
    private static final int C_BADGE_COMB = 0x3FB950;

    public CodeEditor(HTMLViewerMIDlet midlet, String content,
                      String fileType, HTMLEditor parentEditor) {
        this.midlet       = midlet;
        this.content      = content;
        this.fileType     = fileType;
        this.parentEditor = parentEditor;
        this.lines        = new Vector();

        parseLines();
        undo.push(content);

        saveCmd        = new Command("Save",         Command.OK,     1);
        backCmd        = new Command("Back",         Command.BACK,   2);
        snippetsCmd    = new Command("Snippets",     Command.SCREEN, 3);
        previewCmd     = new Command("Preview",      Command.SCREEN, 4);
        formatCmd      = new Command("Format Code",  Command.SCREEN, 5);
        findCmd        = new Command("Find/Replace", Command.SCREEN, 6);
        undoCmd        = new Command("Undo",         Command.SCREEN, 7);
        editSaveCmd    = new Command("Apply",        Command.OK,     1);
        editCancelCmd  = new Command("Cancel",       Command.CANCEL, 2);

        addCommand(saveCmd);
        addCommand(backCmd);
        addCommand(snippetsCmd);
        addCommand(previewCmd);
        addCommand(formatCmd);
        addCommand(findCmd);
        addCommand(undoCmd);
        setCommandListener(this);
    }

    // ----------------------------------------------------------------
    // Paint
    // ----------------------------------------------------------------
    protected void paint(Graphics g) {
        int w = getWidth(), h = getHeight();

        g.setColor(C_BG);
        g.fillRect(0, 0, w, h);

        drawTitleBar(g, w);

        g.setColor(C_GUTTER);
        g.fillRect(0, 20, 32, h - 40);
        g.setColor(0x3E4451);
        g.drawLine(32, 20, 32, h - 20);

        int y = 25 - scrollY;
        for (int i = 0; i < lines.size(); i++) {
            String line = (String) lines.elementAt(i);
            if (y > 19 && y < h - 20) {
                if (i % 2 == 0) {
                    g.setColor(0x1A1A1A);
                    g.fillRect(33, y - 1, w - 33, 14);
                }
                if (i == selectedLine) {
                    g.setColor(C_LINE_HL);
                    g.fillRect(33, y - 1, w - 33, 14);
                    g.setColor(C_TITLE);
                    g.fillRect(33, y - 1, 2, 14);
                }
                g.setColor(i == selectedLine ? 0xC6C6C6 : C_GUTTER_NUM);
                String num = String.valueOf(i + 1);
                g.drawString(num, 30, y, Graphics.TOP | Graphics.RIGHT);
                drawLine(g, line, 37 - scrollX, y);
            }
            y += 14;
        }

        drawStatusBar(g, w, h);

        if (showKeyHelp) drawKeyHelp(g, w, h);
    }

    private void drawTitleBar(Graphics g, int w) {
        g.setColor(0x0D1117);
        g.fillRect(0, 0, w, 20);
        g.setColor(C_TITLE);
        g.fillRect(0, 0, w, 18);

        int badgeColor = C_BADGE_HTML;
        String typeLabel = "HTML";
        if (fileType.equals("css"))      { badgeColor = C_BADGE_CSS;  typeLabel = "CSS"; }
        else if (fileType.equals("js"))  { badgeColor = C_BADGE_JS;   typeLabel = "JS"; }
        else if (fileType.equals("combined")) { badgeColor = C_BADGE_COMB; typeLabel = "HTML+CSS+JS"; }

        g.setColor(badgeColor);
        g.fillRoundRect(3, 3, typeLabel.length() * 6 + 6, 14, 4, 4);
        g.setColor(0x1E1E1E);
        g.drawString(typeLabel, 6, 4, Graphics.TOP | Graphics.LEFT);

        g.setColor(C_WHITE);
        g.drawString("Editor", typeLabel.length() * 6 + 14, 3, Graphics.TOP | Graphics.LEFT);

        if (undo.canUndo()) {
            g.setColor(0xF0DB4F);
            g.drawString("*", w - 10, 3, Graphics.TOP | Graphics.LEFT);
        }
    }

    private void drawStatusBar(Graphics g, int w, int h) {
        g.setColor(C_STATUS);
        g.fillRect(0, h - 20, w, 20);
        g.setColor(C_TITLE2);
        g.drawLine(0, h - 20, w, h - 20);

        g.setColor(0xFFFFFF);
        g.drawString("Ln:" + (selectedLine + 1) + "/" + lines.size(), 3, h - 17,
                     Graphics.TOP | Graphics.LEFT);

        g.setColor(C_HINT);
        g.drawString(statusMessage, 60, h - 17, Graphics.TOP | Graphics.LEFT);
    }

    private void drawKeyHelp(Graphics g, int w, int h) {
        int bw = w - 16, bh = 120;
        int bx = 8, by = (h - bh) / 2;
        g.setColor(0x0D1117);
        g.fillRoundRect(bx, by, bw, bh, 10, 10);
        g.setColor(C_TITLE);
        g.drawRoundRect(bx, by, bw, bh, 10, 10);

        g.setColor(C_TITLE2);
        g.drawString("Key Shortcuts", bx + 6, by + 4, Graphics.TOP | Graphics.LEFT);
        g.setColor(C_HINT);
        g.drawLine(bx + 4, by + 16, bx + bw - 4, by + 16);

        String[] keys = {
            "UP/DN   Move line",
            "LEFT/RT Scroll horiz",
            "5       Edit all",
            "7       Edit line",
            "9       New line below",
            "0       Delete line",
            "*       Toggle this help"
        };
        g.setColor(C_TEXT);
        for (int i = 0; i < keys.length; i++) {
            g.drawString(keys[i], bx + 6, by + 20 + i * 13, Graphics.TOP | Graphics.LEFT);
        }
    }

    // ----------------------------------------------------------------
    // Line dispatcher
    // ----------------------------------------------------------------
    private void drawLine(Graphics g, String line, int x, int y) {
        if      (fileType.equals("css"))  drawCSSLine(g, line, x, y);
        else if (fileType.equals("js"))   drawJSLine(g, line, x, y);
        else                              drawHTMLLine(g, line, x, y);
    }

    // ----------------------------------------------------------------
    // HTML syntax highlighter
    // ----------------------------------------------------------------
    private void drawHTMLLine(Graphics g, String line, int x, int y) {
        if (fileType.equals("combined")) {
            String tr = line.trim();
            boolean css = (tr.indexOf(':') != -1 && !tr.startsWith("<"))
                         || tr.indexOf('{') != -1 || tr.indexOf('}') != -1;
            boolean js  = tr.startsWith("function") || tr.startsWith("var ")
                         || tr.startsWith("//")      || tr.startsWith("const ")
                         || tr.startsWith("let ")    || tr.startsWith("if (")
                         || tr.startsWith("return")  || tr.startsWith("document")
                         || tr.startsWith("window")  || tr.startsWith("console");
            if (css && !tr.startsWith("<")) { drawCSSLine(g, line, x, y); return; }
            if (js)                         { drawJSLine(g, line, x, y);  return; }
        }

        int pos = 0;
        boolean inTag = false, inStr = false, inCmt = false;

        while (pos < line.length()) {
            char c = line.charAt(pos);

            if (!inStr && !inCmt && pos + 3 < line.length()
                    && line.substring(pos, pos + 4).equals("<!--")) inCmt = true;

            if (inCmt) {
                g.setColor(C_COMMENT);
                if (pos + 2 < line.length() && line.substring(pos, pos + 3).equals("-->")) {
                    for (int i = 0; i < 3 && pos < line.length(); i++, pos++) {
                        g.drawChar(line.charAt(pos), x, y, Graphics.TOP | Graphics.LEFT);
                        x += 6;
                    }
                    inCmt = false; continue;
                }
            } else if (c == '<') { inTag = true;  g.setColor(C_TAG);    }
            else if (c == '>')   {                 g.setColor(C_TAG);    }
            else if (inTag && c == '"') { inStr = !inStr; g.setColor(C_STRING); }
            else if (inTag && inStr)    g.setColor(C_STRING);
            else if (inTag)             g.setColor(C_ATTR);
            else                        g.setColor(C_TEXT);

            g.drawChar(c, x, y, Graphics.TOP | Graphics.LEFT); x += 6;
            if (c == '>') { inTag = false; inStr = false; }
            pos++;
        }
    }

    // ----------------------------------------------------------------
    // CSS syntax highlighter
    // ----------------------------------------------------------------
    private void drawCSSLine(Graphics g, String line, int x, int y) {
        boolean inStr = false, inCmt = false, inBrace = false;
        int colonPos = line.indexOf(':');

        for (int pos = 0; pos < line.length(); pos++) {
            char c = line.charAt(pos);

            if (!inStr && pos + 1 < line.length()
                    && line.substring(pos, pos + 2).equals("/*")) inCmt = true;
            if (inCmt && pos + 1 < line.length()
                    && line.substring(pos, pos + 2).equals("*/")) {
                g.setColor(C_COMMENT);
                g.drawChar(c, x, y, Graphics.TOP | Graphics.LEFT); x += 6;
                g.drawChar(line.charAt(pos + 1), x, y, Graphics.TOP | Graphics.LEFT); x += 6;
                pos++; inCmt = false; continue;
            }

            if      (inCmt)                { g.setColor(C_COMMENT);  }
            else if (c == '{' || c == '}') { g.setColor(C_BRACE);    inBrace = (c == '{'); }
            else if (c == '"' || c == '\'') { inStr = !inStr;        g.setColor(C_STRING); }
            else if (inStr)                { g.setColor(C_VALUE);    }
            else if (inBrace && colonPos != -1 && pos < colonPos) { g.setColor(C_PROPERTY); }
            else if (inBrace)              { g.setColor(C_VALUE);    }
            else                           { g.setColor(C_SELECTOR); }

            g.drawChar(c, x, y, Graphics.TOP | Graphics.LEFT); x += 6;
        }
    }

    // ----------------------------------------------------------------
    // JS syntax highlighter FIXED: word variable was missing
    // ----------------------------------------------------------------
    private void drawJSLine(Graphics g, String line, int x, int y) {
        if (line.trim().startsWith("//")) {
            g.setColor(C_JS_CMT);
            g.drawString(line, x, y, Graphics.TOP | Graphics.LEFT);
            return;
        }

        int pos = 0;
        while (pos < line.length()) {
            char c = line.charAt(pos);

            // String literals
            if (c == '"' || c == '\'' || c == '`') {
                char q = c;
                g.setColor(C_JS_STR);
                g.drawChar(c, x, y, Graphics.TOP | Graphics.LEFT); x += 6; pos++;
                while (pos < line.length() && line.charAt(pos) != q) {
                    g.drawChar(line.charAt(pos), x, y, Graphics.TOP | Graphics.LEFT);
                    x += 6; pos++;
                }
                if (pos < line.length()) {
                    g.drawChar(line.charAt(pos), x, y, Graphics.TOP | Graphics.LEFT);
                    x += 6; pos++;
                }
                continue;
            }

            // Keywords / identifiers / numbers
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_') {
                // ── FIX: collect the word into a String variable ──
                int end = pos;
                while (end < line.length() &&
                       ((line.charAt(end) >= 'A' && line.charAt(end) <= 'Z') ||
                        (line.charAt(end) >= 'a' && line.charAt(end) <= 'z') ||
                        (line.charAt(end) >= '0' && line.charAt(end) <= '9') ||
                         line.charAt(end) == '_')) {
                    end++;
                }
                // 'word' is now properly declared
                String word = line.substring(pos, end);

                boolean isKW = false;
                for (int k = 0; k < JS_KEYWORDS.length; k++) {
                    if (JS_KEYWORDS[k].equals(word)) { isKW = true; break; }
                }
                boolean isFn  = (end < line.length() && line.charAt(end) == '(');
                boolean isNum = false;
                try { Double.parseDouble(word); isNum = true; } catch (Exception e) {}

                if      (isKW)  g.setColor(C_KW);
                else if (isFn)  g.setColor(C_JS_FN);
                else if (isNum) g.setColor(C_JS_NUM);
                else            g.setColor(C_JS_TXT);

                g.drawString(word, x, y, Graphics.TOP | Graphics.LEFT);
                x += word.length() * 6;
                pos = end;
                continue;
            }

            // Digits
            if (c >= '0' && c <= '9') {
                g.setColor(C_JS_NUM);
            } else {
                g.setColor(C_JS_TXT);
            }
            g.drawChar(c, x, y, Graphics.TOP | Graphics.LEFT); x += 6; pos++;
        }
    }

    // ----------------------------------------------------------------
    // Key handling
    // ----------------------------------------------------------------
    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);

        if (action == UP) {
            if (selectedLine > 0) {
                selectedLine--;
                if (selectedLine * 14 < scrollY) scrollY = Math.max(0, scrollY - 14);
            }
            statusMessage = "Ln " + (selectedLine + 1);
            repaint();
        } else if (action == DOWN) {
            if (selectedLine < lines.size() - 1) {
                selectedLine++;
                if ((selectedLine + 1) * 14 - scrollY > getHeight() - 44)
                    scrollY += 14;
            }
            statusMessage = "Ln " + (selectedLine + 1);
            repaint();
        } else if (action == LEFT) {
            scrollX = Math.max(0, scrollX - 12);
            repaint();
        } else if (action == RIGHT) {
            scrollX += 12;
            repaint();
        } else if (keyCode == KEY_NUM5) {
            openEditAllBox();
        } else if (keyCode == KEY_NUM7) {
            openEditLineBox(selectedLine);
        } else if (keyCode == KEY_NUM9) {
            lines.insertElementAt("", selectedLine + 1);
            selectedLine++;
            pushUndoSnapshot();
            statusMessage = "New line";
            repaint();
        } else if (keyCode == KEY_NUM0) {
            if (lines.size() > 1) {
                lines.removeElementAt(selectedLine);
                if (selectedLine >= lines.size()) selectedLine = lines.size() - 1;
                pushUndoSnapshot();
                statusMessage = "Line deleted";
                repaint();
            }
        } else if (keyCode == KEY_STAR) {
            showKeyHelp = !showKeyHelp;
            repaint();
        } else if (keyCode == KEY_NUM1) {
            performUndo();
        }
    }

    private void openEditLineBox(int lineIdx) {
        editingLineIndex = lineIdx;
        editAllMode      = false;
        String current   = (String) lines.elementAt(lineIdx);
        editBox = new TextBox("Edit Line " + (lineIdx + 1), current, 500, TextField.ANY);
        editBox.addCommand(editSaveCmd);
        editBox.addCommand(editCancelCmd);
        editBox.setCommandListener(this);
        midlet.getDisplay().setCurrent(editBox);
    }

    private void openEditAllBox() {
        editAllMode      = true;
        editingLineIndex = -1;
        editBox = new TextBox("Edit All (" + fileType.toUpperCase() + ")",
                              getContent(), 10000, TextField.ANY);
        editBox.addCommand(editSaveCmd);
        editBox.addCommand(editCancelCmd);
        editBox.setCommandListener(this);
        midlet.getDisplay().setCurrent(editBox);
    }

    private void performUndo() {
        String prev = undo.undo();
        if (prev != null) {
            content = prev;
            parseLines();
            statusMessage = "Undone";
        } else {
            statusMessage = "Nothing to undo";
        }
        repaint();
    }

    // ----------------------------------------------------------------
    // Command handling
    // ----------------------------------------------------------------
    public void commandAction(Command c, Displayable d) {
        if (d == editBox) {
            if (c == editSaveCmd) {
                if (editAllMode) {
                    content = editBox.getString();
                    parseLines();
                    pushUndoSnapshot();
                    statusMessage = "Saved all";
                } else {
                    lines.setElementAt(editBox.getString(), editingLineIndex);
                    pushUndoSnapshot();
                    statusMessage = "Line updated";
                }
                editBox = null; editingLineIndex = -1; editAllMode = false;
                midlet.getDisplay().setCurrent(this); repaint();
            } else if (c == editCancelCmd) {
                editBox = null; editingLineIndex = -1; editAllMode = false;
                statusMessage = "Cancelled";
                midlet.getDisplay().setCurrent(this); repaint();
            }
        } else {
            if      (c == backCmd)     parentEditor.returnToEditor();
            else if (c == saveCmd)     saveContent();
            else if (c == snippetsCmd)
                new SnippetLibrary(midlet, this, fileType).show();
            else if (c == previewCmd)
                midlet.getDisplay().setCurrent(new LivePreview(midlet, getContent(), fileType));
            else if (c == formatCmd)   formatCode();
            else if (c == findCmd)     new FindReplace(midlet, this, getContent()).show();
            else if (c == undoCmd)     performUndo();
        }
    }

    // ----------------------------------------------------------------
    // Save callback
    // ----------------------------------------------------------------
    private void saveContent() {
        String ct = getContent();
        if      (fileType.equals("html"))     parentEditor.onHTMLEditorSave(ct);
        else if (fileType.equals("css"))      parentEditor.onCSSEditorSave(ct);
        else if (fileType.equals("js"))       parentEditor.onJSEditorSave(ct);
        else if (fileType.equals("combined")) parentEditor.onCombinedEditorSave(ct);
    }

    // ----------------------------------------------------------------
    // Parse / Format
    // ----------------------------------------------------------------
    private void parseLines() {
        lines.removeAllElements();
        StringBuffer cur = new StringBuffer();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\n') { lines.addElement(cur.toString()); cur = new StringBuffer(); }
            else if (c != '\r') cur.append(c);
        }
        if (cur.length() > 0 || lines.size() == 0) lines.addElement(cur.toString());
    }

    private void pushUndoSnapshot() {
        undo.push(getContent());
    }

    private void formatCode() {
        pushUndoSnapshot();
        if      (fileType.equals("html") || fileType.equals("combined"))
            content = formatHTML(getContent());
        else if (fileType.equals("css")) content = formatCSS(getContent());
        else if (fileType.equals("js"))  content = formatJS(getContent());
        parseLines();
        statusMessage = "Formatted";
        repaint();
    }

    private String formatHTML(String html) {
        StringBuffer f   = new StringBuffer();
        StringBuffer tag = new StringBuffer();
        int indent = 0; boolean inTag = false;
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            if (c == '<') {
                String text = tag.toString().trim();
                if (text.length() > 0 && !inTag) {
                    addIndent(f, indent); f.append(text).append('\n');
                }
                tag = new StringBuffer(); inTag = true; tag.append(c);
            } else if (c == '>') {
                tag.append(c);
                String t = tag.toString();
                if (t.startsWith("</")) indent = Math.max(0, indent - 1);
                addIndent(f, indent);
                f.append(t).append('\n');
                if (!t.startsWith("</") && !t.endsWith("/>") && !t.startsWith("<!"))
                    indent++;
                tag = new StringBuffer(); inTag = false;
            } else {
                tag.append(c);
            }
        }
        return f.toString();
    }

    private String formatCSS(String css) {
        StringBuffer f = new StringBuffer();
        int indent = 0;
        for (int i = 0; i < css.length(); i++) {
            char c = css.charAt(i);
            if      (c == '{') { f.append(" {\n"); indent++; }
            else if (c == '}') { indent = Math.max(0, indent - 1); f.append("\n}\n\n"); }
            else if (c == ';') {
                f.append(";\n");
                if (i + 1 < css.length() && css.charAt(i + 1) != '}')
                    addIndent(f, indent);
            } else if (c != '\n' && c != '\r') {
                if (f.length() == 0 || f.charAt(f.length() - 1) == '\n')
                    addIndent(f, indent);
                f.append(c);
            }
        }
        return f.toString();
    }

    private String formatJS(String js) {
        StringBuffer f = new StringBuffer();
        int indent = 0;
        for (int i = 0; i < js.length(); i++) {
            char c = js.charAt(i);
            if      (c == '{') { f.append(" {\n"); indent++; addIndent(f, indent); }
            else if (c == '}') {
                indent = Math.max(0, indent - 1);
                f.append("\n"); addIndent(f, indent); f.append("}\n");
            }
            else if (c == ';') { f.append(";\n"); addIndent(f, indent); }
            else if (c != '\n' && c != '\r') f.append(c);
        }
        return f.toString();
    }

    private void addIndent(StringBuffer sb, int level) {
        for (int i = 0; i < level * 2; i++) sb.append(' ');
    }

    // ----------------------------------------------------------------
    // Public helpers
    // ----------------------------------------------------------------
    public String getContent() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < lines.size(); i++) {
            sb.append((String) lines.elementAt(i));
            if (i < lines.size() - 1) sb.append('\n');
        }
        return sb.toString();
    }

    public void insertTemplate(String template) {
        String[] tlines = splitToLines(template);
        for (int i = 0; i < tlines.length; i++)
            lines.insertElementAt(tlines[i], selectedLine + 1 + i);
        pushUndoSnapshot();
        statusMessage = "Snippet inserted (" + tlines.length + " lines)";
        repaint();
    }

    public void setContent(String newContent) {
        pushUndoSnapshot();
        this.content = newContent;
        parseLines();
        statusMessage = "Content updated";
        repaint();
    }

    private String[] splitToLines(String text) {
        Vector v = new Vector();
        StringBuffer cur = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') { v.addElement(cur.toString()); cur = new StringBuffer(); }
            else if (c != '\r') cur.append(c);
        }
        if (cur.length() > 0) v.addElement(cur.toString());
        String[] r = new String[v.size()];
        for (int i = 0; i < v.size(); i++) r[i] = (String) v.elementAt(i);
        return r;
    }
}