import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import java.util.Vector;

public class RecentFiles implements CommandListener {
    private HTMLViewerMIDlet midlet;
    private List fileList;
    private static final String RECENT_STORE = "RECENTFILES";
    private static final int MAX_RECENT = 10;
    private static Vector recentList = new Vector();
    private Command backCommand;
    private Command openCommand;
    private Command clearCommand;
    
    public RecentFiles(HTMLViewerMIDlet midlet) {
        this.midlet = midlet;
        
        backCommand = new Command("Back", Command.BACK, 1);
        openCommand = new Command("Open", Command.OK, 1);
        clearCommand = new Command("Clear All", Command.ITEM, 2);
        
        fileList = new List("Recent Files", List.IMPLICIT);
        fileList.addCommand(backCommand);
        fileList.addCommand(openCommand);
        fileList.addCommand(clearCommand);
        fileList.setCommandListener(this);
        
        loadRecent();
    }
    
    public void show() {
        refreshList();
        midlet.getDisplay().setCurrent(fileList);
    }
    
    private void refreshList() {
        fileList.deleteAll();
        if (recentList.size() == 0) {
            fileList.append("No recent files", null);
        } else {
            for (int i = recentList.size() - 1; i >= 0; i--) {
                RecentFile rf = (RecentFile) recentList.elementAt(i);
                fileList.append(rf.name + " [" + rf.type.toUpperCase() + "]", null);
            }
        }
    }
    
    public static void addFile(String name, String type) {
        // Remove duplicate
        for (int i = recentList.size() - 1; i >= 0; i--) {
            RecentFile rf = (RecentFile) recentList.elementAt(i);
            if (rf.name.equals(name)) {
                recentList.removeElementAt(i);
            }
        }
        
        RecentFile rf = new RecentFile();
        rf.name = name;
        rf.type = type;
        recentList.addElement(rf);
        
        // Keep only last MAX_RECENT
        while (recentList.size() > MAX_RECENT) {
            recentList.removeElementAt(0);
        }
        
        saveRecentStatic();
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.getDisplay().setCurrent(midlet.getMainMenu());
        } else if (c == openCommand || c == List.SELECT_COMMAND) {
            openSelected();
        } else if (c == clearCommand) {
            recentList.removeAllElements();
            saveRecentStatic();
            refreshList();
        }
    }
    
    private void openSelected() {
        int idx = fileList.getSelectedIndex();
        int realIdx = recentList.size() - 1 - idx;
        
        if (realIdx < 0 || realIdx >= recentList.size()) return;
        
        RecentFile rf = (RecentFile) recentList.elementAt(realIdx);
        midlet.openInEditor("", rf.type, rf.name);
    }
    
    private void loadRecent() {
        try {
            RecordStore rs = RecordStore.openRecordStore(RECENT_STORE, false);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                String raw = new String(data);
                parseRecent(raw);
            }
            rs.closeRecordStore();
        } catch (Exception e) {}
    }
    
    private static void saveRecentStatic() {
        try {
            try {
                RecordStore.deleteRecordStore("RECENTFILES");
            } catch (Exception e) {}
            
            RecordStore rs = RecordStore.openRecordStore("RECENTFILES", true);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < recentList.size(); i++) {
                RecentFile rf = (RecentFile) recentList.elementAt(i);
                sb.append(rf.name).append("|").append(rf.type);
                if (i < recentList.size() - 1) sb.append("~~");
            }
            byte[] data = sb.toString().getBytes();
            rs.addRecord(data, 0, data.length);
            rs.closeRecordStore();
        } catch (Exception e) {}
    }
    
    private void parseRecent(String raw) {
        recentList.removeAllElements();
        if (raw.length() == 0) return;
        
        int pos = 0;
        while (pos < raw.length()) {
            int end = raw.indexOf("~~", pos);
            String entry = end == -1 ? raw.substring(pos) : raw.substring(pos, end);
            
            int sep = entry.indexOf('|');
            if (sep != -1) {
                RecentFile rf = new RecentFile();
                rf.name = entry.substring(0, sep);
                rf.type = entry.substring(sep + 1);
                recentList.addElement(rf);
            }
            
            if (end == -1) break;
            pos = end + 2;
        }
    }
    
    static class RecentFile {
        String name = "";
        String type = "";
    }
}