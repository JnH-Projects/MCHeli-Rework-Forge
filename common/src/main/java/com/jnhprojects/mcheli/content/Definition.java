package com.jnhprojects.mcheli.content;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base class for anything loaded from a MCHeli {@code key = value} text file.
 *
 * <p>Subclasses register a handler per lower-case key in {@link #registerKeys(KeyTable)}. Keys
 * that no handler claims are reported back to the loader, which makes it easy to see which parts
 * of the legacy format are still waiting to be ported.
 */
public abstract class Definition {
    private final String name;
    private final Map<String, Consumer<String>> handlers = new HashMap<>();
    private String pack = "";

    protected Definition(String name) {
        this.name = name;
        registerKeys(new KeyTable(handlers));
    }

    /** File name without extension, lower case. Also used as the registry path. */
    public String name() {
        return name;
    }

    /**
     * Id of the content pack the file came from, {@code original} for the imported 1.0.3 content,
     * or empty for a definition built in code.
     */
    public String pack() {
        return pack;
    }

    void setPack(String pack) {
        this.pack = pack;
    }

    protected abstract void registerKeys(KeyTable keys);

    /**
     * Applies one entry. Subclasses may call it to forward an alias to another key.
     *
     * @param key lower-case key
     * @return {@code false} if no handler is registered for {@code key}
     */
    protected final boolean accept(String key, String value) {
        Consumer<String> handler = handlers.get(key);
        if (handler == null) {
            return false;
        }
        handler.accept(value);
        return true;
    }

    /**
     * Called once every line has been applied. Fill in derived values here and throw
     * {@link InvalidDefinitionException} if the definition cannot be used.
     */
    protected void validate() throws InvalidDefinitionException {
    }

    public static final class KeyTable {
        private final Map<String, Consumer<String>> handlers;

        private KeyTable(Map<String, Consumer<String>> handlers) {
            this.handlers = handlers;
        }

        public void put(String key, Consumer<String> handler) {
            handlers.put(DefinitionValues.lower(key), handler);
        }

        /** Registers the same handler under several keys. */
        public void put(Consumer<String> handler, String... keys) {
            for (String key : keys) {
                put(key, handler);
            }
        }

        /** For handlers that need to know which alias was used, such as {@code AddSeat} vs {@code AddGunnerSeat}. */
        public void putAliased(String key, java.util.function.BiConsumer<String, String> handler) {
            String lower = DefinitionValues.lower(key);
            handlers.put(lower, value -> handler.accept(lower, value));
        }
    }
}
