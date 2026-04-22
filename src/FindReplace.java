import javax.microedition.lcdui.*;

public class FindReplace implements CommandListener {
    private HTMLViewerMIDlet midlet;
    private CodeEditor editor;
    private String content;
    private Form findForm;
    private TextField findField;
    private TextField replaceField;
    private Command findCommand;
    private Command replaceCommand;
    private Command replaceAllCommand;
    private Command backCommand;
    
    public FindReplace(HTMLViewerMIDlet midlet, CodeEditor editor, String content) {
        this.midlet = midlet;
        this.editor = editor;
        this.content = content;
        
        findForm = new Form("Find & Replace");
        findField = new TextField("Find:", "", 50, TextField.ANY);
        replaceField = new TextField("Replace with:", "", 50, TextField.ANY);
        
        findForm.append(findField);
        findForm.append(replaceField);
        
        findCommand = new Command("Find", Command.OK, 1);
        replaceCommand = new Command("Replace", Command.ITEM, 2);
        replaceAllCommand = new Command("Replace All", Command.ITEM, 3);
        backCommand = new Command("Back", Command.BACK, 4);
        
        findForm.addCommand(findCommand);
        findForm.addCommand(replaceCommand);
        findForm.addCommand(replaceAllCommand);
        findForm.addCommand(backCommand);
        findForm.setCommandListener(this);
    }
    
    public void show() {
        midlet.getDisplay().setCurrent(findForm);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.getDisplay().setCurrent(editor);
        } else if (c == findCommand) {
            find();
        } else if (c == replaceCommand) {
            replaceFirst();
        } else if (c == replaceAllCommand) {
            replaceAll();
        }
    }
    
    private void find() {
        String searchText = findField.getString();
        int index = content.indexOf(searchText);
        
        if (index != -1) {
            Alert alert = new Alert("Found", "Text found at position " + index, 
                                  null, AlertType.INFO);
            alert.setTimeout(2000);
            midlet.getDisplay().setCurrent(alert, findForm);
        } else {
            Alert alert = new Alert("Not Found", "Text not found", null, AlertType.WARNING);
            alert.setTimeout(2000);
            midlet.getDisplay().setCurrent(alert, findForm);
        }
    }
    
    private void replaceFirst() {
        String searchText = findField.getString();
        String replaceText = replaceField.getString();
        
        int index = content.indexOf(searchText);
        if (index != -1) {
            String newContent = content.substring(0, index) + replaceText + 
                              content.substring(index + searchText.length());
            editor.setContent(newContent);
            
            Alert alert = new Alert("Success", "Replaced 1 occurrence", null, AlertType.INFO);
            alert.setTimeout(2000);
            midlet.getDisplay().setCurrent(alert, editor);
        } else {
            Alert alert = new Alert("Not Found", "Text not found", null, AlertType.WARNING);
            alert.setTimeout(2000);
            midlet.getDisplay().setCurrent(alert, findForm);
        }
    }
    
    private void replaceAll() {
        String searchText = findField.getString();
        String replaceText = replaceField.getString();
        
        String newContent = stringReplaceAll(content, searchText, replaceText);
        editor.setContent(newContent);
        
        Alert alert = new Alert("Success", "All occurrences replaced", null, AlertType.INFO);
        alert.setTimeout(2000);
        midlet.getDisplay().setCurrent(alert, editor);
    }
    
    private String stringReplaceAll(String source, String find, String replacement) {
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
}