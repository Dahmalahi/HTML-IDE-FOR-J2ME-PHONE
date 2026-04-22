import javax.microedition.lcdui.*;

public class TemplateManager implements CommandListener {
    private HTMLViewerMIDlet midlet;
    private CodeEditor editor;
    private String fileType;
    private List templateList;
    private Command backCommand;
    private Command insertCommand;
    
    private static final String[] HTML_TEMPLATES = {
        "Basic HTML5",
        "HTML Form",
        "Table",
        "Div Container",
        "Link",
        "Image",
        "List",
        "Button"
    };
    
    private static final String[] CSS_TEMPLATES = {
        "Flexbox Container",
        "Grid Layout",
        "Button Style",
        "Card Style",
        "Animation",
        "Media Query",
        "Reset CSS"
    };
    
    public TemplateManager(HTMLViewerMIDlet midlet, CodeEditor editor, String fileType) {
        this.midlet = midlet;
        this.editor = editor;
        this.fileType = fileType;
        
        templateList = new List("Code Templates", List.IMPLICIT);
        
        String[] templates = fileType.equals("html") ? HTML_TEMPLATES : CSS_TEMPLATES;
        for (int i = 0; i < templates.length; i++) {
            templateList.append(templates[i], null);
        }
        
        backCommand = new Command("Back", Command.BACK, 1);
        insertCommand = new Command("Insert", Command.OK, 1);
        
        templateList.addCommand(backCommand);
        templateList.addCommand(insertCommand);
        templateList.setCommandListener(this);
    }
    
    public void show() {
        midlet.getDisplay().setCurrent(templateList);
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.getDisplay().setCurrent(editor);
        } else if (c == insertCommand || c == List.SELECT_COMMAND) {
            insertTemplate(templateList.getSelectedIndex());
        }
    }
    
    private void insertTemplate(int index) {
        String template = "";
        
        if (fileType.equals("html")) {
            template = getHTMLTemplate(index);
        } else {
            template = getCSSTemplate(index);
        }
        
        editor.insertTemplate(template);
        midlet.getDisplay().setCurrent(editor);
    }
    
    private String getHTMLTemplate(int index) {
        switch (index) {
            case 0: // Basic HTML5
                return "<!DOCTYPE html>\n<html>\n<head>\n  <meta charset=\"UTF-8\">\n  " +
                       "<title>Document</title>\n</head>\n<body>\n  \n</body>\n</html>";
            case 1: // Form
                return "<form action=\"\" method=\"post\">\n  <input type=\"text\" name=\"field\">\n  " +
                       "<button type=\"submit\">Submit</button>\n</form>";
            case 2: // Table
                return "<table>\n  <tr>\n    <th>Header</th>\n  </tr>\n  <tr>\n    " +
                       "<td>Data</td>\n  </tr>\n</table>";
            case 3: // Div
                return "<div class=\"container\">\n  <p>Content</p>\n</div>";
            case 4: // Link
                return "<a href=\"#\">Link Text</a>";
            case 5: // Image
                return "<img src=\"image.jpg\" alt=\"Description\">";
            case 6: // List
                return "<ul>\n  <li>Item 1</li>\n  <li>Item 2</li>\n</ul>";
            case 7: // Button
                return "<button class=\"btn\">Click Me</button>";
            default:
                return "";
        }
    }
    
    private String getCSSTemplate(int index) {
        switch (index) {
            case 0: // Flexbox
                return ".flex-container {\n  display: flex;\n  justify-content: center;\n  " +
                       "align-items: center;\n}";
            case 1: // Grid
                return ".grid-container {\n  display: grid;\n  grid-template-columns: 1fr 1fr;\n  " +
                       "gap: 20px;\n}";
            case 2: // Button
                return ".btn {\n  background-color: #007bff;\n  color: white;\n  " +
                       "padding: 10px 20px;\n  border: none;\n  border-radius: 4px;\n}";
            case 3: // Card
                return ".card {\n  background: white;\n  padding: 20px;\n  " +
                       "border-radius: 8px;\n  box-shadow: 0 2px 4px rgba(0,0,0,0.1);\n}";
            case 4: // Animation
                return "@keyframes fadeIn {\n  from { opacity: 0; }\n  to { opacity: 1; }\n}";
            case 5: // Media Query
                return "@media (max-width: 768px) {\n  .container {\n    width: 100%;\n  }\n}";
            case 6: // Reset
                return "* {\n  margin: 0;\n  padding: 0;\n  box-sizing: border-box;\n}";
            default:
                return "";
        }
    }
}