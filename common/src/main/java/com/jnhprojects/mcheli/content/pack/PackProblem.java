package com.jnhprojects.mcheli.content.pack;

/**
 * Something wrong with a content pack, for the log and for operators in the chat. A problem never
 * stops the game: the pack, or the one file, is left out and everything else loads.
 *
 * @param pack    the pack id, or its file name when the id could not be read
 * @param message what is wrong, in English
 */
public record PackProblem(String pack, String message) {
    @Override
    public String toString() {
        return pack + ": " + message;
    }
}
