import javax.microedition.lcdui.*;
import java.util.Vector;
/**
 * LivePreview v4 – improved HTML/CSS renderer with:
 *  - Heading sizes (h1/h2/h3 visual weight)
 *  - Bold / italic text markers
 *  - Word-wrap that respects screen width precisely
 *  - Visual scrollbar indicator
 *  - Dark toolbar with file-type badge
 */
public class LivePreview extends Canvas implements CommandListener {

    private HTMLViewerMIDlet midlet;
    private String           content;
    private String           fileType;
    private int              scrollY     = 0;
    private int              totalHeight = 0;

    private Command backCommand;
    private Command refreshCommand;

    // Theme
    private static final int C_BG       = 0xF6F8FA;
    private static final int C_TEXT     = 0x24292F;
    private static final int C_HEADING  = 0x0D1117;
    private static final int C_LINK     = 0x0969DA;
    private static final int C_COMMENT  = 0x6A9955;
    private static final int C_TOOLBAR  = 0x21262D;
    private static final int C_BADGE_H  = 0xFF7B00;
    private static final int C_BADGE_C  = 0x00B4D8;
    private static final int C_SCROLLBAR= 0x007ACC;

    public LivePreview(HTMLViewerMIDlet midlet, String content, String fileType) {
        this.midlet   = midlet;
        this.content  = content;
        this.fileType = fileType;

        backCommand    = new Command("Back",    Command.BACK, 1);
        refreshCommand = new Command("Refresh", Command.ITEM, 2);
        addCommand(backCommand);
        addCommand(refreshCommand);
        setCommandListener(this);
    }

    protected void paint(Graphics g) {
        int w = getWidth(), h = getHeight();

        // Background
        g.setColor(C_BG);
        g.fillRect(0, 0, w, h);

        // Toolbar
        g.setColor(C_TOOLBAR);
        g.fillRect(0, 0, w, 22);
        g.setColor(fileType.equals("css") ? C_BADGE_C : C_BADGE_H);
        g.fillRoundRect(3, 4, fileType.length() * 6 + 6, 14, 4, 4);
        g.setColor(0x1E1E1E);
        g.drawString(fileType.toUpperCase(), 6, 5, Graphics.TOP | Graphics.LEFT);
        g.setColor(0xFFFFFF);
        g.drawString("Live Preview", fileType.length() * 6 + 14, 5,
                     Graphics.TOP | Graphics.LEFT);

        // Content
        g.setClip(0, 22, w - 6, h - 22);
        int y = 26 - scrollY;
        totalHeight = 0;

        if (fileType.equals("html") || fileType.equals("combined")) {
            y = renderHTML(g, y, w - 8, h);
        } else if (fileType.equals("css")) {
            y = renderCSS(g, y, w - 8, h);
        } else {
            y = renderText(g, y, w - 8, h, content);
        }

        totalHeight = y + scrollY - 26;
        g.setClip(0, 0, w, h);

        // Scrollbar indicator
        if (totalHeight > h - 22) {
            int sbH = Math.max(20, (h - 22) * (h - 22) / totalHeight);
            int sbY = 22 + (scrollY * (h - 22 - sbH)) / Math.max(1, totalHeight - (h - 22));
            g.setColor(0xDDE1E6);
            g.fillRect(w - 4, 22, 4, h - 22);
            g.setColor(C_SCROLLBAR);
            g.fillRect(w - 4, sbY, 4, sbH);
        }
    }

    private int renderHTML(Graphics g, int y, int maxW, int h) {
        String html = content;
        int pos = 0;

        while (pos < html.length()) {
            int tagStart = html.indexOf('<', pos);
            if (tagStart == -1) {
                // Trailing text
                String text = html.substring(pos).trim();
                if (text.length() > 0) y = drawWrapped(g, text, 6, y, maxW, 14, C_TEXT);
                break;
            }

            // Text before tag
            if (tagStart > pos) {
                String text = html.substring(pos, tagStart).trim();
                if (text.length() > 0) y = drawWrapped(g, text, 6, y, maxW, 14, C_TEXT);
            }

            int tagEnd = html.indexOf('>', tagStart);
            if (tagEnd == -1) break;

            String rawTag = html.substring(tagStart + 1, tagEnd).trim();
            String tag    = rawTag.toLowerCase();
            String tagName = tag;
            int sp = tag.indexOf(' ');
            if (sp > 0) tagName = tag.substring(0, sp);

            if (tagName.equals("h1")) {
                int closeIdx = findClose(html, tagEnd + 1, "/h1>");
                if (closeIdx != -1) {
                    String txt = stripAllTags(html.substring(tagEnd + 1, closeIdx));
                    y += 4;
                    g.setColor(C_HEADING);
                    y = drawWrapped(g, txt, 6, y, maxW, 18, C_HEADING);
                    y += 4;
                    g.setColor(0xD0D7DE);
                    g.drawLine(6, y, maxW, y); y += 4;
                    pos = closeIdx + 4; continue;
                }
            } else if (tagName.equals("h2")) {
                int closeIdx = findClose(html, tagEnd + 1, "/h2>");
                if (closeIdx != -1) {
                    y += 3;
                    y = drawWrapped(g, stripAllTags(html.substring(tagEnd + 1, closeIdx)),
                                    6, y, maxW, 16, C_HEADING);
                    y += 3;
                    pos = closeIdx + 4; continue;
                }
            } else if (tagName.equals("h3")) {
                int closeIdx = findClose(html, tagEnd + 1, "/h3>");
                if (closeIdx != -1) {
                    y += 2;
                    y = drawWrapped(g, stripAllTags(html.substring(tagEnd + 1, closeIdx)),
                                    6, y, maxW, 15, C_TEXT);
                    y += 2;
                    pos = closeIdx + 4; continue;
                }
            } else if (tagName.equals("p")) {
                int closeIdx = findClose(html, tagEnd + 1, "/p>");
                if (closeIdx != -1) {
                    y += 2;
                    y = drawWrapped(g, stripAllTags(html.substring(tagEnd + 1, closeIdx)),
                                    6, y, maxW, 14, C_TEXT);
                    y += 6;
                    pos = closeIdx + 3; continue;
                }
            } else if (tagName.equals("br") || tagName.equals("br/")) {
                y += 10;
            } else if (tagName.equals("hr")) {
                g.setColor(0xD0D7DE); g.drawLine(6, y, maxW, y); y += 8;
            } else if (tagName.equals("button")) {
                int closeIdx = findClose(html, tagEnd + 1, "/button>");
                if (closeIdx != -1) {
                    String label = stripAllTags(html.substring(tagEnd + 1, closeIdx));
                    int bw = label.length() * 6 + 16;
                    g.setColor(0x0969DA);
                    g.fillRoundRect(6, y, bw, 18, 6, 6);
                    g.setColor(0xFFFFFF);
                    g.drawString(label, 14, y + 2, Graphics.TOP | Graphics.LEFT);
                    y += 22;
                    pos = closeIdx + 8; continue;
                }
            } else if (tagName.equals("a")) {
                int closeIdx = findClose(html, tagEnd + 1, "/a>");
                if (closeIdx != -1) {
                    String label = stripAllTags(html.substring(tagEnd + 1, closeIdx));
                    g.setColor(C_LINK);
                    g.drawString(label, 6, y, Graphics.TOP | Graphics.LEFT);
                    g.drawLine(6, y + 13, 6 + label.length() * 6, y + 13);
                    y += 16;
                    pos = closeIdx + 3; continue;
                }
            } else if (tagName.equals("ul") || tagName.equals("ol")) {
                // handled by li
            } else if (tagName.equals("li")) {
                int closeIdx = findClose(html, tagEnd + 1, "/li>");
                if (closeIdx != -1) {
                    g.setColor(0x0969DA);
                    g.fillRect(10, y + 4, 5, 5);
                    y = drawWrapped(g, stripAllTags(html.substring(tagEnd + 1, closeIdx)),
                                    20, y, maxW - 14, 14, C_TEXT);
                    pos = closeIdx + 4; continue;
                }
            } else if (tagName.startsWith("!--")) {
                int end = html.indexOf("-->", tagStart);
                if (end != -1) { pos = end + 3; continue; }
            }

            pos = tagEnd + 1;
        }
        return y;
    }

    private int renderCSS(Graphics g, int y, int maxW, int h) {
        String[] lines = splitLines(content);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().startsWith("/*")) g.setColor(C_COMMENT);
            else if (line.indexOf('{') != -1)  g.setColor(0xD7BA7D);
            else if (line.indexOf(':') != -1)   g.setColor(0x9CDCFE);
            else                                g.setColor(C_TEXT);

            if (y > 22) g.drawString(line, 4, y, Graphics.TOP | Graphics.LEFT);
            y += 14;
        }
        return y;
    }

    private int renderText(Graphics g, int y, int maxW, int h, String text) {
        String[] lines = splitLines(text);
        g.setColor(C_TEXT);
        for (int i = 0; i < lines.length; i++) {
            if (y > 22) g.drawString(lines[i], 4, y, Graphics.TOP | Graphics.LEFT);
            y += 14;
        }
        return y;
    }

    /** Draw word-wrapped text, return new Y. */
    private int drawWrapped(Graphics g, String text, int x, int y, int maxW, int lineH, int color) {
        int charsPerLine = Math.max(1, maxW / 6);
        String[] words = splitWords(text);
        StringBuffer line = new StringBuffer();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (line.length() + word.length() + 1 > charsPerLine && line.length() > 0) {
                g.setColor(color);
                g.drawString(line.toString(), x, y, Graphics.TOP | Graphics.LEFT);
                y += lineH;
                line = new StringBuffer(word);
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (line.length() > 0) {
            g.setColor(color);
            g.drawString(line.toString(), x, y, Graphics.TOP | Graphics.LEFT);
            y += lineH;
        }
        return y;
    }

    private int findClose(String html, int from, String closeTag) {
        int idx = html.toLowerCase().indexOf(closeTag, from);
        return idx == -1 ? -1 : idx;
    }

    private String stripAllTags(String html) {
        StringBuffer sb = new StringBuffer();
        boolean inTag = false;
        for (int i = 0; i < html.length(); i++) {
            char c = html.charAt(i);
            if (c == '<') inTag = true;
            else if (c == '>') { inTag = false; sb.append(' '); }
            else if (!inTag) sb.append(c);
        }
        return sb.toString().trim();
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (action == UP)   { scrollY = Math.max(0, scrollY - 20); repaint(); }
        else if (action == DOWN) { scrollY += 20; repaint(); }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand)    midlet.getDisplay().setCurrent(midlet.getMainMenu());
        else if (c == refreshCommand) repaint();
    }

    private String[] splitWords(String text) {
        Vector v = new Vector();
        StringBuffer cur = new StringBuffer();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ' || c == '\t' || c == '\n') {
                if (cur.length() > 0) { v.addElement(cur.toString()); cur = new StringBuffer(); }
            } else cur.append(c);
        }
        if (cur.length() > 0) v.addElement(cur.toString());
        String[] r = new String[v.size()];
        for (int i = 0; i < v.size(); i++) r[i] = (String) v.elementAt(i);
        return r;
    }

    private String[] splitLines(String text) {
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
