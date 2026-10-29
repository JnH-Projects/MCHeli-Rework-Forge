package com.jnhprojects.mcheli.content.pack;

/** A content pack that cannot be used as it is. The message says why, for the pack's author. */
public final class PackException extends Exception {
    public PackException(String message) {
        super(message);
    }
}
