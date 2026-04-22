import javax.microedition.lcdui.*;
import javax.microedition.rms.*;

public class HTMLViewer implements CommandListener {
    private HTMLViewerMIDlet midlet;
    private Form viewerForm;
    private Command backCommand;
    private Command refreshCommand;
    
    private static final String RECORD_STORE = "HTMLSTORE";
    
    public HTMLViewer(HTMLViewerMIDlet midlet) {
        this.midlet = midlet;
        
        viewerForm = new Form("HTML Viewer");
        
        backCommand = new Command("Back", Command.BACK, 1);
        refreshCommand = new Command("Refresh", Command.ITEM, 2);
        
        viewerForm.addCommand(backCommand);
        viewerForm.addCommand(refreshCommand);
        viewerForm.setCommandListener(this);
    }
    
    public void show() {
        loadAndDisplay();
        midlet.getDisplay().setCurrent(viewerForm);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.getDisplay().setCurrent(midlet.getMainMenu());
        } else if (c == refreshCommand) {
            loadAndDisplay();
        }
    }
    
    private void loadAndDisplay() {
        viewerForm.deleteAll();
        
        try {
            RecordStore rs = RecordStore.openRecordStore(RECORD_STORE, false);
            
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                String content = new String(data);
                
                String[] parts = split(content, '|');
                if (parts.length >= 3) {
                    viewerForm.append("URL: " + parts[0] + "\n");
                    viewerForm.append("Title: " + parts[1] + "\n");
                    viewerForm.append("--------------------\n");
                    
                    // Parse and display HTML
                    parseAndDisplayHTML(parts[2]);
                }
            } else {
                viewerForm.append("No HTML content saved.\nUse Editor to create content.");
            }
            
            rs.closeRecordStore();
            
        } catch (RecordStoreNotFoundException e) {
            viewerForm.append("No HTML content found.\nUse Editor to create content.");
        } catch (Exception e) {
            viewerForm.append("Error: " + e.getMessage());
        }
    }
    
    private void parseAndDisplayHTML(String html) {
        // Simple HTML parser for J2ME
        String text = html;
        
        // Remove HTML tags and display content
        text = removeTag(text, "<html>");
        text = removeTag(text, "</html>");
        text = removeTagSection(text, "<head>", "</head>");
        text = removeTag(text, "<body>");
        text = removeTag(text, "</body>");
        
        // Handle headings
        text = replaceTag(text, "<h1>", "</h1>", "\n## ", " ##\n");
        text = replaceTag(text, "<h2>", "</h2>", "\n# ", " #\n");
        text = replaceTag(text, "<h3>", "</h3>", "\n# ", " #\n");
        
        // Handle paragraphs
        text = replaceTag(text, "<p>", "</p>", "\n", "\n");
        
        // Handle line breaks
        text = stringReplace(text, "<br>", "\n");
        text = stringReplace(text, "<br/>", "\n");
        text = stringReplace(text, "<BR>", "\n");
        
        viewerForm.append(text);
    }
    
    private String removeTag(String text, String tag) {
        return stringReplace(text, tag, "");
    }
    
    private String removeTagSection(String text, String startTag, String endTag) {
        int start = text.indexOf(startTag);
        int end = text.indexOf(endTag);
        
        if (start != -1 && end != -1) {
            return text.substring(0, start) + text.substring(end + endTag.length());
        }
        return text;
    }
    
    private String replaceTag(String text, String startTag, String endTag, 
                             String startReplace, String endReplace) {
        text = stringReplace(text, startTag, startReplace);
        text = stringReplace(text, endTag, endReplace);
        return text;
    }
    
    // String replace method for J2ME
    private String stringReplace(String source, String find, String replacement) {
        if (source == null || find == null || replacement == null) {
            return source;
        }
        
        StringBuffer result = new StringBuffer();
        int findLength = find.length();
        int startIndex = 0;
        int indexOf;
        
        while ((indexOf = source.indexOf(find, startIndex)) >= 0) {
            result.append(source.substring(startIndex, indexOf));
            result.append(replacement);
            startIndex = indexOf + findLength;
        }
        
        result.append(source.substring(startIndex));
        return result.toString();
    }
    
    private String[] split(String str, char delimiter) {
        if (str == null || str.length() == 0) {
            return new String[0];
        }
        
        int count = 1;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == delimiter) count++;
        }
        
        String[] result = new String[count];
        int start = 0;
        int index = 0;
        
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == delimiter) {
                result[index++] = str.substring(start, i);
                start = i + 1;
            }
        }
        result[index] = str.substring(start);
        
        return result;
    }
}