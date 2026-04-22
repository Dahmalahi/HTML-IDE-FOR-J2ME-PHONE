import javax.microedition.lcdui.*;

/**
 * SplashScreen - animated startup screen for HTML IDE v4.0
 * Shows a gradient logo with version info, then auto-transitions to main menu.
 */
public class SplashScreen extends Canvas implements Runnable {

    private HTMLViewerMIDlet midlet;
    private int frame       = 0;
    private boolean running = true;

    // Brand colors
    private static final int C_BG_TOP    = 0x0D1117;
    private static final int C_BG_BOT    = 0x161B22;
    private static final int C_ACCENT    = 0x007ACC;
    private static final int C_ACCENT2   = 0x00B4D8;
    private static final int C_WHITE     = 0xFFFFFF;
    private static final int C_GRAY      = 0x8B949E;
    private static final int C_ORANGE    = 0xFF7B00;
    private static final int C_GREEN     = 0x3FB950;

    public SplashScreen(HTMLViewerMIDlet midlet) {
        this.midlet = midlet;
        setFullScreenMode(true);
    }

    public void show() {
        midlet.getDisplay().setCurrent(this);
        new Thread(this).start();
    }

    public void run() {
        try {
            for (frame = 0; frame <= 30 && running; frame++) {
                repaint();
                Thread.sleep(60);
            }
            Thread.sleep(800);
        } catch (InterruptedException e) {}
        if (running) midlet.showMainMenu();
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // --- Background gradient (striped approximation) ---
        for (int y = 0; y < h; y++) {
            int t = (y * 255) / h;
            int r = lerpC((C_BG_TOP >> 16) & 0xFF, (C_BG_BOT >> 16) & 0xFF, t);
            int gv= lerpC((C_BG_TOP >>  8) & 0xFF, (C_BG_BOT >>  8) & 0xFF, t);
            int b = lerpC( C_BG_TOP        & 0xFF,  C_BG_BOT        & 0xFF,  t);
            g.setColor(r, gv, b);
            g.drawLine(0, y, w, y);
        }

        // --- Animated progress bar (bottom) ---
        int progress = (frame * w) / 30;
        g.setColor(C_BG_TOP);
        g.fillRect(0, h - 6, w, 6);
        g.setColor(C_ACCENT);
        g.fillRect(0, h - 6, progress, 3);
        g.setColor(C_ACCENT2);
        g.fillRect(0, h - 3, progress, 3);

        // --- Center content ---
        int cx = w / 2;
        int cy = h / 2;

        // Fade-in alpha simulation: draw more elements each frame
        int alpha = frame < 10 ? frame : 10;

        // Logo box
        int bx = cx - 36, by = cy - 44;
        g.setColor(C_ACCENT);
        g.fillRoundRect(bx, by, 72, 52, 10, 10);
        g.setColor(C_ACCENT2);
        g.drawRoundRect(bx, by, 72, 52, 10, 10);

        // "</>" symbol
        g.setColor(C_WHITE);
        g.drawString("< / >", cx - 15, by + 16, Graphics.TOP | Graphics.LEFT);

        // HTML . CSS . JS row
        if (alpha >= 2) {
            g.setColor(C_ORANGE);
            g.drawString("HTML", cx - 30, by + 32, Graphics.TOP | Graphics.LEFT);
            g.setColor(C_GRAY);
            g.drawString(".", cx - 3,  by + 32, Graphics.TOP | Graphics.LEFT);
            g.setColor(C_ACCENT2);
            g.drawString("CSS",  cx + 1,  by + 32, Graphics.TOP | Graphics.LEFT);
            g.setColor(C_GRAY);
            g.drawString(".", cx + 18, by + 32, Graphics.TOP | Graphics.LEFT);
            g.setColor(C_GREEN);
            g.drawString("JS",   cx + 22, by + 32, Graphics.TOP | Graphics.LEFT);
        }

        // Title
        if (alpha >= 4) {
            g.setColor(C_WHITE);
            g.drawString("HTML  IDE", cx - 28, cy + 14, Graphics.TOP | Graphics.LEFT);
        }

        // Version badge
        if (alpha >= 6) {
            g.setColor(C_ACCENT);
            g.fillRoundRect(cx - 16, cy + 28, 32, 14, 6, 6);
            g.setColor(C_WHITE);
            g.drawString("v4.0", cx - 11, cy + 30, Graphics.TOP | Graphics.LEFT);
        }

        // Tagline
        if (alpha >= 8) {
            g.setColor(C_GRAY);
            g.drawString("Mobile Web Developer", cx - 52, cy + 48, Graphics.TOP | Graphics.LEFT);
        }

        // "Loading..." dots
        if (frame > 18) {
            int dots = ((frame - 18) / 3) % 4;
            StringBuffer sb = new StringBuffer("Loading");
            for (int d = 0; d < dots; d++) sb.append('.');
            g.setColor(C_GRAY);
            g.drawString(sb.toString(), cx - 22, h - 24, Graphics.TOP | Graphics.LEFT);
        }
    }

    protected void keyPressed(int keyCode) {
        running = false;
        midlet.showMainMenu();
    }

    private int lerpC(int a, int b, int t) {
        return a + ((b - a) * t / 255);
    }
}
