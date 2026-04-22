remove emoji # HTML IDE for J2ME - Brief Description

## What It Is
A **mobile HTML/CSS/JavaScript IDE** built entirely in Java ME (J2ME) for old Nokia/Sony Ericsson feature phones running MIDP 2.0.

---

## Core Features

### 📝 Code Editor
- Syntax highlighting for **HTML, CSS and JavaScript**
- Dark theme (VS Code style)
- Line numbers
- Press **5** = edit entire file
- Press **7** = edit single line
- Press **9** = insert new line
- Press **0** = delete line
- Auto-format/beautify code

### 🗂️ Three Edit Modes
- **HTML only** editor
- **CSS only** editor
- **JS only** editor
- **Combined** = all 3 in one `.html` file

### 📁 Project Manager
- Create/open/delete projects
- Each project has `index.html` + `style.css` + `script.js`
- Saved to device memory (RMS)

### 💾 File Manager (JSR-75)
- Browse device file system
- Save HTML/CSS/JS files to memory card
- Open files from storage

### 🎨 Color Picker
- **Palette mode** = 30 preset colors
- **Slider mode** = custom RGB mixing
- **Hex input** = type `#RRGGBB` directly
- Inserts picked color into CSS

### 🔍 DOM Inspector
- Visual tree view of HTML structure
- Shows tag names, IDs, class names
- Shows nesting depth
- Click node for full info

### 🕐 Recent Files
- Tracks last 10 opened files
- Quick reopen from list
- Remembers file type

### 👁️ Live Preview
- Renders HTML output on canvas
- Scrollable preview

### 🔎 Find & Replace
- Find text in code
- Replace first or replace all

### 📋 Code Templates
- 8 HTML snippets
- 7 CSS snippets
- Insert at cursor position

---

## Technical Stack

```
Language  : Java ME (J2ME)
Profile   : MIDP 2.0
Config    : CLDC 1.1
Storage   : RMS (Record Store) + JSR-75 (File System)
UI        : Low-level Canvas + High-level LCDUI Forms
Target    : Feature phones (Nokia, Sony Ericsson etc)
```

---

## File Structure
```
src/
├── HTMLViewerMIDlet.java  → Main app + menu
├── HTMLEditor.java        → Editor controller
├── CodeEditor.java        → Canvas code editor
├── HTMLViewer.java        → HTML viewer
├── HTMLRunner.java        → HTML renderer
├── FileManager.java       → JSR-75 file access
├── ProjectManager.java    → Multi-file projects
├── ColorPicker.java       → Color selection tool
├── DOMInspector.java      → HTML tree viewer
├── RecentFiles.java       → File history
├── TemplateManager.java   → Code snippets
├── LivePreview.java       → Live render
└── FindReplace.java       → Search tool
```

---

## In Simple Words
> It is like having a **mini VS Code** on your old Nokia phone where you can write HTML websites, style them with CSS, add JavaScript logic, manage multiple projects and save files to your memory card — all without internet or a computer.
