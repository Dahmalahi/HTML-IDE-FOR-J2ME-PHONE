import javax.microedition.lcdui.*;

/**
 * SnippetLibrary – categorised code-snippet browser.
 * Replaces the old TemplateManager with more categories and a
 * richer description panel.
 */
public class SnippetLibrary implements CommandListener {

    private HTMLViewerMIDlet midlet;
    private CodeEditor       editor;
    private String           fileType;

    private List   catList;
    private List   snippetList;
    private Form   previewForm;
    private int    currentCat = -1;

    private Command backCmd;
    private Command insertCmd;
    private Command previewCmd;

    // ---------------------------------------------------------------
    // Category names
    // ---------------------------------------------------------------
    private static final String[] CAT_HTML = {
        "Structure", "Text & Headings", "Links & Images",
        "Forms", "Tables", "Media", "Semantic HTML5"
    };
    private static final String[] CAT_CSS = {
        "Layout / Flexbox", "Layout / Grid", "Typography",
        "Colors & Backgrounds", "Animations", "Responsive", "Utilities"
    };
    private static final String[] CAT_JS = {
        "DOM", "Events", "Functions", "Fetch / AJAX",
        "Arrays", "Objects", "Utilities"
    };

    // ---------------------------------------------------------------
    // Snippet names per category (HTML)
    // ---------------------------------------------------------------
    private static final String[][] NAMES_HTML = {
        // Structure
        { "HTML5 Boilerplate", "Head section", "Body wrapper", "Div container" },
        // Text & Headings
        { "h1–h6 set", "Paragraph", "Strong + em", "Blockquote", "Pre/code block" },
        // Links & Images
        { "Anchor link", "Image", "Image with link", "SVG icon" },
        // Forms
        { "Contact form", "Login form", "Search bar", "Select dropdown", "Checkbox group" },
        // Tables
        { "Basic table", "Striped table", "Responsive table wrapper" },
        // Media
        { "Video embed", "Audio player", "YouTube iframe", "Figure+caption" },
        // Semantic HTML5
        { "Nav bar", "Article", "Aside", "Footer", "Header", "Main + section" }
    };
    private static final String[][] NAMES_CSS = {
        // Flexbox
        { "Flex row centre", "Flex column", "Space-between", "Flex card" },
        // Grid
        { "2-col grid", "3-col grid", "Auto-fill grid", "Grid named areas" },
        // Typography
        { "Google Font import", "Fluid font-size", "Text truncate", "Line clamp" },
        // Colors & Backgrounds
        { "CSS variables", "Gradient bg", "Semi-transparent", "Box shadow" },
        // Animations
        { "FadeIn", "Slide up", "Pulse", "Spin loader" },
        // Responsive
        { "Mobile-first MQ", "Tablet MQ", "Hide on mobile" },
        // Utilities
        { "Reset *", "Visually hidden", "Clearfix", "Aspect ratio 16/9" }
    };
    private static final String[][] NAMES_JS = {
        // DOM
        { "getElementById", "querySelector all", "Create element", "Remove element" },
        // Events
        { "addEventListener click", "DOMContentLoaded", "Keyboard event", "Custom event" },
        // Functions
        { "Arrow function", "Async/await", "Debounce", "Throttle" },
        // Fetch / AJAX
        { "fetch GET", "fetch POST JSON", "Error handling", "Load JSON" },
        // Arrays
        { "map/filter/reduce", "forEach loop", "Array spread", "Unique values" },
        // Objects
        { "Object destructure", "Spread merge", "JSON parse/stringify", "Deep clone" },
        // Utilities
        { "LocalStorage get/set", "Cookie helper", "UUID generator", "Format date" }
    };

    public SnippetLibrary(HTMLViewerMIDlet midlet, CodeEditor editor, String fileType) {
        this.midlet   = midlet;
        this.editor   = editor;
        this.fileType = fileType;

        backCmd    = new Command("Back",    Command.BACK, 1);
        insertCmd  = new Command("Insert",  Command.OK,   1);
        previewCmd = new Command("Preview", Command.ITEM, 2);

        buildCategoryList();
    }

    private void buildCategoryList() {
        catList = new List("Snippets – " + fileType.toUpperCase(), List.IMPLICIT);
        String[] cats = fileType.equals("css") ? CAT_CSS
                      : fileType.equals("js")  ? CAT_JS
                      : CAT_HTML;
        for (int i = 0; i < cats.length; i++) catList.append(cats[i], null);
        catList.addCommand(backCmd);
        catList.setCommandListener(this);
    }

    public void show() {
        midlet.getDisplay().setCurrent(catList);
    }

    // ---------------------------------------------------------------
    private void showSnippets(int cat) {
        currentCat = cat;
        String[][] names = fileType.equals("css") ? NAMES_CSS
                         : fileType.equals("js")  ? NAMES_JS
                         : NAMES_HTML;
        snippetList = new List(getCatLabel(cat), List.IMPLICIT);
        for (int i = 0; i < names[cat].length; i++)
            snippetList.append(names[cat][i], null);
        snippetList.addCommand(backCmd);
        snippetList.addCommand(insertCmd);
        snippetList.addCommand(previewCmd);
        snippetList.setCommandListener(this);
        midlet.getDisplay().setCurrent(snippetList);
    }

    private String getCatLabel(int cat) {
        String[] cats = fileType.equals("css") ? CAT_CSS
                      : fileType.equals("js")  ? CAT_JS
                      : CAT_HTML;
        return cat < cats.length ? cats[cat] : "Snippets";
    }

    private void doPreview(int snIdx) {
        String code = getSnippet(currentCat, snIdx);
        previewForm = new Form("Preview");
        StringItem si = new StringItem(null, code);
        previewForm.append(si);
        previewForm.addCommand(insertCmd);
        previewForm.addCommand(backCmd);
        previewForm.setCommandListener(this);
        midlet.getDisplay().setCurrent(previewForm);
    }

    private void doInsert(int snIdx) {
        String code = getSnippet(currentCat, snIdx);
        editor.insertTemplate(code);
        midlet.getDisplay().setCurrent(editor);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCmd) {
            if (d == snippetList || d == previewForm) {
                midlet.getDisplay().setCurrent(catList);
            } else {
                midlet.getDisplay().setCurrent(editor);
            }
        } else if (d == catList) {
            showSnippets(catList.getSelectedIndex());
        } else if (c == previewCmd && d == snippetList) {
            doPreview(snippetList.getSelectedIndex());
        } else if (c == insertCmd) {
            if (d == snippetList) doInsert(snippetList.getSelectedIndex());
            else if (d == previewForm) {
                // find index from preview – just use current selection
                doInsert(snippetList.getSelectedIndex());
            }
        }
    }

    // ---------------------------------------------------------------
    // All snippet bodies
    // ---------------------------------------------------------------
    private String getSnippet(int cat, int idx) {
        if (fileType.equals("html")) return getHTML(cat, idx);
        if (fileType.equals("css"))  return getCSS(cat, idx);
        if (fileType.equals("js"))   return getJS(cat, idx);
        return getHTML(cat, idx);
    }

    private String getHTML(int cat, int idx) {
        switch (cat) {
            case 0: return getHTMLStructure(idx);
            case 1: return getHTMLText(idx);
            case 2: return getHTMLLinks(idx);
            case 3: return getHTMLForms(idx);
            case 4: return getHTMLTables(idx);
            case 5: return getHTMLMedia(idx);
            case 6: return getHTMLSemantic(idx);
        }
        return "";
    }
    private String getHTMLStructure(int i) {
        switch (i) {
            case 0: return "<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n  <title>Page Title</title>\n  <link rel=\"stylesheet\" href=\"style.css\">\n</head>\n<body>\n\n  <script src=\"script.js\"></script>\n</body>\n</html>";
            case 1: return "<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n  <meta name=\"description\" content=\"Page description\">\n  <title>Title</title>\n</head>";
            case 2: return "<body>\n  <div class=\"wrapper\">\n    <!-- content -->\n  </div>\n</body>";
            case 3: return "<div class=\"container\">\n  <!-- content here -->\n</div>";
        }
        return "";
    }
    private String getHTMLText(int i) {
        switch (i) {
            case 0: return "<h1>Heading 1</h1>\n<h2>Heading 2</h2>\n<h3>Heading 3</h3>\n<h4>Heading 4</h4>\n<h5>Heading 5</h5>\n<h6>Heading 6</h6>";
            case 1: return "<p>Paragraph text goes here.</p>";
            case 2: return "<p><strong>Bold text</strong> and <em>italic text</em>.</p>";
            case 3: return "<blockquote>\n  <p>Quote text here.</p>\n  <cite>Author Name</cite>\n</blockquote>";
            case 4: return "<pre><code class=\"language-js\">\n// code here\n</code></pre>";
        }
        return "";
    }
    private String getHTMLLinks(int i) {
        switch (i) {
            case 0: return "<a href=\"https://example.com\" target=\"_blank\" rel=\"noopener\">Link Text</a>";
            case 1: return "<img src=\"image.jpg\" alt=\"Description\" width=\"300\" height=\"200\" loading=\"lazy\">";
            case 2: return "<a href=\"#\">\n  <img src=\"image.jpg\" alt=\"Clickable image\" width=\"200\">\n</a>";
            case 3: return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"24\" height=\"24\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\">\n  <circle cx=\"12\" cy=\"12\" r=\"10\"/>\n</svg>";
        }
        return "";
    }
    private String getHTMLForms(int i) {
        switch (i) {
            case 0: return "<form action=\"\" method=\"post\">\n  <div class=\"form-group\">\n    <label for=\"name\">Name</label>\n    <input type=\"text\" id=\"name\" name=\"name\" required>\n  </div>\n  <div class=\"form-group\">\n    <label for=\"email\">Email</label>\n    <input type=\"email\" id=\"email\" name=\"email\" required>\n  </div>\n  <div class=\"form-group\">\n    <label for=\"msg\">Message</label>\n    <textarea id=\"msg\" name=\"message\" rows=\"4\"></textarea>\n  </div>\n  <button type=\"submit\" class=\"btn\">Send</button>\n</form>";
            case 1: return "<form class=\"login-form\">\n  <input type=\"text\" placeholder=\"Username\" class=\"input\">\n  <input type=\"password\" placeholder=\"Password\" class=\"input\">\n  <button type=\"submit\" class=\"btn btn-primary\">Login</button>\n  <a href=\"#\">Forgot password?</a>\n</form>";
            case 2: return "<form class=\"search\">\n  <input type=\"search\" placeholder=\"Search...\" class=\"search-input\">\n  <button type=\"submit\" class=\"search-btn\">Search</button>\n</form>";
            case 3: return "<select name=\"options\" id=\"sel\">\n  <option value=\"\">Choose...</option>\n  <option value=\"1\">Option 1</option>\n  <option value=\"2\">Option 2</option>\n</select>";
            case 4: return "<fieldset>\n  <legend>Select options</legend>\n  <label><input type=\"checkbox\" name=\"opt\" value=\"a\"> Option A</label>\n  <label><input type=\"checkbox\" name=\"opt\" value=\"b\"> Option B</label>\n</fieldset>";
        }
        return "";
    }
    private String getHTMLTables(int i) {
        switch (i) {
            case 0: return "<table>\n  <thead>\n    <tr><th>Name</th><th>Age</th><th>City</th></tr>\n  </thead>\n  <tbody>\n    <tr><td>Alice</td><td>28</td><td>Paris</td></tr>\n    <tr><td>Bob</td><td>34</td><td>Lyon</td></tr>\n  </tbody>\n</table>";
            case 1: return "<table class=\"striped\">\n  <thead>\n    <tr><th>Col 1</th><th>Col 2</th></tr>\n  </thead>\n  <tbody>\n    <tr><td>A</td><td>B</td></tr>\n    <tr class=\"alt\"><td>C</td><td>D</td></tr>\n  </tbody>\n</table>";
            case 2: return "<div class=\"table-responsive\">\n  <table>\n    <!-- table contents -->\n  </table>\n</div>";
        }
        return "";
    }
    private String getHTMLMedia(int i) {
        switch (i) {
            case 0: return "<video controls width=\"300\">\n  <source src=\"video.mp4\" type=\"video/mp4\">\n  Your browser doesn't support video.\n</video>";
            case 1: return "<audio controls>\n  <source src=\"audio.mp3\" type=\"audio/mpeg\">\n  Your browser doesn't support audio.\n</audio>";
            case 2: return "<iframe width=\"300\" height=\"169\"\n  src=\"https://www.youtube.com/embed/VIDEO_ID\"\n  frameborder=\"0\" allowfullscreen>\n</iframe>";
            case 3: return "<figure>\n  <img src=\"photo.jpg\" alt=\"Photo\">\n  <figcaption>Caption text here</figcaption>\n</figure>";
        }
        return "";
    }
    private String getHTMLSemantic(int i) {
        switch (i) {
            case 0: return "<nav>\n  <ul class=\"nav-list\">\n    <li><a href=\"/\">Home</a></li>\n    <li><a href=\"/about\">About</a></li>\n    <li><a href=\"/contact\">Contact</a></li>\n  </ul>\n</nav>";
            case 1: return "<article>\n  <h2>Article Title</h2>\n  <p class=\"meta\">By Author &mdash; 2025</p>\n  <p>Article body...</p>\n</article>";
            case 2: return "<aside class=\"sidebar\">\n  <h3>Related</h3>\n  <ul><li><a href=\"#\">Link</a></li></ul>\n</aside>";
            case 3: return "<footer>\n  <p>&copy; 2025 My Site. All rights reserved.</p>\n  <nav><a href=\"/privacy\">Privacy</a></nav>\n</footer>";
            case 4: return "<header>\n  <h1 class=\"site-title\">Site Name</h1>\n  <nav><!-- nav here --></nav>\n</header>";
            case 5: return "<main>\n  <section id=\"intro\">\n    <h2>Section Title</h2>\n    <p>Content...</p>\n  </section>\n</main>";
        }
        return "";
    }

    private String getCSS(int cat, int idx) {
        switch (cat) {
            case 0: return getCSSFlex(idx);
            case 1: return getCSSGrid(idx);
            case 2: return getCSSType(idx);
            case 3: return getCSSColor(idx);
            case 4: return getCSSAnim(idx);
            case 5: return getCSSResponsive(idx);
            case 6: return getCSSUtil(idx);
        }
        return "";
    }
    private String getCSSFlex(int i) {
        switch (i) {
            case 0: return ".flex-center {\n  display: flex;\n  justify-content: center;\n  align-items: center;\n  gap: 16px;\n}";
            case 1: return ".flex-col {\n  display: flex;\n  flex-direction: column;\n  gap: 12px;\n}";
            case 2: return ".flex-between {\n  display: flex;\n  justify-content: space-between;\n  align-items: center;\n}";
            case 3: return ".card {\n  display: flex;\n  flex-direction: column;\n  background: #fff;\n  border-radius: 8px;\n  box-shadow: 0 2px 8px rgba(0,0,0,.12);\n  overflow: hidden;\n}";
        }
        return "";
    }
    private String getCSSGrid(int i) {
        switch (i) {
            case 0: return ".grid-2 {\n  display: grid;\n  grid-template-columns: 1fr 1fr;\n  gap: 20px;\n}";
            case 1: return ".grid-3 {\n  display: grid;\n  grid-template-columns: repeat(3, 1fr);\n  gap: 16px;\n}";
            case 2: return ".grid-auto {\n  display: grid;\n  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));\n  gap: 16px;\n}";
            case 3: return ".layout {\n  display: grid;\n  grid-template-areas:\n    'header header'\n    'sidebar main'\n    'footer footer';\n  grid-template-columns: 220px 1fr;\n}\n.header { grid-area: header; }\n.sidebar { grid-area: sidebar; }\n.main   { grid-area: main; }\n.footer { grid-area: footer; }";
        }
        return "";
    }
    private String getCSSType(int i) {
        switch (i) {
            case 0: return "@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');\nbody { font-family: 'Inter', sans-serif; }";
            case 1: return "h1 { font-size: clamp(1.5rem, 4vw, 3rem); }";
            case 2: return ".truncate {\n  white-space: nowrap;\n  overflow: hidden;\n  text-overflow: ellipsis;\n}";
            case 3: return ".clamp-3 {\n  display: -webkit-box;\n  -webkit-line-clamp: 3;\n  -webkit-box-orient: vertical;\n  overflow: hidden;\n}";
        }
        return "";
    }
    private String getCSSColor(int i) {
        switch (i) {
            case 0: return ":root {\n  --color-primary: #007ACC;\n  --color-secondary: #00B4D8;\n  --color-bg: #f5f5f5;\n  --color-text: #333;\n  --radius: 8px;\n}";
            case 1: return ".gradient-bg {\n  background: linear-gradient(135deg, #007ACC 0%, #00B4D8 100%);\n}";
            case 2: return ".overlay {\n  background: rgba(0, 0, 0, 0.5);\n  backdrop-filter: blur(4px);\n}";
            case 3: return ".shadow {\n  box-shadow:\n    0 1px 3px rgba(0,0,0,0.12),\n    0 4px 16px rgba(0,0,0,0.08);\n}";
        }
        return "";
    }
    private String getCSSAnim(int i) {
        switch (i) {
            case 0: return "@keyframes fadeIn {\n  from { opacity: 0; transform: translateY(8px); }\n  to   { opacity: 1; transform: translateY(0); }\n}\n.fade-in { animation: fadeIn 0.3s ease forwards; }";
            case 1: return "@keyframes slideUp {\n  from { transform: translateY(20px); opacity: 0; }\n  to   { transform: translateY(0);    opacity: 1; }\n}\n.slide-up { animation: slideUp 0.4s ease forwards; }";
            case 2: return "@keyframes pulse {\n  0%,100% { transform: scale(1);    }\n  50%     { transform: scale(1.05); }\n}\n.pulse { animation: pulse 1.5s ease-in-out infinite; }";
            case 3: return "@keyframes spin {\n  to { transform: rotate(360deg); }\n}\n.spinner {\n  width: 32px; height: 32px;\n  border: 3px solid #ddd;\n  border-top-color: #007ACC;\n  border-radius: 50%;\n  animation: spin .8s linear infinite;\n}";
        }
        return "";
    }
    private String getCSSResponsive(int i) {
        switch (i) {
            case 0: return "/* Mobile-first */\n.container { width: 100%; padding: 0 16px; }\n@media (min-width: 768px) {\n  .container { max-width: 720px; margin: 0 auto; }\n}";
            case 1: return "@media (min-width: 768px) and (max-width: 1024px) {\n  /* tablet styles */\n}";
            case 2: return "@media (max-width: 599px) {\n  .hide-mobile { display: none !important; }\n}";
        }
        return "";
    }
    private String getCSSUtil(int i) {
        switch (i) {
            case 0: return "*, *::before, *::after {\n  margin: 0;\n  padding: 0;\n  box-sizing: border-box;\n}";
            case 1: return ".sr-only {\n  position: absolute;\n  width: 1px; height: 1px;\n  overflow: hidden;\n  clip: rect(0,0,0,0);\n  white-space: nowrap;\n}";
            case 2: return ".clearfix::after {\n  content: '';\n  display: table;\n  clear: both;\n}";
            case 3: return ".ratio-16-9 {\n  aspect-ratio: 16 / 9;\n  width: 100%;\n}";
        }
        return "";
    }

    private String getJS(int cat, int idx) {
        switch (cat) {
            case 0: return getJSDOM(idx);
            case 1: return getJSEvents(idx);
            case 2: return getJSFunc(idx);
            case 3: return getJSFetch(idx);
            case 4: return getJSArray(idx);
            case 5: return getJSObject(idx);
            case 6: return getJSUtil(idx);
        }
        return "";
    }
    private String getJSDOM(int i) {
        switch (i) {
            case 0: return "var el = document.getElementById('myId');\nel.textContent = 'Hello';\nel.style.color = '#007ACC';";
            case 1: return "var items = document.querySelectorAll('.item');\nitems.forEach(function(el) {\n  el.classList.add('active');\n});";
            case 2: return "var el = document.createElement('div');\nel.className = 'new-item';\nel.textContent = 'New Item';\ndocument.body.appendChild(el);";
            case 3: return "var el = document.getElementById('target');\nif (el) el.parentNode.removeChild(el);";
        }
        return "";
    }
    private String getJSEvents(int i) {
        switch (i) {
            case 0: return "document.getElementById('btn').addEventListener('click', function(e) {\n  e.preventDefault();\n  console.log('Clicked!');\n});";
            case 1: return "document.addEventListener('DOMContentLoaded', function() {\n  // DOM is ready\n  init();\n});";
            case 2: return "document.addEventListener('keydown', function(e) {\n  if (e.key === 'Enter') { /* handle */ }\n  if (e.key === 'Escape') { /* handle */ }\n});";
            case 3: return "var evt = new CustomEvent('myEvent', { detail: { value: 42 } });\ndocument.dispatchEvent(evt);\ndocument.addEventListener('myEvent', function(e) {\n  console.log(e.detail.value);\n});";
        }
        return "";
    }
    private String getJSFunc(int i) {
        switch (i) {
            case 0: return "var double = function(x) { return x * 2; };\nconsole.log(double(5)); // 10";
            case 1: return "function getData(url) {\n  return fetch(url).then(function(r) { return r.json(); });\n}\ngetData('/api/data').then(function(d) { console.log(d); });";
            case 2: return "function debounce(fn, delay) {\n  var t;\n  return function() {\n    var args = arguments;\n    clearTimeout(t);\n    t = setTimeout(function() { fn.apply(this, args); }, delay);\n  };\n}\nvar search = debounce(function(q) { /* search */ }, 300);";
            case 3: return "function throttle(fn, limit) {\n  var last = 0;\n  return function() {\n    var now = Date.now();\n    if (now - last >= limit) { last = now; fn.apply(this, arguments); }\n  };\n}";
        }
        return "";
    }
    private String getJSFetch(int i) {
        switch (i) {
            case 0: return "fetch('https://api.example.com/data')\n  .then(function(res) { return res.json(); })\n  .then(function(data) { console.log(data); })\n  .catch(function(err) { console.error(err); });";
            case 1: return "fetch('/api/create', {\n  method: 'POST',\n  headers: { 'Content-Type': 'application/json' },\n  body: JSON.stringify({ name: 'Alice', age: 28 })\n}).then(function(r) { return r.json(); }).then(console.log);";
            case 2: return "fetch(url)\n  .then(function(res) {\n    if (!res.ok) throw new Error('HTTP ' + res.status);\n    return res.json();\n  })\n  .catch(function(e) { console.error('Fetch failed:', e.message); });";
            case 3: return "function loadJSON(url, cb) {\n  var x = new XMLHttpRequest();\n  x.open('GET', url);\n  x.onload = function() { if (x.status === 200) cb(JSON.parse(x.responseText)); };\n  x.send();\n}";
        }
        return "";
    }
    private String getJSArray(int i) {
        switch (i) {
            case 0: return "var nums = [1,2,3,4,5];\nvar doubled  = nums.map(function(n) { return n * 2; });\nvar evens    = nums.filter(function(n) { return n % 2 === 0; });\nvar sum      = nums.reduce(function(a, b) { return a + b; }, 0);";
            case 1: return "[1,2,3].forEach(function(item, idx) {\n  console.log(idx, item);\n});";
            case 2: return "var a = [1, 2, 3];\nvar b = [4, 5, 6];\nvar merged = a.concat(b); // [1,2,3,4,5,6]";
            case 3: return "var arr = [1, 2, 2, 3, 3, 4];\nvar unique = arr.filter(function(v, i, a) { return a.indexOf(v) === i; });";
        }
        return "";
    }
    private String getJSObject(int i) {
        switch (i) {
            case 0: return "var user = { name: 'Alice', age: 28, city: 'Paris' };\nvar name = user.name;\nvar age  = user.age;";
            case 1: return "var defaults = { color: 'blue', size: 10 };\nvar options  = { size: 20, weight: 'bold' };\nvar merged = {};\nfor (var k in defaults) merged[k] = defaults[k];\nfor (var k in options)  merged[k] = options[k];";
            case 2: return "var obj = { a: 1, b: [2, 3] };\nvar json   = JSON.stringify(obj);\nvar parsed = JSON.parse(json);\nconsole.log(parsed.b); // [2,3]";
            case 3: return "function deepClone(obj) {\n  return JSON.parse(JSON.stringify(obj));\n}";
        }
        return "";
    }
    private String getJSUtil(int i) {
        switch (i) {
            case 0: return "function lsGet(key) {\n  try { return localStorage.getItem(key); } catch(e) { return null; }\n}\nfunction lsSet(key, val) {\n  try { localStorage.setItem(key, val); } catch(e) {}\n}";
            case 1: return "function getCookie(name) {\n  var match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));\n  return match ? decodeURIComponent(match[1]) : undefined;\n}";
            case 2: return "function uuid() {\n  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {\n    var r = Math.random()*16|0, v = c=='x'?r:(r&0x3|0x8);\n    return v.toString(16);\n  });\n}";
            case 3: return "function formatDate(d) {\n  var dt = d || new Date();\n  return dt.getFullYear() + '-' +\n    ('0'+(dt.getMonth()+1)).slice(-2) + '-' +\n    ('0'+dt.getDate()).slice(-2);\n}";
        }
        return "";
    }
}
