import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import java.util.Vector;

public class ProjectManager implements CommandListener {
    private HTMLViewerMIDlet midlet;
    private List projectList;
    private Form newProjectForm;
    private Form projectDetailForm;
    private TextField projectNameField;
    private TextField projectDescField;
    
    private Command backCommand;
    private Command newCommand;
    private Command openCommand;
    private Command deleteCommand;
    private Command saveCommand;
    private Command editHTMLCommand;
    private Command editCSSCommand;
    private Command editJSCommand;
    
    private static final String PROJECT_STORE = "PROJECTS";
    private Vector projects;
    private int selectedProject = -1;
    
    public ProjectManager(HTMLViewerMIDlet midlet) {
        this.midlet = midlet;
        projects = new Vector();
        
        backCommand = new Command("Back", Command.BACK, 1);
        newCommand = new Command("New Project", Command.ITEM, 2);
        openCommand = new Command("Open", Command.OK, 1);
        deleteCommand = new Command("Delete", Command.ITEM, 3);
        saveCommand = new Command("Save", Command.OK, 1);
        editHTMLCommand = new Command("Edit HTML", Command.ITEM, 1);
        editCSSCommand = new Command("Edit CSS", Command.ITEM, 2);
        editJSCommand = new Command("Edit JS", Command.ITEM, 3);
        
        projectList = new List("Projects", List.IMPLICIT);
        projectList.addCommand(backCommand);
        projectList.addCommand(newCommand);
        projectList.addCommand(openCommand);
        projectList.addCommand(deleteCommand);
        projectList.setCommandListener(this);
        
        loadProjects();
    }
    
    public void show() {
        refreshProjectList();
        midlet.getDisplay().setCurrent(projectList);
    }
    
    private void refreshProjectList() {
        projectList.deleteAll();
        if (projects.size() == 0) {
            projectList.append("No projects yet", null);
        }
        for (int i = 0; i < projects.size(); i++) {
            Project p = (Project) projects.elementAt(i);
            projectList.append(p.name + " (" + p.fileCount + " files)", null);
        }
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            if (d == projectDetailForm || d == newProjectForm) {
                midlet.getDisplay().setCurrent(projectList);
            } else {
                midlet.getDisplay().setCurrent(midlet.getMainMenu());
            }
        } else if (c == newCommand) {
            showNewProjectForm();
        } else if (c == openCommand || c == List.SELECT_COMMAND) {
            if (d == projectList) openSelectedProject();
        } else if (c == deleteCommand) {
            deleteSelectedProject();
        } else if (c == saveCommand) {
            saveNewProject();
        } else if (c == editHTMLCommand) {
            openProjectEditor("html");
        } else if (c == editCSSCommand) {
            openProjectEditor("css");
        } else if (c == editJSCommand) {
            openProjectEditor("js");
        }
    }
    
    private void showNewProjectForm() {
        newProjectForm = new Form("New Project");
        projectNameField = new TextField("Project Name:", "", 50, TextField.ANY);
        projectDescField = new TextField("Description:", "", 100, TextField.ANY);
        
        newProjectForm.append(projectNameField);
        newProjectForm.append(projectDescField);
        newProjectForm.append("Files: index.html, style.css, script.js");
        
        newProjectForm.addCommand(saveCommand);
        newProjectForm.addCommand(backCommand);
        newProjectForm.setCommandListener(this);
        midlet.getDisplay().setCurrent(newProjectForm);
    }
    
    private void saveNewProject() {
        String name = projectNameField.getString().trim();
        if (name.length() == 0) {
            showError("Project name cannot be empty");
            return;
        }
        
        Project p = new Project();
        p.name = name;
        p.description = projectDescField.getString();
        p.fileCount = 3;
        p.html = getDefaultHTML(name);
        p.css = getDefaultCSS();
        p.js = getDefaultJS();
        
        projects.addElement(p);
        saveProjects();
        refreshProjectList();
        
        Alert alert = new Alert("Created", "Project '" + name + "' created!", null, AlertType.INFO);
        alert.setTimeout(2000);
        midlet.getDisplay().setCurrent(alert, projectList);
    }
    
    private void openSelectedProject() {
        int idx = projectList.getSelectedIndex();
        if (idx < 0 || idx >= projects.size()) return;
        
        selectedProject = idx;
        Project p = (Project) projects.elementAt(idx);
        
        projectDetailForm = new Form(p.name);
        projectDetailForm.append("Description: " + p.description + "\n");
        projectDetailForm.append("Files:\n");
        projectDetailForm.append("  index.html\n");
        projectDetailForm.append("  style.css\n");
        projectDetailForm.append("  script.js\n");
        
        projectDetailForm.addCommand(editHTMLCommand);
        projectDetailForm.addCommand(editCSSCommand);
        projectDetailForm.addCommand(editJSCommand);
        projectDetailForm.addCommand(backCommand);
        projectDetailForm.setCommandListener(this);
        midlet.getDisplay().setCurrent(projectDetailForm);
    }
    
    private void openProjectEditor(String fileType) {
        if (selectedProject < 0 || selectedProject >= projects.size()) return;
        Project p = (Project) projects.elementAt(selectedProject);
        
        String content = "";
        if (fileType.equals("html")) content = p.html;
        else if (fileType.equals("css")) content = p.css;
        else if (fileType.equals("js")) content = p.js;
        
        midlet.openInEditor(content, fileType, p.name + "/" + 
                           (fileType.equals("html") ? "index.html" : 
                            fileType.equals("css") ? "style.css" : "script.js"));
    }
    
    private void deleteSelectedProject() {
        int idx = projectList.getSelectedIndex();
        if (idx < 0 || idx >= projects.size()) return;
        
        Project p = (Project) projects.elementAt(idx);
        projects.removeElementAt(idx);
        saveProjects();
        refreshProjectList();
        
        Alert alert = new Alert("Deleted", "Project '" + p.name + "' deleted", null, AlertType.INFO);
        alert.setTimeout(2000);
        midlet.getDisplay().setCurrent(alert, projectList);
    }
    
    private void loadProjects() {
        try {
            RecordStore rs = RecordStore.openRecordStore(PROJECT_STORE, false);
            int numRecords = rs.getNumRecords();
            
            for (int i = 1; i <= numRecords; i++) {
                try {
                    byte[] data = rs.getRecord(i);
                    String raw = new String(data);
                    Project p = parseProject(raw);
                    if (p != null) projects.addElement(p);
                } catch (Exception e) {}
            }
            rs.closeRecordStore();
        } catch (Exception e) {}
    }
    
    private void saveProjects() {
        try {
            try {
                RecordStore.deleteRecordStore(PROJECT_STORE);
            } catch (Exception e) {}
            
            RecordStore rs = RecordStore.openRecordStore(PROJECT_STORE, true);
            for (int i = 0; i < projects.size(); i++) {
                Project p = (Project) projects.elementAt(i);
                String raw = serializeProject(p);
                byte[] data = raw.getBytes();
                rs.addRecord(data, 0, data.length);
            }
            rs.closeRecordStore();
        } catch (Exception e) {}
    }
    
    private String serializeProject(Project p) {
        return p.name + "~~" + p.description + "~~" + p.html + "~~" + p.css + "~~" + p.js;
    }
    
    private Project parseProject(String raw) {
        int d1 = raw.indexOf("~~");
        if (d1 == -1) return null;
        int d2 = raw.indexOf("~~", d1 + 2);
        if (d2 == -1) return null;
        int d3 = raw.indexOf("~~", d2 + 2);
        if (d3 == -1) return null;
        int d4 = raw.indexOf("~~", d3 + 2);
        if (d4 == -1) return null;
        
        Project p = new Project();
        p.name = raw.substring(0, d1);
        p.description = raw.substring(d1 + 2, d2);
        p.html = raw.substring(d2 + 2, d3);
        p.css = raw.substring(d3 + 2, d4);
        p.js = raw.substring(d4 + 2);
        p.fileCount = 3;
        return p;
    }
    
    private String getDefaultHTML(String name) {
        return "<div class=\"container\">\n  <h1>" + name + "</h1>\n" +
               "  <p>Welcome to " + name + " project.</p>\n" +
               "  <button onclick=\"init()\">Start</button>\n</div>";
    }
    
    private String getDefaultCSS() {
        return "body { background:#f5f5f5; font-family:Arial; padding:20px; }\n" +
               ".container { background:white; padding:20px; border-radius:8px; }\n" +
               "h1 { color:#333; }\np { color:#666; }\n" +
               "button { background:#007bff; color:white; padding:8px 16px; border:none; border-radius:4px; }";
    }
    
    private String getDefaultJS() {
        return "function init() {\n  console.log('Project started');\n" +
               "  document.querySelector('h1').style.color = '#007bff';\n}\n" +
               "window.onload = function() { init(); };";
    }
    
    private void showError(String msg) {
        Alert alert = new Alert("Error", msg, null, AlertType.ERROR);
        alert.setTimeout(2000);
        midlet.getDisplay().setCurrent(alert, midlet.getDisplay().getCurrent());
    }
    
    class Project {
        String name = "";
        String description = "";
        String html = "";
        String css = "";
        String js = "";
        int fileCount = 3;
    }
}