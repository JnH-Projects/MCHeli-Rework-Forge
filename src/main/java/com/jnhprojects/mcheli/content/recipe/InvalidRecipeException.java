package com.jnhprojects.mcheli.content.recipe;

/** A recipe line that cannot be turned into a 26.x recipe; the message says why. */
public class InvalidRecipeException extends RuntimeException {
    public InvalidRecipeException(String message) {
        super(message);
    }
}
