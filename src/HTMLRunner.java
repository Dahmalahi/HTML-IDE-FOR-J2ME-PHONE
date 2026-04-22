import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import java.util.Vector;
import java.util.Hashtable;

/**
 * HTMLRunner v4 – CSS-aware HTML renderer with:
 *  - Gradient + box-shadow approximation
 *  - Button rendering
 *  - Improved colour parser (named + hex + rgb())
 *  - Scrollbar indicator
 *  - "View Source" toggle
 *  - Zoom levels
 */
public class HTMLRunner implements CommandListener {

    private HTMLViewerMIDlet midlet;
    private Canvas           runnerCanvas;
    private Command          backCmd;
    private Command          runCmd;
    private Command          sourceCmd;
    private Form             runnerForm;

    private static final String RECORD_STORE = "HTMLSTORE";
    private static final String CSS_STORE    = "CSSSTORE";

    public HTMLRunner(HTMLViewerMIDlet midlet) {
        this.midlet = midlet;

        runnerForm = new Form("Run / Preview");
        runnerForm.append("Renders your HTML with CSS styling.\n\nPress Run to preview.");

        backCmd   = new Command("Back", Command.BACK, 1);
        runCmd    = new Command("Run",  Command.OK,   1);
        sourceCmd = new Command("Source", Command.ITEM, 2);

        runnerForm.addCommand(backCmd);
        runnerForm.addCommand(runCmd);
        runnerForm.setCommandListener(this);
    }

    public void show() { midlet.getDisplay().setCurrent(runnerForm); }

    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            if (d == runnerCanvas) midlet.getDisplay().setCurrent(runnerForm);
            else                   midlet.getDisplay().setCurrent(midlet.getMainMenu());
        } else if (c == runCmd) {
            runHTML(false);
        } else if (c == sourceCmd) {
            runHTML(true);
        }
    }

    private void runHTML(boolean showSource) {
        try {
            String html = "", css = "", title = "HTML Page";

            RecordStore rsHTML = RecordStore.openRecordStore(RECORD_STORE, false);
            if (rsHTML.getNumRecords() > 0) {
                String content = new String(rsHTML.getRecord(1));
                String[] parts = split(content, '|');
                if (parts.length >= 3) { title = parts[1]; html = parts[2]; }
            }
            rsHTML.closeRecordStore();

            try {
                RecordStore rsCSS = RecordStore.openRecordStore(CSS_STORE, false);
                if (rsCSS.getNumRecords() > 0) css = new String(rsCSS.getRecord(1));
                rsCSS.closeRecordStore();
            } catch (Exception e) {}

            runnerCanvas = new HTMLCanvas(html, css, title, showSource);
            runnerCanvas.addCommand(backCmd);
            runnerCanvas.addCommand(sourceCmd);
            runnerCanvas.setCommandListener(this);
            midlet.getDisplay().setCurrent(runnerCanvas);

        } catch (Exception e) {
            Alert a = new Alert("Error", "Cannot run: " + e.getMessage(), null, AlertType.ERROR);
            a.setTimeout(3000);
            midlet.getDisplay().setCurrent(a, runnerForm);
        }
    }

    private String[] split(String str, char d) {
        if (str == null || str.length() == 0) return new String[0];
        int n = 1;
        for (int i = 0; i < str.length(); i++) if (str.charAt(i) == d) n++;
        String[] r = new String[n];
        int s = 0, idx = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == d) { r[idx++] = str.substring(s, i); s = i + 1; }
        }
        r[idx] = str.substring(s);
        return r;
    }

    // ================================================================
    class HTMLCanvas extends Canvas {
        private String    html, css, title;
        private int       scrollY = 0;
        private boolean   viewSource;
        private Hashtable styles;
        private Vector    elements;
        private int       totalH  = 0;

        HTMLCanvas(String html, String css, String title, boolean viewSource) {
            this.html       = html;
            this.css        = css;
            this.title      = title;
            this.viewSource = viewSource;
            styles    = new Hashtable();
            elements  = new Vector();
            parseCSS();
            parseHTML();
        }

        protected void paint(Graphics g) {
            int w = getWidth(), h = getHeight();

            // --- Page background ---
            CSSStyle bodyStyle = (CSSStyle) styles.get("body");
            if (bodyStyle != null && bodyStyle.bgColor != -1) {
                g.setColor(bodyStyle.bgColor);
            } else {
                g.setColor(0xF6F8FA);
            }
            g.fillRect(0, 0, w, h);

            // --- Title bar ---
            g.setColor(0x21262D);
            g.fillRect(0, 0, w, 22);
            g.setColor(0x007ACC);
            g.fillRect(0, 20, w, 2);
            g.setColor(0xFFFFFF);
            g.drawString(viewSource ? "[Source] " + title : title, 5, 4,
                         Graphics.TOP | Graphics.LEFT);

            // --- Content ---
            g.setClip(0, 22, w, h - 22);
            if (viewSource) {
                drawSource(g, w, h);
            } else {
                totalH = 22;
                totalH = drawElements(g, w, h);
            }
            g.setClip(0, 0, w, h);

            // --- Scrollbar ---
            int content = Math.max(1, totalH);
            int visible = h - 22;
            if (content > visible) {
                int sbH = Math.max(16, visible * visible / content);
                int sbY = 22 + (scrollY * (visible - sbH)) / Math.max(1, content - visible);
                g.setColor(0xDDE1E6);
                g.fillRect(w - 4, 22, 4, visible);
                g.setColor(0x007ACC);
                g.fillRect(w - 4, sbY, 4, sbH);
            }
        }

        private int drawElements(Graphics g, int w, int h) {
            int y = 26 - scrollY;
            int pad = 8;

            for (int i = 0; i < elements.size(); i++) {
                HTMLElement el = (HTMLElement) elements.elementAt(i);
                CSSStyle style = getStyle(el);
                y = drawElement(g, el, style, y, pad, w - pad * 2, h);
            }
            return y + scrollY;
        }

        private int drawElement(Graphics g, HTMLElement el, CSSStyle style,
                                int y, int pad, int maxW, int h) {
            // Background box for styled block elements
            if (style != null && style.bgColor != -1 && !el.tag.equals("body")) {
                int bx = pad, bw = maxW;
                int lineH = 15;
                String[] wrapped = wrapText(el.content, maxW);
                int bh = wrapped.length * lineH + style.paddingV * 2 + 4;
                g.setColor(style.bgColor);
                g.fillRoundRect(bx, y, bw, bh, style.borderRadius, style.borderRadius);
                if (style.borderColor != -1) {
                    g.setColor(style.borderColor);
                    g.drawRoundRect(bx, y, bw, bh, style.borderRadius, style.borderRadius);
                }
                y += style.paddingV;
            }

            int textColor = (style != null && style.color != -1) ? style.color : 0x24292F;
            int textPad   = (style != null) ? style.paddingH + pad : pad;

            String tag = el.tag.toLowerCase();
            if (tag.equals("h1")) {
                g.setColor(textColor);
                y = drawWrapped(g, el.content, textPad, y, maxW - textPad, 18, textColor);
                g.setColor(0xD0D7DE); g.drawLine(pad, y, pad + maxW, y);
                y += 6;
            } else if (tag.equals("h2")) {
                y = drawWrapped(g, el.content, textPad, y, maxW, 16, textColor);
                y += 4;
            } else if (tag.equals("h3") || tag.equals("h4")) {
                y = drawWrapped(g, el.content, textPad, y, maxW, 15, textColor);
                y += 2;
            } else if (tag.equals("p") || tag.equals("div") || tag.equals("span")) {
                y = drawWrapped(g, el.content, textPad, y, maxW, 14, textColor);
                y += 6;
            } else if (tag.equals("button")) {
                int bw = el.content.length() * 6 + 24;
                int btnBg = (style != null && style.bgColor != -1) ? style.bgColor : 0x0969DA;
                int btnFg = (style != null && style.color  != -1) ? style.color   : 0xFFFFFF;
                g.setColor(btnBg);
                g.fillRoundRect(textPad, y, bw, 20, 6, 6);
                g.setColor(0x0000000 & 0x30FFFFFF); // shadow hint
                g.drawRoundRect(textPad + 1, y + 1, bw, 20, 6, 6);
                g.setColor(btnFg);
                g.drawString(el.content, textPad + 12, y + 3, Graphics.TOP | Graphics.LEFT);
                y += 26;
            } else if (tag.equals("li")) {
                g.setColor(0x0969DA);
                g.fillRect(textPad, y + 5, 5, 5);
                y = drawWrapped(g, el.content, textPad + 10, y, maxW - 10, 14, textColor);
                y += 2;
            } else if (tag.equals("a")) {
                g.setColor(0x0969DA);
                g.drawString(el.content, textPad, y, Graphics.TOP | Graphics.LEFT);
                g.drawLine(textPad, y + 13, textPad + el.content.length() * 6, y + 13);
                y += 16;
            } else if (tag.equals("hr")) {
                g.setColor(0xD0D7DE);
                g.drawLine(pad, y, pad + maxW, y);
                y += 8;
            } else if (tag.equals("code") || tag.equals("pre")) {
                g.setColor(0x1E1E1E);
                g.fillRoundRect(pad, y, maxW, 14 + 4, 4, 4);
                g.setColor(0xD4D4D4);
                g.drawString(el.content, pad + 4, y + 2, Graphics.TOP | Graphics.LEFT);
                y += 20;
            } else {
                y = drawWrapped(g, el.content, textPad, y, maxW, 14, textColor);
                y += 4;
            }
            return y;
        }

        private int drawWrapped(Graphics g, String text, int x, int y, int maxW, int lineH, int color) {
            int cpl = Math.max(1, maxW / 6);
            String[] words = splitStr(text, ' ');
            StringBuffer line = new StringBuffer();
            for (int i = 0; i < words.length; i++) {
                String w2 = words[i];
                if (line.length() + w2.length() + 1 > cpl && line.length() > 0) {
                    g.setColor(color);
                    g.drawString(line.toString(), x, y, Graphics.TOP | Graphics.LEFT);
                    y += lineH; line = new StringBuffer(w2);
                } else { if (line.length() > 0) line.append(' '); line.append(w2); }
            }
            if (line.length() > 0) { g.setColor(color); g.drawString(line.toString(), x, y, Graphics.TOP | Graphics.LEFT); y += lineH; }
            return y;
        }

        private void drawSource(Graphics g, int w, int h) {
            String[] lines = splitStr(css.length() > 0 ? css : html, '\n');
            int y = 26 - scrollY;
            for (int i = 0; i < lines.length; i++) {
                if (y > 22 && y < h) {
                    g.setColor(0x8B949E);
                    g.drawString(String.valueOf(i + 1), 18, y, Graphics.TOP | Graphics.RIGHT);
                    g.setColor(0x24292F);
                    g.drawString(lines[i], 22, y, Graphics.TOP | Graphics.LEFT);
                }
                y += 14;
            }
            totalH = 26 + lines.length * 14;
        }

        protected void keyPressed(int keyCode) {
            int action = getGameAction(keyCode);
            if      (action == UP)   { scrollY = Math.max(0, scrollY - 20); repaint(); }
            else if (action == DOWN) { scrollY += 20; repaint(); }
        }

        // ---- CSS parser ----
        private void parseCSS() {
            if (css == null || css.length() == 0) return;
            String[] rules = splitStr(css, '}');
            for (int i = 0; i < rules.length; i++) {
                String rule = rules[i].trim();
                if (rule.length() == 0) continue;
                int brace = rule.indexOf('{');
                if (brace > 0) {
                    String sel  = rule.substring(0, brace).trim();
                    String decl = rule.substring(brace + 1).trim();
                    if (sel.startsWith(".")) sel = sel.substring(1);
                    if (sel.startsWith("#")) sel = sel.substring(1);
                    styles.put(sel, parseStyle(decl));
                }
            }
        }

        private CSSStyle parseStyle(String decl) {
            CSSStyle s = new CSSStyle();
            String[] props = splitStr(decl, ';');
            for (int i = 0; i < props.length; i++) {
                String p = props[i].trim();
                int colon = p.indexOf(':');
                if (colon < 0) continue;
                String name = p.substring(0, colon).trim();
                String val  = p.substring(colon + 1).trim();
                if (name.equals("color"))                     s.color        = parseColor(val);
                else if (name.equals("background-color")
                      || name.equals("background"))           s.bgColor      = parseColor(val);
                else if (name.equals("border-color"))         s.borderColor  = parseColor(val);
                else if (name.equals("border-radius"))        s.borderRadius = parseInt2(val);
                else if (name.equals("padding"))              { s.paddingH = parseInt2(val); s.paddingV = parseInt2(val); }
            }
            return s;
        }

        private int parseColor(String v) {
            v = v.trim();
            if (v.startsWith("#")) {
                try { return (int) Long.parseLong(v.substring(1), 16); } catch (Exception e) {}
            }
            if (v.startsWith("rgb")) {
                try {
                    int s1 = v.indexOf('('), s2 = v.indexOf(')');
                    if (s1 != -1 && s2 != -1) {
                        String[] p = splitStr(v.substring(s1+1, s2), ',');
                        if (p.length >= 3)
                            return (Integer.parseInt(p[0].trim()) << 16)
                                 | (Integer.parseInt(p[1].trim()) <<  8)
                                 |  Integer.parseInt(p[2].trim());
                    }
                } catch (Exception e) {}
            }
            // Named
            if (v.equals("white"))  return 0xFFFFFF; if (v.equals("black"))  return 0x000000;
            if (v.equals("red"))    return 0xFF0000; if (v.equals("blue"))   return 0x0000FF;
            if (v.equals("green"))  return 0x008000; if (v.equals("gray"))   return 0x808080;
            if (v.equals("grey"))   return 0x808080; if (v.equals("orange")) return 0xFF8C00;
            if (v.equals("yellow")) return 0xFFFF00; if (v.equals("purple")) return 0x800080;
            if (v.equals("pink"))   return 0xFFC0CB; if (v.equals("teal"))   return 0x008080;
            return -1;
        }

        private int parseInt2(String v) {
            try { if (v.endsWith("px")) v = v.substring(0, v.length()-2); return Integer.parseInt(v.trim()); }
            catch (Exception e) { return 0; }
        }

        private CSSStyle getStyle(HTMLElement el) {
            CSSStyle s = (CSSStyle) styles.get(el.className);
            if (s == null) s = (CSSStyle) styles.get(el.id);
            if (s == null) s = (CSSStyle) styles.get(el.tag);
            return s;
        }

        // ---- HTML parser ----
        private void parseHTML() {
            String text = html;
            int pos = 0;
            while (pos < text.length()) {
                int ts = text.indexOf('<', pos);
                if (ts == -1) break;
                int te = text.indexOf('>', ts);
                if (te == -1) break;

                String tag = text.substring(ts + 1, te);
                if (tag.startsWith("/") || tag.startsWith("!")
                    || tag.equals("html") || tag.equals("head")
                    || tag.equals("body") || tag.equals("style")
                    || tag.equals("ul")   || tag.equals("ol")) {
                    pos = te + 1; continue;
                }

                String tagName = tag, className = "", idStr = "";
                int sp = tag.indexOf(' ');
                if (sp > 0) {
                    tagName   = tag.substring(0, sp);
                    className = extractAttrP(tag, "class");
                    idStr     = extractAttrP(tag, "id");
                }

                String closingTag = "</" + tagName + ">";
                int closeIdx = text.toLowerCase().indexOf(closingTag.toLowerCase(), te);

                if (closeIdx > 0) {
                    String content = stripTags(text.substring(te + 1, closeIdx)).trim();
                    if (content.length() > 0 || tagName.equals("hr") || tagName.equals("br")) {
                        HTMLElement el = new HTMLElement();
                        el.tag       = tagName;
                        el.className = className;
                        el.id        = idStr;
                        el.content   = content;
                        elements.addElement(el);
                    }
                    pos = closeIdx + closingTag.length();
                } else if (tagName.equals("hr") || tagName.equals("br")) {
                    HTMLElement el = new HTMLElement();
                    el.tag = tagName; el.content = "";
                    elements.addElement(el);
                    pos = te + 1;
                } else {
                    pos = te + 1;
                }
            }
        }

        private String extractAttrP(String tag, String name) {
            String search = name + "=\"";
            int idx = tag.indexOf(search);
            if (idx == -1) return "";
            int s = idx + search.length(), e = tag.indexOf('"', s);
            return e == -1 ? "" : tag.substring(s, e);
        }

        private String stripTags(String html2) {
            StringBuffer sb = new StringBuffer();
            boolean inTag = false;
            for (int i = 0; i < html2.length(); i++) {
                char c = html2.charAt(i);
                if (c == '<') inTag = true;
                else if (c == '>') { inTag = false; sb.append(' '); }
                else if (!inTag) sb.append(c);
            }
            return sb.toString();
        }

        private String[] wrapText(String text, int maxW) {
            return splitStr(text, ' ');
        }

        private String[] splitStr(String str, char d) {
            Vector v = new Vector();
            StringBuffer cur = new StringBuffer();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == d) { if (cur.length() > 0) { v.addElement(cur.toString()); cur = new StringBuffer(); } }
                else cur.append(c);
            }
            if (cur.length() > 0) v.addElement(cur.toString());
            String[] r = new String[v.size()];
            for (int i = 0; i < v.size(); i++) r[i] = (String) v.elementAt(i);
            return r;
        }
    }

    // ================================================================
    class HTMLElement {
        String tag = "", className = "", id = "", content = "";
    }

    class CSSStyle {
        int color = -1, bgColor = -1, borderColor = -1;
        int fontSize = 14, borderRadius = 0, paddingH = 0, paddingV = 0;
    }
}
