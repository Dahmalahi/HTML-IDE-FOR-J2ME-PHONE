import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Vector;

public class FileManager implements CommandListener {
    private HTMLViewerMIDlet midlet;
    private List fileList;
    private List actionList;
    private Form saveForm;
    private TextField fileNameField;
    private TextBox contentBox;
    
    private Command backCommand;
    private Command selectCommand;
    private Command rootsCommand;
    private Command saveCommand;
    private Command openCommand;
    
    private String currentPath = "";
    private String currentContent = "";
    private String fileType = "html";
    
    public FileManager(HTMLViewerMIDlet midlet) {
        this.midlet = midlet;
        
        fileList = new List("File Browser", List.IMPLICIT);
        backCommand = new Command("Back", Command.BACK, 1);
        selectCommand = new Command("Select", Command.OK, 1);
        rootsCommand = new Command("Roots", Command.ITEM, 2);
        
        fileList.addCommand(backCommand);
        fileList.addCommand(selectCommand);
        fileList.addCommand(rootsCommand);
        fileList.setCommandListener(this);
        
        // Action list
        actionList = new List("File Actions", List.IMPLICIT);
        actionList.append("Save HTML File", null);
        actionList.append("Save CSS File", null);
        actionList.append("Open File", null);
        actionList.append("Browse Files", null);
        actionList.addCommand(backCommand);
        actionList.addCommand(selectCommand);
        actionList.setCommandListener(this);
    }
    
    public void show() {
        midlet.getDisplay().setCurrent(actionList);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            if (d == fileList) {
                midlet.getDisplay().setCurrent(actionList);
            } else if (d == saveForm) {
                midlet.getDisplay().setCurrent(actionList);
            } else if (d == contentBox) {
                midlet.getDisplay().setCurrent(actionList);
            } else {
                midlet.getDisplay().setCurrent(midlet.getMainMenu());
            }
        } else if (c == selectCommand || c == List.SELECT_COMMAND) {
            if (d == actionList) {
                handleActionSelection(actionList.getSelectedIndex());
            } else if (d == fileList) {
                handleFileSelection();
            }
        } else if (c == rootsCommand) {
            showRoots();
        } else if (c == saveCommand) {
            saveFile();
        } else if (c == openCommand) {
            String fileName = fileList.getString(fileList.getSelectedIndex());
            if (!fileName.endsWith("/")) {
                openFile(currentPath + fileName);
            }
        }
    }
    
    private void handleActionSelection(int index) {
        switch (index) {
            case 0: // Save HTML
                fileType = "html";
                showSaveForm("html");
                break;
            case 1: // Save CSS
                fileType = "css";
                showSaveForm("css");
                break;
            case 2: // Open File
                showRoots();
                break;
            case 3: // Browse Files
                showRoots();
                break;
        }
    }
    
    private void showSaveForm(String type) {
        saveForm = new Form("Save " + type.toUpperCase() + " File");
        fileNameField = new TextField("File Name:", "document." + type, 50, TextField.ANY);
        
        saveForm.append(fileNameField);
        saveForm.append("Click Select to choose location");
        
        saveCommand = new Command("Save", Command.OK, 1);
        saveForm.addCommand(saveCommand);
        saveForm.addCommand(backCommand);
        saveForm.setCommandListener(this);
        
        if (type.equals("html")) {
            currentContent = getSampleHTML();
        } else {
            currentContent = getSampleCSS();
        }
        
        midlet.getDisplay().setCurrent(saveForm);
    }
    
    private void showRoots() {
        fileList.deleteAll();
        fileList.setTitle("Select Root");
        currentPath = "";
        
        try {
            Enumeration roots = FileSystemRegistry.listRoots();
            while (roots.hasMoreElements()) {
                String root = (String) roots.nextElement();
                fileList.append(root, null);
            }
            
            if (fileList.size() == 0) {
                fileList.append("No file system available", null);
            }
            
            midlet.getDisplay().setCurrent(fileList);
        } catch (Exception e) {
            showError("Error accessing file system: " + e.getMessage());
        }
    }
    
    private void handleFileSelection() {
        String selected = fileList.getString(fileList.getSelectedIndex());
        
        if (selected.equals("No file system available")) {
            return;
        }
        
        if (selected.equals("..")) {
            navigateUp();
        } else if (selected.endsWith("/")) {
            navigateToDirectory(selected);
        } else {
            openFile(currentPath + selected);
        }
    }
    
    private void navigateToDirectory(String dir) {
        if (currentPath.equals("")) {
            currentPath = "file:///" + dir;
        } else {
            currentPath = currentPath + dir;
        }
        
        listDirectory(currentPath);
    }
    
    private void navigateUp() {
        if (currentPath.length() > 8) {
            int lastSlash = currentPath.lastIndexOf('/', currentPath.length() - 2);
            if (lastSlash > 7) {
                currentPath = currentPath.substring(0, lastSlash + 1);
                listDirectory(currentPath);
            } else {
                currentPath = "";
                showRoots();
            }
        }
    }
    
    private void listDirectory(String path) {
        fileList.deleteAll();
        fileList.setTitle(path);
        
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(path);
            
            if (fc.isDirectory()) {
                fileList.append("..", null);
                
                Enumeration files = fc.list();
                while (files.hasMoreElements()) {
                    String fileName = (String) files.nextElement();
                    fileList.append(fileName, null);
                }
            }
            
            fc.close();
        } catch (Exception e) {
            showError("Error listing directory: " + e.getMessage());
            if (fc != null) {
                try { fc.close(); } catch (Exception ex) {}
            }
        }
    }
    
    private void saveFile() {
        String fileName = fileNameField.getString();
        
        if (fileName == null || fileName.length() == 0) {
            showError("Please enter a file name");
            return;
        }
        
        // Show roots to select save location
        if (currentPath.equals("")) {
            showRoots();
            return;
        }
        
        FileConnection fc = null;
        OutputStream os = null;
        
        try {
            String fullPath = currentPath + fileName;
            fc = (FileConnection) Connector.open(fullPath);
            
            if (!fc.exists()) {
                fc.create();
            }
            
            os = fc.openOutputStream();
            os.write(currentContent.getBytes());
            os.close();
            fc.close();
            
            Alert alert = new Alert("Success", "File saved: " + fileName, null, AlertType.INFO);
            alert.setTimeout(2000);
            midlet.getDisplay().setCurrent(alert, actionList);
            
        } catch (Exception e) {
            showError("Error saving file: " + e.getMessage());
        } finally {
            try {
                if (os != null) os.close();
                if (fc != null) fc.close();
            } catch (Exception e) {}
        }
    }
    
    private void openFile(String path) {
        FileConnection fc = null;
        InputStream is = null;
        
        try {
            fc = (FileConnection) Connector.open(path);
            
            if (fc.exists() && !fc.isDirectory()) {
                is = fc.openInputStream();
                
                int size = (int) fc.fileSize();
                byte[] data = new byte[size];
                is.read(data);
                
                String content = new String(data);
                
                contentBox = new TextBox("File Content", content, 
                    content.length() < 5000 ? 5000 : content.length(), TextField.ANY);
                contentBox.addCommand(backCommand);
                contentBox.setCommandListener(this);
                
                midlet.getDisplay().setCurrent(contentBox);
                
                is.close();
                fc.close();
            }
        } catch (Exception e) {
            showError("Error opening file: " + e.getMessage());
        } finally {
            try {
                if (is != null) is.close();
                if (fc != null) fc.close();
            } catch (Exception e) {}
        }
    }
    
    private String getSampleHTML() {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "  <title>Sample Page</title>\n" +
               "  <link rel=\"stylesheet\" href=\"style.css\">\n" +
               "</head>\n" +
               "<body>\n" +
               "  <div class=\"container\">\n" +
               "    <h1 class=\"title\">Hello World!</h1>\n" +
               "    <p class=\"text\">This is a sample HTML page with CSS.</p>\n" +
               "    <button class=\"btn\">Click Me</button>\n" +
               "  </div>\n" +
               "</body>\n" +
               "</html>";
    }
    
    private String getSampleCSS() {
        return "/* Sample CSS Stylesheet */\n" +
               "body {\n" +
               "  background-color: #f0f0f0;\n" +
               "  font-family: Arial, sans-serif;\n" +
               "  margin: 0;\n" +
               "  padding: 20px;\n" +
               "}\n\n" +
               ".container {\n" +
               "  max-width: 600px;\n" +
               "  margin: 0 auto;\n" +
               "  background: white;\n" +
               "  padding: 20px;\n" +
               "  border-radius: 8px;\n" +
               "  box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n" +
               "}\n\n" +
               ".title {\n" +
               "  color: #333;\n" +
               "  font-size: 24px;\n" +
               "  margin-bottom: 10px;\n" +
               "}\n\n" +
               ".text {\n" +
               "  color: #666;\n" +
               "  line-height: 1.6;\n" +
               "}\n\n" +
               ".btn {\n" +
               "  background-color: #007bff;\n" +
               "  color: white;\n" +
               "  padding: 10px 20px;\n" +
               "  border: none;\n" +
               "  border-radius: 4px;\n" +
               "  cursor: pointer;\n" +
               "}";
    }
    
    private void showError(String message) {
        Alert alert = new Alert("Error", message, null, AlertType.ERROR);
        alert.setTimeout(3000);
        midlet.getDisplay().setCurrent(alert, actionList);
    }
}