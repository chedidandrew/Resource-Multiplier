package com.chedidandrew.smartresourcedrops.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LegacySearchBoxTest {
    @Test
    void emptyValueShowsTheLegacyPlaceholder() {
        assertEquals(
                "Search categories...",
                LegacySearchBox.suggestionFor("", "Search categories..."));
    }

    @Test
    void typedValueClearsAutocompleteSuggestion() {
        assertNull(LegacySearchBox.suggestionFor("logs", "Search categories..."));
        assertNull(LegacySearchBox.suggestionFor(" ", "Search categories..."));
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> LegacySearchBox.suggestionFor(null, "Search categories..."));
        assertThrows(
                NullPointerException.class,
                () -> LegacySearchBox.suggestionFor("", null));
    }
}
