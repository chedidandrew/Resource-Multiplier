package com.chedidandrew.smartresourcedrops.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Adapts 1.19.2's autocomplete-style EditBox suggestion into an empty-value
 * placeholder. Vanilla otherwise appends the full hint after typed text.
 */
final class LegacySearchBox extends EditBox {
    private static final Consumer<String> NOOP_RESPONDER = value -> { };

    private final String emptyHint;
    private Consumer<String> searchResponder = NOOP_RESPONDER;
    private String activeSuggestion;

    LegacySearchBox(
            final Font font,
            final int x,
            final int y,
            final int width,
            final int height,
            final Component message
    ) {
        super(font, x, y, width, height, message);
        this.emptyHint = message.getString();
        super.setResponder(this::onValueChanged);
        this.updateSuggestion(this.getValue());
    }

    /** Keeps the internal hint updater installed while retaining screen callbacks. */
    @Override
    public void setResponder(final Consumer<String> responder) {
        this.searchResponder = responder == null ? NOOP_RESPONDER : responder;
    }

    String activeSuggestion() {
        return this.activeSuggestion;
    }

    String emptyHint() {
        return this.emptyHint;
    }

    private void onValueChanged(final String value) {
        // 1.19.2 setValue may report its untrimmed input after reporting the
        // stored value. The displayed value is the source of truth for hints.
        this.updateSuggestion(this.getValue());
        this.searchResponder.accept(value);
    }

    private void updateSuggestion(final String value) {
        this.activeSuggestion = suggestionFor(value, this.emptyHint);
        super.setSuggestion(this.activeSuggestion);
    }

    static String suggestionFor(final String value, final String emptyHint) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(emptyHint, "emptyHint");
        return value.isEmpty() ? emptyHint : null;
    }
}
