package com.jnhprojects.mcheli.workshop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditHistoryTest {
    @Test
    void undoAndRedoWalkTheSteps() {
        EditHistory<String> history = new EditHistory<>();
        history.record("a", null);
        history.record("b", null);
        assertEquals("b", history.undo("c"));
        assertEquals("a", history.undo("b"));
        assertFalse(history.canUndo());
        assertEquals("b", history.redo("a"));
        assertEquals("c", history.redo("b"));
        assertFalse(history.canRedo());
    }

    @Test
    void changesWithTheSameKeyMerge() {
        EditHistory<String> history = new EditHistory<>();
        history.record("a", "drag");
        history.record("ab", "drag");
        history.record("abc", "drag");
        assertEquals("a", history.undo("abcd"));
        assertFalse(history.canUndo());
    }

    @Test
    void endingAMergeStartsANewStep() {
        EditHistory<String> history = new EditHistory<>();
        history.record("a", "drag");
        history.endMerge();
        history.record("b", "drag");
        assertEquals("b", history.undo("c"));
        assertTrue(history.canUndo());
    }

    @Test
    void aNewChangeClearsRedo() {
        EditHistory<String> history = new EditHistory<>();
        history.record("a", null);
        history.undo("b");
        history.record("a", null);
        assertFalse(history.canRedo());
    }
}
