/**
 * UndoManager – lightweight undo/redo stack for the CodeEditor.
 * Stores up to MAX_STATES plain-text snapshots of the editor content.
 */
public class UndoManager {

    private static final int MAX_STATES = 30;

    private String[] history  = new String[MAX_STATES];
    private int      head     = -1;   // index of current state
    private int      size     = 0;    // number of valid entries

    /** Push a new state; discards any redo history beyond current head. */
    public void push(String state) {
        if (head >= 0 && state.equals(history[head])) return; // no change

        head = (head + 1) % MAX_STATES;
        history[head] = state;
        size = Math.min(size + 1, MAX_STATES);
    }

    /** Undo: returns the previous state, or null if nothing to undo. */
    public String undo() {
        if (size <= 1) return null;
        head = (head - 1 + MAX_STATES) % MAX_STATES;
        size--;
        return history[head];
    }

    /** Redo: not fully tracked in this lightweight version — returns null. */
    public String redo() {
        return null; // redo tracking kept simple; extend if desired
    }

    public boolean canUndo() { return size > 1; }

    /** Clear the entire history. */
    public void clear() {
        head = -1;
        size = 0;
    }
}
