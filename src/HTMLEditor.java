import javax.microedition.lcdui.*;
import javax.microedition.rms.*;

public class HTMLEditor implements CommandListener {
    private HTMLViewerMIDlet midlet;
    private Form editorForm;
    private TextField urlField;
    private TextField titleField;
    private ChoiceGroup modeGroup;

    private Command saveCommand;
    private Command loadCommand;
    private Command clearCommand;
    private Command backCommand;
    private Command editHTMLCommand;
    private Command editCSSCommand;
    private Command editJSCommand;
    private Command editCombinedCommand;
    private Command inspectCommand;
    private Command colorPickerCommand;

    private static final String RECORD_STORE   = "HTMLSTORE";
    private static final String CSS_STORE      = "CSSSTORE";
    private static final String JS_STORE       = "JSSTORE";
    private static final String COMBINED_STORE = "COMBINEDSTORE";

    private String currentHTML     = "";
    private String currentCSS      = "";
    private String currentJS       = "";
    private String currentFileName = "untitled";

    public HTMLEditor(HTMLViewerMIDlet midlet) {
        this.midlet = midlet;
        initUI();
        currentHTML = getSampleHTML();
        currentCSS  = getSampleCSS();
        currentJS   = getSampleJS();
    }

    private void initUI() {
        editorForm = new Form("HTML/CSS/JS IDE");

        urlField   = new TextField("URL/Filename:", "index.html", 100, TextField.ANY);
        titleField = new TextField("Title:", "My Page", 50, TextField.ANY);

        String[] modes = {"Separate Files", "Combined .html"};
        modeGroup = new ChoiceGroup("Edit Mode:", ChoiceGroup.EXCLUSIVE, modes, null);

        editorForm.append(urlField);
        editorForm.append(titleField);
        editorForm.append(modeGroup);
        editorForm.append("Use commands below to edit");

        saveCommand        = new Command("Save",          Command.OK,   1);
        loadCommand        = new Command("Load",          Command.ITEM, 2);
        clearCommand       = new Command("Clear",         Command.ITEM, 3);
        editHTMLCommand    = new Command("Edit HTML",     Command.ITEM, 4);
        editCSSCommand     = new Command("Edit CSS",      Command.ITEM, 5);
        editJSCommand      = new Command("Edit JS",       Command.ITEM, 6);
        editCombinedCommand= new Command("Edit Combined", Command.ITEM, 7);
        inspectCommand     = new Command("DOM Inspector", Command.ITEM, 8);
        colorPickerCommand = new Command("Color Picker",  Command.ITEM, 9);
        backCommand        = new Command("Back",          Command.BACK, 10);

        editorForm.addCommand(saveCommand);
        editorForm.addCommand(loadCommand);
        editorForm.addCommand(clearCommand);
        editorForm.addCommand(editHTMLCommand);
        editorForm.addCommand(editCSSCommand);
        editorForm.addCommand(editJSCommand);
        editorForm.addCommand(editCombinedCommand);
        editorForm.addCommand(inspectCommand);
        editorForm.addCommand(colorPickerCommand);
        editorForm.addCommand(backCommand);
        editorForm.setCommandListener(this);
    }

    public void show() {
        midlet.getDisplay().setCurrent(editorForm);
    }

    // Called from HTMLViewerMIDlet.openInEditor()
    public void openFile(String content, String fileType, String fileName) {
        currentFileName = fileName;
        urlField.setString(fileName);

        if (fileType.equals("html")) {
            if (content.length() > 0) currentHTML = content;
        } else if (fileType.equals("css")) {
            if (content.length() > 0) currentCSS = content;
        } else if (fileType.equals("js")) {
            if (content.length() > 0) currentJS = content;
        } else if (fileType.equals("combined")) {
            if (content.length() > 0) parseCombinedFile(content);
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.getDisplay().setCurrent(midlet.getMainMenu());
        } else if (c == editHTMLCommand) {
            openCodeEditor(currentHTML, "html");
        } else if (c == editCSSCommand) {
            openCodeEditor(currentCSS, "css");
        } else if (c == editJSCommand) {
            openCodeEditor(currentJS, "js");
        } else if (c == editCombinedCommand) {
            openCombinedEditor();
        } else if (c == saveCommand) {
            saveContent();
        } else if (c == loadCommand) {
            loadContent();
        } else if (c == clearCommand) {
            clearFields();
        } else if (c == inspectCommand) {
            showDOMInspector();
        } else if (c == colorPickerCommand) {
            showColorPicker();
        }
    }

    private void openCodeEditor(String content, String fileType) {
        CodeEditor ce = new CodeEditor(midlet, content, fileType, this);
        midlet.getDisplay().setCurrent(ce);
    }

    private void openCombinedEditor() {
        CodeEditor ce = new CodeEditor(midlet, buildCombinedFile(), "combined", this);
        midlet.getDisplay().setCurrent(ce);
    }

    private String buildCombinedFile() {
        return "<!DOCTYPE html>\n<html>\n<head>\n" +
               "  <meta charset=\"UTF-8\">\n" +
               "  <title>" + titleField.getString() + "</title>\n" +
               "  <style>\n" + currentCSS + "\n  </style>\n" +
               "</head>\n<body>\n" + currentHTML + "\n" +
               "  <script>\n" + currentJS + "\n  </script>\n" +
               "</body>\n</html>";
    }

    private void parseCombinedFile(String combined) {
        int s1 = combined.indexOf("<style>");
        int e1 = combined.indexOf("</style>");
        if (s1 != -1 && e1 != -1)
            currentCSS = combined.substring(s1 + 7, e1).trim();

        int s2 = combined.indexOf("<script>");
        int e2 = combined.indexOf("</script>");
        if (s2 != -1 && e2 != -1)
            currentJS = combined.substring(s2 + 8, e2).trim();

        int s3 = combined.indexOf("<body>");
        int e3 = combined.indexOf("</body>");
        if (s3 != -1 && e3 != -1) {
            String body = combined.substring(s3 + 6, e3).trim();
            int scriptTag = body.indexOf("<script>");
            if (scriptTag != -1) body = body.substring(0, scriptTag).trim();
            currentHTML = body;
        }
    }

    // ---------------------------------------------------------------
    // Callbacks from CodeEditor
    // ---------------------------------------------------------------
    public void onHTMLEditorSave(String content) {
        currentHTML = content;
        RecentFiles.addFile(currentFileName, "html");
        showQuickAlert("HTML saved");
    }

    public void onCSSEditorSave(String content) {
        currentCSS = content;
        RecentFiles.addFile(
            stringReplace(currentFileName, ".html", ".css"), "css");
        showQuickAlert("CSS saved");
    }

    public void onJSEditorSave(String content) {
        currentJS = content;
        RecentFiles.addFile(
            stringReplace(currentFileName, ".html", ".js"), "js");
        showQuickAlert("JS saved");
    }

    public void onCombinedEditorSave(String content) {
        parseCombinedFile(content);
        RecentFiles.addFile(currentFileName, "combined");
        showQuickAlert("Combined file saved");
    }

    // Called from ColorPicker
    public void onColorSelected(String colorHex) {
        currentCSS = currentCSS + "\n/* color: " + colorHex + " */";
        Alert a = new Alert("Color Picked",
            colorHex + " added to CSS!", null, AlertType.INFO);
        a.setTimeout(1500);
        midlet.getDisplay().setCurrent(a, editorForm);
    }

    public void returnToEditor() {
        midlet.getDisplay().setCurrent(editorForm);
    }

    // ---------------------------------------------------------------
    // DOM Inspector & Color Picker launchers
    // ---------------------------------------------------------------
    private void showDOMInspector() {
        DOMInspector inspector = new DOMInspector(midlet, currentHTML, this);
        inspector.show();
    }

    private void showColorPicker() {
        ColorPicker picker = new ColorPicker(midlet, this);
        picker.show();
    }

    // ---------------------------------------------------------------
    // Save / Load
    // ---------------------------------------------------------------
    private void saveContent() {
        try {
            saveRecord(RECORD_STORE,
                urlField.getString() + "|" + titleField.getString() + "|" + currentHTML);
            saveRecord(CSS_STORE, currentCSS);
            saveRecord(JS_STORE, currentJS);
            saveRecord(COMBINED_STORE, buildCombinedFile());

            RecentFiles.addFile(urlField.getString(), "html");

            Alert a = new Alert("Saved", "All files saved!", null, AlertType.INFO);
            a.setTimeout(2000);
            midlet.getDisplay().setCurrent(a, editorForm);
        } catch (Exception e) {
            showError("Error saving: " + e.getMessage());
        }
    }

    private void saveRecord(String storeName, String data) throws Exception {
        RecordStore rs = RecordStore.openRecordStore(storeName, true);
        byte[] bytes = data.getBytes();
        if (rs.getNumRecords() > 0) {
            rs.setRecord(1, bytes, 0, bytes.length);
        } else {
            rs.addRecord(bytes, 0, bytes.length);
        }
        rs.closeRecordStore();
    }

    private void loadContent() {
        try {
            RecordStore rsHTML = RecordStore.openRecordStore(RECORD_STORE, false);
            if (rsHTML.getNumRecords() > 0) {
                String raw = new String(rsHTML.getRecord(1));
                String[] parts = split(raw, '|');
                if (parts.length >= 3) {
                    urlField.setString(parts[0]);
                    titleField.setString(parts[1]);
                    currentHTML = parts[2];
                }
            }
            rsHTML.closeRecordStore();
        } catch (Exception e) { /* no HTML stored yet */ }

        try {
            RecordStore rsCSS = RecordStore.openRecordStore(CSS_STORE, false);
            if (rsCSS.getNumRecords() > 0)
                currentCSS = new String(rsCSS.getRecord(1));
            rsCSS.closeRecordStore();
        } catch (Exception e) { currentCSS = getSampleCSS(); }

        try {
            RecordStore rsJS = RecordStore.openRecordStore(JS_STORE, false);
            if (rsJS.getNumRecords() > 0)
                currentJS = new String(rsJS.getRecord(1));
            rsJS.closeRecordStore();
        } catch (Exception e) { currentJS = getSampleJS(); }

        Alert a = new Alert("Loaded", "Content loaded!", null, AlertType.INFO);
        a.setTimeout(2000);
        midlet.getDisplay().setCurrent(a, editorForm);
    }

    private void clearFields() {
        urlField.setString("index.html");
        titleField.setString("My Page");
        currentHTML = getSampleHTML();
        currentCSS  = getSampleCSS();
        currentJS   = getSampleJS();
        Alert a = new Alert("Cleared", "Reset to defaults", null, AlertType.INFO);
        a.setTimeout(1500);
        midlet.getDisplay().setCurrent(a, editorForm);
    }

    // ---------------------------------------------------------------
    // Samples
    // ---------------------------------------------------------------
    private String getSampleHTML() {
        return "<div class=\"container\">\n" +
               "  <h1 class=\"title\" id=\"main-title\">Hello World!</h1>\n" +
               "  <p class=\"text\" id=\"intro\">HTML+CSS+JS sample.</p>\n" +
               "  <button class=\"btn\" onclick=\"showMsg()\">Click Me</button>\n" +
               "  <div id=\"output\"></div>\n" +
               "</div>";
    }

    private String getSampleCSS() {
        return "* { margin:0; padding:0; box-sizing:border-box; }\n" +
               "body { background:#f0f0f0; font-family:Arial; padding:20px; }\n" +
               ".container { max-width:600px; margin:0 auto; background:white;\n" +
               "  padding:20px; border-radius:8px; }\n" +
               ".title { color:#333; font-size:24px; margin-bottom:10px; }\n" +
               ".text  { color:#666; line-height:1.6; margin-bottom:20px; }\n" +
               ".btn   { background:#007bff; color:white; padding:10px 20px;\n" +
               "  border:none; border-radius:4px; cursor:pointer; }";
    }

    private String getSampleJS() {
        return "// JavaScript\n" +
               "function showMsg() {\n" +
               "  var out = document.getElementById('output');\n" +
               "  out.innerHTML = '<p>Button clicked!</p>';\n" +
               "  out.style.color = '#007bff';\n" +
               "}\n\n" +
               "function init() {\n" +
               "  console.log('Page loaded');\n" +
               "}\n\n" +
               "window.onload = init;";
    }

    // ---------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------
    public String getCurrentHTML()     { return currentHTML; }
    public String getCurrentCSS()      { return currentCSS; }
    public String getCurrentJS()       { return currentJS; }
    public String getCombinedContent() { return buildCombinedFile(); }

    private void showQuickAlert(String msg) {
        Alert a = new Alert("", msg, null, AlertType.INFO);
        a.setTimeout(1000);
        midlet.getDisplay().setCurrent(a, editorForm);
    }

    private void showError(String message) {
        Alert a = new Alert("Error", message, null, AlertType.ERROR);
        a.setTimeout(3000);
        midlet.getDisplay().setCurrent(a, editorForm);
    }

    private String stringReplace(String src, String find, String rep) {
        StringBuffer sb = new StringBuffer();
        int fl = find.length(), idx, start = 0;
        while ((idx = src.indexOf(find, start)) >= 0) {
            sb.append(src.substring(start, idx)).append(rep);
            start = idx + fl;
        }
        sb.append(src.substring(start));
        return sb.toString();
    }

    private String[] split(String str, char delim) {
        if (str == null || str.length() == 0) return new String[0];
        int count = 1;
        for (int i = 0; i < str.length(); i++)
            if (str.charAt(i) == delim) count++;
        String[] r = new String[count];
        int s = 0, idx = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == delim) {
                r[idx++] = str.substring(s, i);
                s = i + 1;
            }
        }
        r[idx] = str.substring(s);
        return r;
    }
}