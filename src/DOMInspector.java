import javax.microedition.lcdui.*;
import java.util.Vector;

/**
 * DOMInspector v4 – improved DOM tree viewer with:
 *  - Colour-coded node types (element / closing / self-closing)
 *  - Attribute panel shows all attributes for the selected node
 *  - Breadcrumb depth indicator
 *  - Search / jump to node by tag name
 *  - Cleaner visual theme matching v4 palette
 */
public class DOMInspector extends Canvas implements CommandListener {

    private HTMLViewerMIDlet midlet;
    private HTMLEditor       parentEditor;
    private String           html;
    private Vector           domTree;
    private int              scrollY      = 0;
    private int              selectedNode = 0;
    private boolean          showAttrPanel = false;

    private Command backCmd;
    private Command infoCmd;
    private Command refreshCmd;
    private Command searchCmd;

    // Search
    private Form      searchForm;
    private TextField searchField;
    private Command   doSearchCmd;
    private Command   cancelSearchCmd;

    // Theme
    private static final int C_BG         = 0x0D1117;
    private static final int C_HEADER     = 0x161B22;
    private static final int C_TITLE      = 0x007ACC;
    private static final int C_TITLE2     = 0x00B4D8;
    private static final int C_SEL        = 0x1F3A52;
    private static final int C_SEL_BORDER = 0x007ACC;
    private static final int C_STATUS     = 0x21262D;
    private static final int C_WHITE      = 0xFFFFFF;
    private static final int C_GRAY       = 0x8B949E;

    // Syntax
    private static final int C_TAG        = 0x569CD6;
    private static final int C_CLOSE_TAG  = 0x4E86B8;
    private static final int C_ATTR_NAME  = 0x9CDCFE;
    private static final int C_ATTR_VAL   = 0xCE9178;
    private static final int C_ID         = 0xDCDCAA;
    private static final int C_CLASS      = 0xB5CEA8;
    private static final int C_SELF_CLOSE = 0xA8DAFF;
    private static final int C_INDENT_LINE= 0x30363E;

    public DOMInspector(HTMLViewerMIDlet midlet, String html, HTMLEditor parentEditor) {
        this.midlet       = midlet;
        this.html         = html;
        this.parentEditor = parentEditor;
        domTree = new Vector();

        backCmd         = new Command("Back",      Command.BACK, 1);
        infoCmd         = new Command("Attrs",     Command.OK,   1);
        refreshCmd      = new Command("Refresh",   Command.ITEM, 2);
        searchCmd       = new Command("Find Tag",  Command.ITEM, 3);
        doSearchCmd     = new Command("Search",    Command.OK,   1);
        cancelSearchCmd = new Command("Cancel",    Command.BACK, 2);

        addCommand(backCmd);
        addCommand(infoCmd);
        addCommand(refreshCmd);
        addCommand(searchCmd);
        setCommandListener(this);

        parseDOM();
    }

    public void show() { midlet.getDisplay().setCurrent(this); }

    // ----------------------------------------------------------------
    // Helper – replaces the missing splitStr() call
    // Returns the first token before the delimiter character
    // e.g. firstToken("div class=\"x\"", ' ') → "div"
    // ----------------------------------------------------------------
    private String firstToken(String src, char delim) {
        if (src == null || src.length() == 0) return src;
        int idx = src.indexOf(delim);
        return idx == -1 ? src : src.substring(0, idx);
    }

    // ----------------------------------------------------------------
    // DOM Parser
    // ----------------------------------------------------------------
    private void parseDOM() {
        domTree.removeAllElements();
        int indent = 0, pos = 0;

        while (pos < html.length()) {
            int tagStart = html.indexOf('<', pos);
            if (tagStart == -1) break;
            int tagEnd = html.indexOf('>', tagStart);
            if (tagEnd == -1) break;

            String tag = html.substring(tagStart + 1, tagEnd).trim();
            if (tag.startsWith("!")) { pos = tagEnd + 1; continue; }

            DOMNode node = new DOMNode();
            node.rawTag = tag;

            if (tag.startsWith("/")) {
                indent = Math.max(0, indent - 1);
                node.tag       = tag.substring(1).toLowerCase();
                node.indent    = indent;
                node.isClosing = true;
            } else {
                node.isClosing = false;

                // ── FIX: use firstToken() instead of missing splitStr() ──
                String tagName = firstToken(tag, ' ').toLowerCase();
                // Strip trailing slash from tag name if present
                if (tagName.endsWith("/")) {
                    tagName = tagName.substring(0, tagName.length() - 1);
                }
                node.isSelfClose = tag.endsWith("/") || isSelfClosing(tagName);
                node.indent      = indent;

                int spaceIdx = tag.indexOf(' ');
                if (spaceIdx != -1) {
                    node.tag        = tag.substring(0, spaceIdx).toLowerCase();
                    node.attributes = tag.substring(spaceIdx + 1).trim();
                    if (node.attributes.endsWith("/")) {
                        node.attributes = node.attributes
                                .substring(0, node.attributes.length() - 1).trim();
                        node.isSelfClose = true;
                    }
                } else {
                    String t = tag;
                    if (t.endsWith("/")) t = t.substring(0, t.length() - 1).trim();
                    node.tag        = t.toLowerCase();
                    node.attributes = "";
                }

                node.id        = extractAttr(tag, "id");
                node.className = extractAttr(tag, "class");
                node.href      = extractAttr(tag, "href");
                node.src       = extractAttr(tag, "src");

                if (!node.isSelfClose) indent++;
            }
            domTree.addElement(node);
            pos = tagEnd + 1;
        }
    }

    private String extractAttr(String tag, String name) {
        String search = name + "=\"";
        int idx = tag.indexOf(search);
        if (idx == -1) return "";
        int start = idx + search.length();
        int end   = tag.indexOf('"', start);
        return end == -1 ? "" : tag.substring(start, end);
    }

    private boolean isSelfClosing(String t) {
        return t.equals("br")     || t.equals("img")    || t.equals("input")
            || t.equals("link")   || t.equals("meta")   || t.equals("hr")
            || t.equals("area")   || t.equals("base")   || t.equals("col")
            || t.equals("embed")  || t.equals("param")  || t.equals("source")
            || t.equals("track")  || t.equals("wbr");
    }

    // ----------------------------------------------------------------
    // Paint
    // ----------------------------------------------------------------
    protected void paint(Graphics g) {
        int w = getWidth(), h = getHeight();

        g.setColor(C_BG);
        g.fillRect(0, 0, w, h);

        // Header
        g.setColor(C_HEADER);
        g.fillRect(0, 0, w, 22);
        g.setColor(C_TITLE);
        g.fillRect(0, 20, w, 2);
        g.setColor(C_TITLE2);
        g.drawString("DOM Inspector", 5, 4, Graphics.TOP | Graphics.LEFT);
        g.setColor(C_GRAY);
        g.drawString("(" + domTree.size() + " nodes)", 84, 4,
                     Graphics.TOP | Graphics.LEFT);

        // Attr panel or node tree
        if (showAttrPanel && selectedNode >= 0 && selectedNode < domTree.size()) {
            drawAttrPanel(g, w, h);
        } else {
            drawTree(g, w, h);
        }

        // Status bar
        g.setColor(C_STATUS);
        g.fillRect(0, h - 18, w, 18);
        g.setColor(C_TITLE);
        g.drawLine(0, h - 18, w, h - 18);
        if (selectedNode >= 0 && selectedNode < domTree.size()) {
            DOMNode n = (DOMNode) domTree.elementAt(selectedNode);
            g.setColor(C_WHITE);
            g.drawString("<" + n.tag + "> d:" + n.indent, 3, h - 14,
                         Graphics.TOP | Graphics.LEFT);
            g.setColor(C_GRAY);
            g.drawString("OK=Attrs *=close", w - 80, h - 14,
                         Graphics.TOP | Graphics.LEFT);
        }
    }

    private void drawTree(Graphics g, int w, int h) {
        int y     = 25 - scrollY;
        int lineH = 15;

        for (int i = 0; i < domTree.size(); i++) {
            DOMNode node = (DOMNode) domTree.elementAt(i);
            if (y > 21 && y < h - 20) {
                // Selection highlight
                if (i == selectedNode) {
                    g.setColor(C_SEL);
                    g.fillRect(0, y - 1, w, lineH);
                    g.setColor(C_SEL_BORDER);
                    g.drawLine(0, y - 1, 0, y + lineH - 2);
                }

                int ix = 5 + node.indent * 10;

                // Indent guide lines
                for (int d = 0; d < node.indent; d++) {
                    g.setColor(C_INDENT_LINE);
                    g.drawLine(5 + d * 10 + 4, y, 5 + d * 10 + 4, y + lineH - 1);
                }

                // Expand/collapse hint dot
                if (!node.isSelfClose && !node.isClosing) {
                    g.setColor(C_TITLE);
                    g.fillRect(ix - 1, y + 5, 3, 3);
                }

                if (node.isClosing) {
                    g.setColor(C_CLOSE_TAG);
                    g.drawString("</" + node.tag + ">", ix, y,
                                 Graphics.TOP | Graphics.LEFT);
                } else {
                    // Opening tag
                    int tx = ix;
                    g.setColor(node.isSelfClose ? C_SELF_CLOSE : C_TAG);
                    g.drawString("<" + node.tag, tx, y, Graphics.TOP | Graphics.LEFT);
                    tx += (node.tag.length() + 1) * 6;

                    // id
                    if (node.id.length() > 0) {
                        g.setColor(C_ID);
                        String idStr = " #" + node.id;
                        g.drawString(idStr, tx, y, Graphics.TOP | Graphics.LEFT);
                        tx += idStr.length() * 6;
                    }
                    // class
                    if (node.className.length() > 0) {
                        g.setColor(C_CLASS);
                        String clStr = " ." + node.className;
                        g.drawString(clStr, tx, y, Graphics.TOP | Graphics.LEFT);
                        tx += clStr.length() * 6;
                    }
                    g.setColor(node.isSelfClose ? C_SELF_CLOSE : C_TAG);
                    g.drawString(node.isSelfClose ? "/>" : ">", tx, y,
                                 Graphics.TOP | Graphics.LEFT);
                }
            }
            y += lineH;
        }
    }

    private void drawAttrPanel(Graphics g, int w, int h) {
        DOMNode n = (DOMNode) domTree.elementAt(selectedNode);

        g.setColor(0x161B22);
        g.fillRect(4, 26, w - 8, h - 48);
        g.setColor(C_TITLE);
        g.drawRoundRect(4, 26, w - 8, h - 48, 6, 6);

        g.setColor(C_TITLE2);
        g.drawString("< " + n.tag + " >", 10, 30, Graphics.TOP | Graphics.LEFT);
        g.setColor(C_GRAY);
        g.drawLine(8, 44, w - 8, 44);

        int y = 48;
        drawAttrRow(g, "tag",   n.tag,                    y); y += 16;
        drawAttrRow(g, "depth", String.valueOf(n.indent),  y); y += 16;
        if (n.id.length()        > 0) { drawAttrRow(g, "id",    n.id,        y); y += 16; }
        if (n.className.length() > 0) { drawAttrRow(g, "class", n.className, y); y += 16; }
        if (n.href.length()      > 0) { drawAttrRow(g, "href",  n.href,      y); y += 16; }
        if (n.src.length()       > 0) { drawAttrRow(g, "src",   n.src,       y); y += 16; }

        if (n.attributes.length() > 0) {
            g.setColor(C_GRAY);
            g.drawString("All attrs:", 10, y, Graphics.TOP | Graphics.LEFT);
            y += 14;
            g.setColor(C_ATTR_VAL);
            String attrs = n.attributes;
            while (attrs.length() > 0) {
                int take = Math.min(attrs.length(), (w - 20) / 6);
                g.drawString(attrs.substring(0, take), 10, y,
                             Graphics.TOP | Graphics.LEFT);
                y += 14;
                attrs = attrs.substring(take);
            }
        }
        g.setColor(C_GRAY);
        g.drawString("Press OK or * to close", 10, h - 44,
                     Graphics.TOP | Graphics.LEFT);
    }

    private void drawAttrRow(Graphics g, String key, String val, int y) {
        g.setColor(C_ATTR_NAME);
        g.drawString(key + ":", 10, y, Graphics.TOP | Graphics.LEFT);
        g.setColor(C_ATTR_VAL);
        g.drawString(val, 10 + (key.length() + 1) * 6 + 4, y,
                     Graphics.TOP | Graphics.LEFT);
    }

    // ----------------------------------------------------------------
    // Key handling
    // ----------------------------------------------------------------
    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (showAttrPanel) {
            showAttrPanel = false; repaint(); return;
        }
        if (action == UP) {
            if (selectedNode > 0) {
                selectedNode--;
                if (selectedNode * 15 < scrollY) scrollY = Math.max(0, scrollY - 15);
            }
            repaint();
        } else if (action == DOWN) {
            if (selectedNode < domTree.size() - 1) {
                selectedNode++;
                if ((selectedNode + 1) * 15 - scrollY > getHeight() - 44)
                    scrollY += 15;
            }
            repaint();
        } else if (action == FIRE) {
            showAttrPanel = true; repaint();
        } else if (keyCode == KEY_STAR) {
            showAttrPanel = false; repaint();
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (d == searchForm) {
            if (c == doSearchCmd) jumpToTag(searchField.getString());
            midlet.getDisplay().setCurrent(this);
        } else {
            if      (c == backCmd)    parentEditor.returnToEditor();
            else if (c == infoCmd)    { showAttrPanel = !showAttrPanel; repaint(); }
            else if (c == refreshCmd) { parseDOM(); repaint(); }
            else if (c == searchCmd)  showSearch();
        }
    }

    private void showSearch() {
        searchForm  = new Form("Find Tag");
        searchField = new TextField("Tag name:", "", 30, TextField.ANY);
        searchForm.append(searchField);
        searchForm.addCommand(doSearchCmd);
        searchForm.addCommand(cancelSearchCmd);
        searchForm.setCommandListener(this);
        midlet.getDisplay().setCurrent(searchForm);
    }

    private void jumpToTag(String tag) {
        String tl = tag.toLowerCase().trim();
        for (int i = 0; i < domTree.size(); i++) {
            DOMNode n = (DOMNode) domTree.elementAt(i);
            if (n.tag.equals(tl)) {
                selectedNode = i;
                scrollY      = Math.max(0, i * 15 - getHeight() / 2);
                repaint();
                return;
            }
        }
        Alert a = new Alert("Not found",
                            "<" + tl + "> not found in DOM",
                            null, AlertType.WARNING);
        a.setTimeout(2000);
        midlet.getDisplay().setCurrent(a, this);
    }

    // ----------------------------------------------------------------
    // DOMNode inner class
    // ----------------------------------------------------------------
    class DOMNode {
        String  tag         = "";
        String  rawTag      = "";
        String  attributes  = "";
        String  id          = "";
        String  className   = "";
        String  href        = "";
        String  src         = "";
        int     indent      = 0;
        boolean isClosing   = false;
        boolean isSelfClose = false;
    }
}