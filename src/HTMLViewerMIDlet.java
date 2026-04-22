import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

/**
 * HTMLViewerMIDlet – entry point for HTML IDE v4.0
 * New in v4: SplashScreen, custom-drawn main menu, Snippet Library,
 * Undo/Redo in editor, better visual theme across all screens.
 */
public class HTMLViewerMIDlet extends MIDlet implements CommandListener {

    private Display        display;
    private Canvas         menuCanvas;
    private HTMLEditor     editor;
    private HTMLViewer     viewer;
    private HTMLRunner     runner;
    private FileManager    fileManager;
    private ProjectManager projectManager;
    private RecentFiles    recentFiles;

    private Command exitCommand;

    private static final int VIEW_HTML   = 0;
    private static final int EDIT_HTML   = 1;
    private static final int RUN_HTML    = 2;
    private static final int PROJECT_MGR = 3;
    private static final int FILE_MGR    = 4;
    private static final int RECENT      = 5;
    private static final int ABOUT       = 6;

    private static final String[] MENU_LABELS = {
        "View HTML",
        "Edit HTML / CSS / JS",
        "Run / Preview",
        "Project Manager",
        "File Manager",
        "Recent Files",
        "About"
    };
    private static final int[] ICON_COLORS = {
        0x00B4D8, 0x007ACC, 0x3FB950,
        0xFF7B00, 0xDCDCAA, 0xCE9178, 0x8B949E
    };

    public HTMLViewerMIDlet() {
        display = Display.getDisplay(this);
    }

    protected void startApp() {
        SplashScreen splash = new SplashScreen(this);
        splash.show();
    }

    public void showMainMenu() {
        if (menuCanvas == null) menuCanvas = buildMenuCanvas();
        display.setCurrent(menuCanvas);
    }

    private Canvas buildMenuCanvas() {
        return new Canvas() {
            private int selectedIdx = 0;
            private Command exitCmd  = new Command("Exit", Command.EXIT, 1);
            {
                addCommand(exitCmd);
                setCommandListener(HTMLViewerMIDlet.this);
                exitCommand = exitCmd;
            }

            protected void paint(Graphics g) {
                int w = getWidth(), h = getHeight();

                // Dark gradient background
                for (int y = 0; y < h; y++) {
                    int t  = (y * 255) / h;
                    int r  = lerp(0x0D, 0x16, t);
                    int gv = lerp(0x11, 0x1B, t);
                    int b  = lerp(0x17, 0x22, t);
                    g.setColor(r, gv, b);
                    g.drawLine(0, y, w, y);
                }

                // Header bar
                g.setColor(0x007ACC);
                g.fillRect(0, 0, w, 26);
                g.setColor(0x00B4D8);
                g.fillRect(0, 23, w, 3);
                g.setColor(0xFFFFFF);
                g.drawString("</>", 4, 5, Graphics.TOP | Graphics.LEFT);
                g.drawString("HTML IDE  v4.0", 28, 6, Graphics.TOP | Graphics.LEFT);

                // Menu items
                int itemH = 28, pad = 4, startY = 32;
                for (int i = 0; i < MENU_LABELS.length; i++) {
                    int iy  = startY + i * (itemH + pad);
                    boolean sel = (i == selectedIdx);

                    if (sel) {
                        g.setColor(0x1F3A52);
                        g.fillRoundRect(4, iy, w - 8, itemH, 6, 6);
                        g.setColor(ICON_COLORS[i]);
                        g.drawRoundRect(4, iy, w - 8, itemH, 6, 6);
                    } else {
                        g.setColor(0x21262D);
                        g.fillRoundRect(4, iy, w - 8, itemH, 6, 6);
                    }

                    g.setColor(ICON_COLORS[i]);
                    g.fillRoundRect(10, iy + 9, 8, 10, 3, 3);

                    g.setColor(sel ? 0xFFFFFF : 0xC9D1D9);
                    g.drawString(MENU_LABELS[i], 24, iy + 8, Graphics.TOP | Graphics.LEFT);

                    if (sel) {
                        g.setColor(ICON_COLORS[i]);
                        g.drawString(">", w - 14, iy + 8, Graphics.TOP | Graphics.LEFT);
                    }
                }

                // Footer hint
                g.setColor(0x21262D);
                g.fillRect(0, h - 18, w, 18);
                g.setColor(0x58A6FF);
                g.drawString("UP/DN navigate  FIRE open", 4, h - 14,
                             Graphics.TOP | Graphics.LEFT);
            }

            protected void keyPressed(int keyCode) {
                int action = getGameAction(keyCode);
                if      (action == UP)   { selectedIdx = (selectedIdx - 1 + MENU_LABELS.length) % MENU_LABELS.length; repaint(); }
                else if (action == DOWN) { selectedIdx = (selectedIdx + 1) % MENU_LABELS.length; repaint(); }
                else if (action == FIRE) { handleMenuSelection(selectedIdx); }
            }

            private int lerp(int a, int b, int t) { return a + ((b - a) * t / 255); }
        };
    }

    protected void pauseApp()  {}
    protected void destroyApp(boolean u) {}

    public void commandAction(Command c, Displayable d) {
        if (c == exitCommand) { destroyApp(true); notifyDestroyed(); }
    }

    private void handleMenuSelection(int index) {
        switch (index) {
            case VIEW_HTML:   if (viewer == null) viewer = new HTMLViewer(this); viewer.show(); break;
            case EDIT_HTML:   if (editor == null) editor = new HTMLEditor(this); editor.show(); break;
            case RUN_HTML:    if (runner == null) runner = new HTMLRunner(this); runner.show(); break;
            case PROJECT_MGR: if (projectManager == null) projectManager = new ProjectManager(this); projectManager.show(); break;
            case FILE_MGR:    if (fileManager == null) fileManager = new FileManager(this); fileManager.show(); break;
            case RECENT:      if (recentFiles == null) recentFiles = new RecentFiles(this); recentFiles.show(); break;
            case ABOUT:       showAbout(); break;
        }
    }

    private void showAbout() {
        Alert a = new Alert("About HTML IDE v4.0",
            "HTML IDE v4.0\n\n" +
            "NEW in v4:\n" +
            "- Animated Splash Screen\n" +
            "- Custom Drawn Menu UI\n" +
            "- Undo / Redo (editor)\n" +
            "- Snippet Library\n" +
            "- Improved Status Bar\n" +
            "- Line-wrap Preview\n" +
            "- Enhanced DOM Inspector\n" +
            "- Better Color Picker\n\n" +
            "Existing:\n" +
            "- Syntax Highlighting\n" +
            "- Project / File Manager\n" +
            "- JSR-75 File Access\n" +
            "- Find & Replace\n" +
            "- Live Preview",
            null, AlertType.INFO);
        a.setTimeout(Alert.FOREVER);
        display.setCurrent(a, menuCanvas);
    }

    public Display     getDisplay()  { return display; }
    public Displayable getMainMenu() { return menuCanvas; }

    public void openInEditor(String content, String fileType, String fileName) {
        if (editor == null) editor = new HTMLEditor(this);
        editor.openFile(content, fileType, fileName);
        editor.show();
    }

    public HTMLEditor getEditor() {
        if (editor == null) editor = new HTMLEditor(this);
        return editor;
    }
}
