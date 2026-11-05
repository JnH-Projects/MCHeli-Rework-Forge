package com.jnhprojects.mcheli.workshop;

import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Undo and redo for the workshop, as whole snapshots of what is edited: a definition is small, so
 * this is simpler and safer than undoing each kind of change by hand.
 *
 * <p>Changes with the same merge key that follow each other count as one step, so typing a number
 * or dragging a gizmo undoes in one go rather than one character or one mouse move at a time.
 *
 * @param <T> the snapshot, which must not change once handed over
 */
public final class EditHistory<T> {
    private static final int MAX_STEPS = 200;

    private final Deque<T> undo = new ArrayDeque<>();
    private final Deque<T> redo = new ArrayDeque<>();
    private @Nullable Object lastMergeKey;

    /**
     * Call before changing anything.
     *
     * @param before   the state before the change
     * @param mergeKey groups this change with the previous one if the keys are equal; {@code null}
     *                 always starts a new step
     */
    public void record(T before, @Nullable Object mergeKey) {
        redo.clear();
        if (mergeKey != null && Objects.equals(mergeKey, lastMergeKey)) {
            return;
        }
        lastMergeKey = mergeKey;
        undo.push(before);
        if (undo.size() > MAX_STEPS) {
            undo.removeLast();
        }
    }

    /** Ends merging, so the next change is a new step even with the same key. */
    public void endMerge() {
        lastMergeKey = null;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /** @return the state to go back to; {@code current} becomes what redo returns to */
    public T undo(T current) {
        lastMergeKey = null;
        redo.push(current);
        return undo.pop();
    }

    /** @return the state to go forward to; {@code current} becomes what undo returns to */
    public T redo(T current) {
        lastMergeKey = null;
        undo.push(current);
        return redo.pop();
    }
}
