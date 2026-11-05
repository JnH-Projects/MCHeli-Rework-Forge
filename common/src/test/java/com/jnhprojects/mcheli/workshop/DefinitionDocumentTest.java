package com.jnhprojects.mcheli.workshop;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DefinitionDocumentTest {
    private static final String TEXT = """
            // A comment
            DisplayName = Scout

            ; another comment
            Speed = 0.5
            NotAKeyWeKnow = 1, 2, 3
            Empty =
            AddSeat = 0, 1, 0
            AddSeat = 1, 1, 0
            """;

    @Test
    void untouchedLinesSurviveARoundTrip() {
        DefinitionDocument doc = new DefinitionDocument(TEXT);
        doc.set("Speed", "0.75");
        assertEquals(TEXT.replace("Speed = 0.5", "Speed = 0.75"), doc.text());
    }

    @Test
    void entriesFollowTheReadersRules() {
        List<DefinitionDocument.Entry> entries = new DefinitionDocument(TEXT).entries();
        assertEquals(List.of("DisplayName", "Speed", "NotAKeyWeKnow", "AddSeat", "AddSeat"),
                entries.stream().map(DefinitionDocument.Entry::key).toList());
        assertEquals("1, 2, 3", entries.get(2).value());
    }

    @Test
    void theLastLineOfAKeyIsTheOneInEffect() {
        DefinitionDocument doc = new DefinitionDocument("Speed = 1\nspeed = 2\n");
        assertEquals("2", doc.value("SPEED"));
        doc.set("Speed", "3");
        assertEquals("Speed = 1\nspeed = 3\n", doc.text());
    }

    @Test
    void settingNullRemovesEveryLineOfAKey() {
        DefinitionDocument doc = new DefinitionDocument("Speed = 1\nMaxHp = 5\nspeed = 2\n");
        doc.set("Speed", null);
        assertEquals("MaxHp = 5\n", doc.text());
        assertNull(doc.value("Speed"));
    }

    @Test
    void aMissingKeyGoesAtTheEnd() {
        DefinitionDocument doc = new DefinitionDocument("MaxHp = 5\n");
        doc.set("Speed", "1");
        assertEquals("MaxHp = 5\nSpeed = 1\n", doc.text());
    }

    @Test
    void aByteOrderMarkIsDropped() {
        assertEquals("Speed = 1\n", new DefinitionDocument("﻿Speed = 1\n").text());
    }
}
